package com.aidigital.aionboarding.external.youtube.impl;

import com.aidigital.aionboarding.external.common.http.PooledRestClientFactory;
import com.aidigital.aionboarding.external.youtube.YoutubeClient;
import com.aidigital.aionboarding.external.youtube.YoutubeExternalException;
import com.aidigital.aionboarding.external.youtube.config.YoutubeProperties;
import com.aidigital.aionboarding.external.youtube.model.YoutubeOEmbedMetadata;
import com.aidigital.aionboarding.external.youtube.model.YoutubeTranscriptResult;
import com.aidigital.aionboarding.external.youtube.model.YoutubeTranscriptSegment;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Production {@link YoutubeClient} using oEmbed and timedtext caption scraping.
 */
public class YoutubeClientImpl implements YoutubeClient {

	private static final Logger LOG = LoggerFactory.getLogger(YoutubeClientImpl.class);

	/**
	 * Caps how many bytes of a non-2xx response body are read into the reported error, so a
	 * misbehaving or oversized provider error response cannot force an unbounded allocation.
	 */
	private static final int MAX_ERROR_BODY_BYTES = 8192;
	private static final String OEMBED_BASE_URL = "https://www.youtube.com/oembed";
	private static final String USER_AGENT = "AI Digital Onboarding/1.0 (+https://ai-digital.com)";
	private static final Pattern CAPTION_TRACK_PATTERN =
			Pattern.compile("\"captionTracks\"\\s*:\\s*(\\[[^\\]]+\\])");
	private static final Pattern BASE_URL_PATTERN =
			Pattern.compile("\"baseUrl\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
	private static final Pattern LANGUAGE_CODE_PATTERN =
			Pattern.compile("\"languageCode\"\\s*:\\s*\"([^\"]+)\"");
	private static final Pattern XML_TEXT_PATTERN =
			Pattern.compile("<text[^>]*start=\"([^\"]+)\"[^>]*>(.*?)</text>", Pattern.DOTALL);

	private final YoutubeProperties properties;
	private final RestClient oembedClient;
	private final RestClient youtubeClient;
	private final ObjectMapper objectMapper;

	public YoutubeClientImpl(YoutubeProperties properties, PooledRestClientFactory factory) {
		this.properties = properties;
		this.oembedClient = factory.createClient("youtube-oembed", "https://www.youtube.com");
		this.youtubeClient = factory.createClient("youtube", "https://www.youtube.com");
		this.objectMapper = new ObjectMapper();
	}

	@Override
	public YoutubeOEmbedMetadata fetchOembed(String url) {
		if (!isSupportedYoutubeUrl(url)) {
			return oEmbedError("A valid YouTube URL is required.");
		}

		try {
			URI uri = UriComponentsBuilder.fromUriString(OEMBED_BASE_URL)
					.queryParam("url", url)
					.queryParam("format", "json")
					.build()
					.encode()
					.toUri();

			JsonNode data = oembedClient.get()
					.uri(uri)
					.header("Accept", "application/json")
					.header("User-Agent", USER_AGENT)
					.retrieve()
					.onStatus(HttpStatusCode::isError, (req, res) -> {
						String body = new String(res.getBody().readNBytes(MAX_ERROR_BODY_BYTES));
						throw new YoutubeExternalException(
								extractOembedError(body, "Failed to load YouTube metadata."));
					})
					.body(JsonNode.class);

			return new YoutubeOEmbedMetadata(
					textValue(data, "title"),
					textValue(data, "author_name"),
					textValue(data, "author_url"),
					textValue(data, "thumbnail_url"),
					intValue(data, "thumbnail_width"),
					intValue(data, "thumbnail_height"),
					firstNonBlank(textValue(data, "provider_name"), "YouTube"),
					"");
		} catch (YoutubeExternalException ex) {
			LOG.warn("YouTube oEmbed returned an error for url={}: {}", url, ex.getMessage());
			return oEmbedError(ex.getMessage());
		} catch (RestClientException ex) {
			LOG.warn("YouTube oEmbed request failed for url={}: {}", url, ex.getMessage());
			return oEmbedError(ex.getMessage());
		} catch (Exception ex) {
			LOG.warn("YouTube oEmbed request failed for url={}: {}", url, ex.getMessage());
			return oEmbedError("Failed to load YouTube metadata.");
		}
	}

	@Override
	public YoutubeTranscriptResult fetchTranscript(String videoId) {
		if (videoId == null || videoId.isBlank()) {
			return transcriptError("", "YouTube video id is required.");
		}

		String trimmedVideoId = videoId.trim();

		try {
			String watchHtml = youtubeClient.get()
					.uri("/watch?v=" + trimmedVideoId)
					.header("Accept-Language", "en-US,en;q=0.9")
					.header("User-Agent", USER_AGENT)
					.retrieve()
					.body(String.class);

			if (watchHtml == null || watchHtml.isBlank()) {
				return transcriptError(trimmedVideoId, "Transcript is unavailable.");
			}

			String captionTrackUrl = resolveCaptionTrackUrl(watchHtml, properties.getTranscriptLang());
			if (captionTrackUrl.isBlank()) {
				return transcriptError(trimmedVideoId, "No caption tracks found for video.");
			}

			String timedText = fetchTimedText(captionTrackUrl);
			List<YoutubeTranscriptSegment> segments = parseTimedText(timedText);

			if (segments.isEmpty()) {
				return transcriptError(trimmedVideoId, "Transcript was empty.");
			}

			if (properties.isTranscriptDebug()) {
				LOG.info("YouTube transcript fetched videoId={} segments={}", trimmedVideoId, segments.size());
			}

			return new YoutubeTranscriptResult(trimmedVideoId, segments, "");
		} catch (Exception ex) {
			if (properties.isTranscriptDebug()) {
				LOG.warn("YouTube transcript failed videoId={}: {}", trimmedVideoId, ex.getMessage());
			}
			return transcriptError(
					trimmedVideoId,
					ex.getMessage() == null ? "Transcript is unavailable." : ex.getMessage());
		}
	}

	private boolean isSupportedYoutubeUrl(String url) {
		if (url == null || url.isBlank()) {
			return false;
		}
		try {
			URI parsed = URI.create(url.trim());
			String host = parsed.getHost();
			return host != null && (host.contains("youtube.com") || host.contains("youtu.be"));
		} catch (IllegalArgumentException ex) {
			return false;
		}
	}

	private YoutubeOEmbedMetadata oEmbedError(String message) {
		return new YoutubeOEmbedMetadata("", "", "", "", null, null, "", message);
	}

	private YoutubeTranscriptResult transcriptError(String videoId, String message) {
		return new YoutubeTranscriptResult(videoId, List.of(), message);
	}

	private String fetchTimedText(String captionTrackUrl) {
		String requestUrl = captionTrackUrl;
		if (!requestUrl.contains("fmt=")) {
			requestUrl = requestUrl + (requestUrl.contains("?") ? "&" : "?") + "fmt=vtt";
		}

		return youtubeClient.get()
				.uri(URI.create(requestUrl))
				.header("User-Agent", USER_AGENT)
				.retrieve()
				.body(String.class);
	}

	private String resolveCaptionTrackUrl(String watchHtml, String preferredLanguage) {
		Matcher captionMatcher = CAPTION_TRACK_PATTERN.matcher(watchHtml);
		if (!captionMatcher.find()) {
			return "";
		}

		String captionTracksJson = captionMatcher.group(1).replace("\\u0026", "&");
		List<CaptionTrack> tracks = new ArrayList<>();
		Matcher baseUrlMatcher = BASE_URL_PATTERN.matcher(captionTracksJson);
		Matcher languageMatcher = LANGUAGE_CODE_PATTERN.matcher(captionTracksJson);

		List<String> baseUrls = new ArrayList<>();
		while (baseUrlMatcher.find()) {
			baseUrls.add(unescapeJson(baseUrlMatcher.group(1)));
		}
		List<String> languageCodes = new ArrayList<>();
		while (languageMatcher.find()) {
			languageCodes.add(languageMatcher.group(1));
		}

		int count = Math.min(baseUrls.size(), languageCodes.size());
		for (int index = 0; index < count; index++) {
			tracks.add(new CaptionTrack(languageCodes.get(index), baseUrls.get(index)));
		}

		if (tracks.isEmpty()) {
			return "";
		}

		if (preferredLanguage != null && !preferredLanguage.isBlank()) {
			for (CaptionTrack track : tracks) {
				if (preferredLanguage.equalsIgnoreCase(track.languageCode())) {
					return track.baseUrl();
				}
			}
		}

		tracks.sort(Comparator.comparing(CaptionTrack::languageCode));
		return tracks.get(0).baseUrl();
	}

	private List<YoutubeTranscriptSegment> parseTimedText(String timedText) {
		if (timedText == null || timedText.isBlank()) {
			return List.of();
		}

		String trimmed = timedText.trim();
		if (trimmed.startsWith("{")) {
			return parseJsonTimedText(trimmed);
		}
		if (trimmed.contains("WEBVTT")) {
			return parseVttTimedText(trimmed);
		}
		return parseXmlTimedText(trimmed);
	}

	private List<YoutubeTranscriptSegment> parseJsonTimedText(String json) {
		try {
			JsonNode root = objectMapper.readTree(json);
			JsonNode events = root.get("events");
			if (events == null || !events.isArray()) {
				return List.of();
			}

			List<YoutubeTranscriptSegment> segments = new ArrayList<>();
			for (JsonNode event : events) {
				double startMs = event.path("tStartMs").asDouble(0);
				String text = "";
				JsonNode segs = event.get("segs");
				if (segs != null && segs.isArray()) {
					StringBuilder builder = new StringBuilder();
					for (JsonNode seg : segs) {
						builder.append(seg.path("utf8").asText(""));
					}
					text = normalizeText(builder.toString());
				}
				if (!text.isBlank()) {
					segments.add(new YoutubeTranscriptSegment(startMs / 1000.0, text));
				}
			}
			return segments;
		} catch (Exception ex) {
			throw new YoutubeExternalException("Failed to parse JSON transcript", ex);
		}
	}

	private List<YoutubeTranscriptSegment> parseVttTimedText(String vtt) {
		List<YoutubeTranscriptSegment> segments = new ArrayList<>();
		String[] lines = vtt.split("\\R");
		Double currentStart = null;

		for (String rawLine : lines) {
			String line = rawLine.trim();
			if (line.isBlank() || "WEBVTT".equals(line) || line.matches("^\\d+$")) {
				continue;
			}

			int arrowIndex = line.indexOf("-->");
			if (arrowIndex >= 0) {
				currentStart = parseVttTimestamp(line.substring(0, arrowIndex).trim());
				continue;
			}

			if (currentStart != null && !line.startsWith("NOTE")) {
				String text = normalizeText(line);
				if (!text.isBlank()) {
					segments.add(new YoutubeTranscriptSegment(currentStart, text));
				}
				currentStart = null;
			}
		}

		return segments;
	}

	private List<YoutubeTranscriptSegment> parseXmlTimedText(String xml) {
		List<YoutubeTranscriptSegment> segments = new ArrayList<>();
		Matcher matcher = XML_TEXT_PATTERN.matcher(xml);
		while (matcher.find()) {
			double start = Double.parseDouble(matcher.group(1));
			String text = normalizeText(unescapeXml(matcher.group(2)));
			if (!text.isBlank()) {
				segments.add(new YoutubeTranscriptSegment(start, text));
			}
		}
		return segments;
	}

	private double parseVttTimestamp(String timestamp) {
		String[] parts = timestamp.split(":");
		if (parts.length == 3) {
			return Integer.parseInt(parts[0]) * 3600
					+ Integer.parseInt(parts[1]) * 60
					+ Double.parseDouble(parts[2].replace(',', '.'));
		}
		if (parts.length == 2) {
			return Integer.parseInt(parts[0]) * 60
					+ Double.parseDouble(parts[1].replace(',', '.'));
		}
		return 0;
	}

	private String extractOembedError(String body, String fallback) {
		try {
			JsonNode node = objectMapper.readTree(body);
			String error = textValue(node, "error");
			if (!error.isBlank()) {
				return error;
			}
		} catch (Exception ignored) {
			// fall through
		}
		return fallback;
	}

	private String textValue(JsonNode node, String field) {
		if (node == null || node.isNull()) {
			return "";
		}
		JsonNode value = node.get(field);
		return value == null || value.isNull() ? "" : value.asText("");
	}

	private Integer intValue(JsonNode node, String field) {
		if (node == null || node.isNull()) {
			return null;
		}
		JsonNode value = node.get(field);
		return value == null || value.isNull() ? null : value.asInt();
	}

	private String firstNonBlank(String first, String second) {
		if (first != null && !first.isBlank()) {
			return first;
		}
		return second == null ? "" : second;
	}

	private String normalizeText(String value) {
		return value
				.replace("\r\n", "\n")
				.replaceAll("[ \\t]+", " ")
				.replaceAll("\\n{3,}", "\n\n")
				.replaceAll("<[^>]+>", " ")
				.trim();
	}

	private String unescapeJson(String value) {
		return value
				.replace("\\\"", "\"")
				.replace("\\\\", "\\")
				.replace("\\/", "/");
	}

	private String unescapeXml(String value) {
		return value
				.replace("&amp;", "&")
				.replace("&lt;", "<")
				.replace("&gt;", ">")
				.replace("&quot;", "\"")
				.replace("&#39;", "'")
				.replace("&nbsp;", " ");
	}

	private record CaptionTrack(String languageCode, String baseUrl) {

	}
}
