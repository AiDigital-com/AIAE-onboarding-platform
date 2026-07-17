package com.aidigital.aionboarding.external.heygen.impl;

import com.aidigital.aionboarding.external.common.http.PooledRestClientFactory;
import com.aidigital.aionboarding.external.heygen.HeyGenClient;
import com.aidigital.aionboarding.external.heygen.HeyGenExternalException;
import com.aidigital.aionboarding.external.heygen.config.HeyGenProperties;
import com.aidigital.aionboarding.external.heygen.model.HeyGenTeacherVideoResult;
import com.aidigital.aionboarding.external.heygen.model.HeyGenVideoStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

/**
 * Production {@link HeyGenClient} using the HeyGen v3 HTTP API.
 */
public class HeyGenClientImpl implements HeyGenClient {

	private static final Logger LOG = LoggerFactory.getLogger(HeyGenClientImpl.class);
	private static final String PROVIDER = "heygen";

	/**
	 * Caps how many bytes of a non-2xx response body are read into the reported error, so a
	 * misbehaving or oversized provider error response cannot force an unbounded allocation.
	 */
	private static final int MAX_ERROR_BODY_BYTES = 8192;

	private final HeyGenProperties properties;
	private final RestClient restClient;
	private final ObjectMapper objectMapper;

	public HeyGenClientImpl(HeyGenProperties properties, PooledRestClientFactory factory) {
		this.properties = properties;
		this.restClient = factory.createClient("heygen", properties.getBaseUrl());
		this.objectMapper = new ObjectMapper();
	}

	@Override
	public HeyGenTeacherVideoResult createTeacherVideo(String prompt) {
		if (prompt == null || prompt.isBlank()) {
			throw new HeyGenExternalException("HeyGen prompt is required");
		}

		ObjectNode body = objectMapper.createObjectNode();
		body.put("prompt", prompt);
		body.put("orientation", "landscape");
		if (properties.getAvatarId() != null && !properties.getAvatarId().isBlank()) {
			body.put("avatar_id", properties.getAvatarId().trim());
		}
		if (properties.getVoiceId() != null && !properties.getVoiceId().isBlank()) {
			body.put("voice_id", properties.getVoiceId().trim());
		}

		LOG.debug("Creating HeyGen teacher video");

		try {
			JsonNode data = postForData("/v3/video-agents", body);
			return new HeyGenTeacherVideoResult(
					PROVIDER,
					prompt,
					properties.getAvatarId(),
					properties.getVoiceId(),
					textValue(data, "session_id"),
					firstNonBlank(textValue(data, "video_id"), textValue(data, "id")),
					firstNonBlank(textValue(data, "status"), "generating"));
		} catch (HeyGenExternalException ex) {
			throw ex;
		} catch (RestClientException ex) {
			throw new HeyGenExternalException("HeyGen API call failed: " + ex.getMessage(), ex);
		} catch (Exception ex) {
			throw new HeyGenExternalException("Unexpected error calling HeyGen API", ex);
		}
	}

	@Override
	public HeyGenVideoStatus getVideoStatus(String videoId) {
		if (videoId == null || videoId.isBlank()) {
			throw new HeyGenExternalException("HeyGen video id is required");
		}

		try {
			String encodedId = UriUtils.encodePathSegment(videoId.trim(), StandardCharsets.UTF_8);
			JsonNode data = getForData("/v3/videos/" + encodedId);
			Double duration = null;
			if (data.has("duration") && !data.get("duration").isNull()) {
				duration = data.get("duration").asDouble();
			}

			return new HeyGenVideoStatus(
					firstNonBlank(textValue(data, "id"), videoId),
					firstNonBlank(textValue(data, "status"), "unknown"),
					textValue(data, "video_url"),
					textValue(data, "thumbnail_url"),
					duration);
		} catch (HeyGenExternalException ex) {
			throw ex;
		} catch (RestClientException ex) {
			throw new HeyGenExternalException("HeyGen API call failed: " + ex.getMessage(), ex);
		} catch (Exception ex) {
			throw new HeyGenExternalException("Unexpected error calling HeyGen API", ex);
		}
	}

	private JsonNode postForData(String path, Object body) {
		JsonNode payload = restClient.post()
				.uri(path)
				.header("X-Api-Key", properties.getApiKey())
				.contentType(MediaType.APPLICATION_JSON)
				.body(body)
				.retrieve()
				.onStatus(HttpStatusCode::isError, (req, res) -> {
					String responseBody = new String(res.getBody().readNBytes(MAX_ERROR_BODY_BYTES));
					throw new HeyGenExternalException(
							extractErrorMessage(responseBody),
							res.getStatusCode().value(),
							responseBody);
				})
				.body(JsonNode.class);

		return unwrapData(payload);
	}

	private JsonNode getForData(String path) {
		JsonNode payload = restClient.get()
				.uri(path)
				.header("X-Api-Key", properties.getApiKey())
				.retrieve()
				.onStatus(HttpStatusCode::isError, (req, res) -> {
					String responseBody = new String(res.getBody().readNBytes(MAX_ERROR_BODY_BYTES));
					throw new HeyGenExternalException(
							extractErrorMessage(responseBody),
							res.getStatusCode().value(),
							responseBody);
				})
				.body(JsonNode.class);

		return unwrapData(payload);
	}

	private JsonNode unwrapData(JsonNode payload) {
		if (payload == null) {
			throw new HeyGenExternalException("HeyGen API returned an empty response");
		}
		if (payload.has("data") && payload.get("data").isObject()) {
			return payload.get("data");
		}
		return payload;
	}

	private String extractErrorMessage(String responseBody) {
		try {
			JsonNode node = objectMapper.readTree(responseBody);
			String fromError = textValue(node.path("error"), "message");
			if (!fromError.isBlank()) {
				return fromError;
			}
			String message = textValue(node, "message");
			if (!message.isBlank()) {
				return message;
			}
			String detail = textValue(node, "detail");
			if (!detail.isBlank()) {
				return detail;
			}
		} catch (Exception ignored) {
			// fall through
		}
		return "HeyGen request failed.";
	}

	private String textValue(JsonNode node, String field) {
		if (node == null || node.isMissingNode() || node.isNull()) {
			return "";
		}
		JsonNode value = node.get(field);
		if (value == null || value.isNull()) {
			return "";
		}
		return value.asText("");
	}

	private String firstNonBlank(String first, String second) {
		if (first != null && !first.isBlank()) {
			return first;
		}
		return second == null ? "" : second;
	}
}
