package com.aidigital.aionboarding.service.material.support;

import com.aidigital.aionboarding.domain.material.entities.Material;
import com.aidigital.aionboarding.domain.material.entities.MaterialFile;
import com.aidigital.aionboarding.domain.material.entities.MaterialLink;
import com.aidigital.aionboarding.domain.material.entities.MaterialYoutubeUrl;
import com.aidigital.aionboarding.external.youtube.YoutubeClient;
import com.aidigital.aionboarding.external.youtube.model.YoutubeTranscriptResult;
import com.aidigital.aionboarding.external.youtube.model.YoutubeTranscriptSegment;
import com.aidigital.aionboarding.service.lesson.util.LessonTextUtil;
import com.aidigital.aionboarding.service.material.services.MaterialFileService;
import com.aidigital.aionboarding.service.material.services.MaterialLinkService;
import com.aidigital.aionboarding.service.material.services.MaterialYoutubeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class MaterialPreparationMapBuilder {

	private static final Pattern YOUTUBE_ID_PATTERN = Pattern.compile(
			"(?:youtube\\.com/watch\\?v=|youtu\\.be/|youtube\\.com/shorts/)([A-Za-z0-9_-]{6,})");

	private final MaterialLinkService materialLinkService;
	private final MaterialYoutubeService materialYoutubeService;
	private final MaterialFileService materialFileService;
	private final YoutubeClient youtubeClient;
	private final LessonTextUtil lessonTextUtil;

	public Map<String, Object> buildMaterialMap(Material material, int sourceNumber) {
		List<MaterialLink> links = materialLinkService.findByMaterialIdOrderBySortOrderAsc(material.getId());
		List<MaterialYoutubeUrl> youtubeUrls =
				materialYoutubeService.findByMaterialIdOrderBySortOrderAsc(material.getId());
		List<MaterialFile> files = materialFileService.findByMaterialId(material.getId());

		List<Map<String, Object>> linkAssets = links.stream().map(this::toLinkAsset).toList();
		List<String> linkUrlList = links.stream().map(MaterialLink::getUrl).distinct().toList();
		List<String> youtubeUrlList = youtubeUrls.stream().map(MaterialYoutubeUrl::getUrl).distinct().toList();
		List<Map<String, Object>> youtubeTranscripts = youtubeUrlList.stream()
				.map(this::buildYoutubeTranscript)
				.toList();
		List<Map<String, Object>> attachments = files.stream().map(this::toAttachment).toList();

		Map<String, Object> map = new LinkedHashMap<>();
		map.put("id", material.getId());
		map.put("sourceNumber", sourceNumber);
		map.put("title", lessonTextUtil.normalizeText(material.getTitle()));
		map.put("description", lessonTextUtil.normalizeText(material.getDescription()));
		map.put("text", lessonTextUtil.normalizeText(material.getTextContent()));
		map.put("youtubeUrls", youtubeUrlList);
		map.put("youtubeVideos", List.of());
		map.put("youtubeTranscripts", youtubeTranscripts);
		map.put("links", linkUrlList);
		map.put("linkAssets", linkAssets);
		map.put("attachments", attachments);
		return map;
	}

	Map<String, Object> toLinkAsset(MaterialLink link) {
		Map<String, Object> asset = new LinkedHashMap<>();
		asset.put("url", link.getUrl());
		asset.put("title", lessonTextUtil.normalizeText(link.getTitle()));
		asset.put("description", lessonTextUtil.normalizeText(link.getDescription()));
		asset.put("imageUrl", link.getImageUrl());
		asset.put("siteName", lessonTextUtil.normalizeText(link.getSiteName()));
		asset.put("extractedText", lessonTextUtil.normalizeText(link.getExtractedText()));
		asset.put("metadataError", lessonTextUtil.normalizeText(link.getMetadataError()));
		return asset;
	}

	Map<String, Object> toAttachment(MaterialFile file) {
		Map<String, Object> attachment = new LinkedHashMap<>();
		attachment.put("id", file.getId());
		attachment.put("name", file.getOriginalName());
		attachment.put("storageKey", file.getStorageKey());
		attachment.put("mimeType", file.getMimeType());
		attachment.put("kind", file.getKind() == null ? "file" : file.getKind().getCode());
		attachment.put("size", file.getSizeBytes());
		attachment.put("openaiFileId", file.getOpenaiFileId() == null ? "" : file.getOpenaiFileId());
		attachment.put("openaiFilePurpose", file.getOpenaiFilePurpose());
		attachment.put("openaiFileStatus", file.getOpenaiFileStatus());
		attachment.put("openaiFileError", file.getOpenaiFileError());
		attachment.put("openaiUploadedAt", file.getOpenaiUploadedAt());
		return attachment;
	}

	Map<String, Object> buildYoutubeTranscript(String url) {
		Map<String, Object> transcript = new LinkedHashMap<>();
		transcript.put("url", url);
		String videoId = extractYoutubeVideoId(url);
		transcript.put("videoId", videoId);
		if (videoId.isBlank()) {
			transcript.put("status", "unavailable");
			transcript.put("error", "Could not parse YouTube video id.");
			transcript.put("preparedText", "");
			return transcript;
		}
		YoutubeTranscriptResult result = youtubeClient.fetchTranscript(videoId);
		if (!result.isAvailable() || result.segments() == null || result.segments().isEmpty()) {
			transcript.put("status", "unavailable");
			transcript.put("error", result.error() == null ? "Transcript unavailable." : result.error());
			transcript.put("preparedText", "");
			return transcript;
		}
		String preparedText = result.segments().stream()
				.map(YoutubeTranscriptSegment::text)
				.filter(text -> text != null && !text.isBlank())
				.reduce((a, b) -> a + " " + b)
				.orElse("");
		transcript.put("status", "ready");
		transcript.put("error", "");
		transcript.put("wasCondensed", false);
		transcript.put("preparedText", lessonTextUtil.normalizeText(preparedText));
		transcript.put("rawCharacters", preparedText.length());
		transcript.put("preparedCharacters", preparedText.length());
		transcript.put("segmentCount", result.segments().size());
		return transcript;
	}

	String extractYoutubeVideoId(String url) {
		Matcher matcher = YOUTUBE_ID_PATTERN.matcher(url == null ? "" : url);
		return matcher.find() ? matcher.group(1) : "";
	}
}
