package com.aidigital.aionboarding.service.lesson.support;

import com.aidigital.aionboarding.domain.common.dictionary.LessonAssetKindCode;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonAssetKind;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.lesson.entities.LessonAsset;
import com.aidigital.aionboarding.external.youtube.YoutubeClient;
import com.aidigital.aionboarding.external.youtube.model.YoutubeOEmbedMetadata;
import com.aidigital.aionboarding.service.common.dictionary.DictionaryLookupService;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.lesson.models.CreateLessonAssetInput;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonAssetEntityService;
import com.aidigital.aionboarding.service.link.services.LinkMetadataService;
import com.aidigital.aionboarding.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class LessonAssetDraftBuilder {

	private static final Set<String> SUPPORTED_ASSET_KINDS = Set.of(
			LessonAssetKindCode.LINK,
			LessonAssetKindCode.YOUTUBE,
			LessonAssetKindCode.IMAGE,
			LessonAssetKindCode.FILE,
			LessonAssetKindCode.VIDEO);

	private final LinkMetadataService linkMetadataService;
	private final YoutubeClient youtubeClient;
	private final DictionaryLookupService dictionaryLookupService;
	private final LessonAssetEntityService lessonAssetEntityService;
	private final StorageService storageService;
	private final CurrentTime currentTime;

	public String resolveAssetKind(CreateLessonAssetInput input) {
		String requestedKind = stringVal(input.kind());
		String url = stringVal(input.url()).trim();
		String kind = requestedKind;
		if ("url".equals(requestedKind)) {
			if (!isHttpUrl(url)) {
				throw new AppException(ErrorReason.C002, "A valid HTTP or HTTPS URL is required.");
			}
			kind = isSupportedYoutubeUrl(url) ? LessonAssetKindCode.YOUTUBE : LessonAssetKindCode.LINK;
		}
		if (!SUPPORTED_ASSET_KINDS.contains(kind)) {
			throw new AppException(ErrorReason.C002, "Unsupported lesson asset type.");
		}
		validateKindRequirements(kind, url, input);
		return kind;
	}

	public LessonAsset persistAsset(AppUser viewer, Lesson lesson, CreateLessonAssetInput input, String kind) {
		AssetDraft draft = buildDraft(input, kind);
		draft = enrichDraft(viewer, draft, kind);
		LessonAsset asset = new LessonAsset();
		asset.setLesson(lesson);
		asset.setKind(assetKind(draft.kind()));
		asset.setTitle(draft.title());
		asset.setUrl(draft.url());
		asset.setDescription(draft.description());
		asset.setImageUrl(draft.imageUrl());
		asset.setSiteName(draft.siteName());
		asset.setOriginalName(draft.originalName());
		asset.setStorageKey(draft.storageKey());
		asset.setMimeType(draft.mimeType());
		asset.setSizeBytes(draft.sizeBytes());
		asset.setMetadata(draft.metadata());
		asset.setCreatedAt(currentTime.utcDateTime());
		return lessonAssetEntityService.save(asset);
	}

	void validateKindRequirements(String kind, String url, CreateLessonAssetInput input) {
		String storageKey = stringVal(input.storageKey()).trim();
		String originalName = stringVal(input.originalName()).trim();
		long sizeBytes = input.sizeBytes() == null ? 0L : input.sizeBytes();
		if ((LessonAssetKindCode.LINK.equals(kind) || LessonAssetKindCode.YOUTUBE.equals(kind)) && url.isBlank()) {
			throw new AppException(ErrorReason.C002, "URL is required.");
		}
		if (isUploadedFileKind(kind) && (storageKey.isBlank() || originalName.isBlank() || sizeBytes <= 0)) {
			throw new AppException(ErrorReason.C002, "Uploaded file details are required.");
		}
	}

	AssetDraft buildDraft(CreateLessonAssetInput input, String kind) {
		return new AssetDraft(
				kind,
				stringVal(input.title()),
				stringVal(input.url()).trim(),
				stringVal(input.description()),
				stringVal(input.imageUrl()),
				stringVal(input.siteName()),
				stringVal(input.originalName()).trim(),
				stringVal(input.storageKey()).trim(),
				stringVal(input.mimeType()).trim(),
				input.sizeBytes() == null ? 0L : input.sizeBytes(),
				input.metadata() == null ? new HashMap<>() : new HashMap<>(input.metadata()));
	}

	AssetDraft enrichDraft(AppUser viewer, AssetDraft draft, String kind) {
		if (LessonAssetKindCode.YOUTUBE.equals(kind)) {
			YoutubeOEmbedMetadata metadata = youtubeClient.fetchOembed(draft.url());
			draft = draft.enrichYoutube(metadata);
		}
		if (LessonAssetKindCode.LINK.equals(kind)) {
			Map<String, Object> metadata = linkMetadataService.fetch(draft.url());
			draft = draft.enrichLink(metadata);
		}
		if (isUploadedFileKind(kind)) {
			if (draft.storageKey().isBlank()) {
				throw new AppException(ErrorReason.C002, "File storage key is required.");
			}
			storageService.confirmUpload(viewer, draft.storageKey());
		}
		return draft;
	}

	boolean isUploadedFileKind(String kind) {
		return LessonAssetKindCode.IMAGE.equals(kind)
				|| LessonAssetKindCode.FILE.equals(kind)
				|| LessonAssetKindCode.VIDEO.equals(kind);
	}

	LessonAssetKind assetKind(String code) {
		return dictionaryLookupService.getLessonAssetKindReference(code);
	}

	boolean isHttpUrl(String url) {
		try {
			URI uri = URI.create(url);
			return "http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme());
		} catch (Exception ex) {
			return false;
		}
	}

	boolean isSupportedYoutubeUrl(String url) {
		try {
			String host = URI.create(url).getHost();
			return host != null && (host.contains("youtube.com") || host.contains("youtu.be"));
		} catch (Exception ex) {
			return false;
		}
	}

	String stringVal(Object value) {
		return value == null ? "" : String.valueOf(value);
	}

	private record AssetDraft(
			String kind,
			String title,
			String url,
			String description,
			String imageUrl,
			String siteName,
			String originalName,
			String storageKey,
			String mimeType,
			long sizeBytes,
			Map<String, Object> metadata
	) {

		AssetDraft enrichYoutube(YoutubeOEmbedMetadata metadata) {
			Map<String, Object> nextMetadata = new HashMap<>(metadata());
			nextMetadata.put("authorName", metadata.authorName());
			nextMetadata.put("authorUrl", metadata.authorUrl());
			nextMetadata.put("metadataError", metadata.error());
			return new AssetDraft(
					kind,
					firstNonBlank(title(), metadata.title(), "YouTube video"),
					url,
					firstNonBlank(description(), metadata.authorName()),
					firstNonBlank(imageUrl(), metadata.thumbnailUrl()),
					firstNonBlank(metadata.providerName(), "YouTube"),
					originalName,
					storageKey,
					mimeType,
					sizeBytes,
					nextMetadata);
		}

		AssetDraft enrichLink(Map<String, Object> metadata) {
			Map<String, Object> nextMetadata = new HashMap<>(metadata());
			nextMetadata.put("extractedText", stringVal(metadata.get("extractedText")));
			nextMetadata.put("metadataError", stringVal(metadata.get("error")));
			return new AssetDraft(
					kind,
					firstNonBlank(title(), stringVal(metadata.get("title")), stringVal(metadata.get("siteName")), "Web" +
							" link"),
					url,
					firstNonBlank(description(), stringVal(metadata.get("description"))),
					firstNonBlank(imageUrl(), stringVal(metadata.get("imageUrl"))),
					firstNonBlank(siteName(), stringVal(metadata.get("siteName"))),
					originalName,
					storageKey,
					mimeType,
					sizeBytes,
					nextMetadata);
		}

		String stringVal(Object value) {
			return value == null ? "" : String.valueOf(value);
		}

		String firstNonBlank(String... values) {
			for (String value : values) {
				if (value != null && !value.isBlank()) {
					return value;
				}
			}
			return "";
		}
	}
}
