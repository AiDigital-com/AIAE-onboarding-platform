package com.aidigital.aionboarding.service.lesson.models;

import java.util.Map;

public record CreateLessonAssetInput(
		String kind,
		String url,
		String storageKey,
		String mimeType,
		String originalName,
		Long sizeBytes,
		String title,
		String description,
		String imageUrl,
		String siteName,
		Map<String, Object> metadata
) {

}
