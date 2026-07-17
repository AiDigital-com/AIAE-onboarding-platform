package com.aidigital.aionboarding.service.lesson.models;

import java.time.LocalDateTime;
import java.util.Map;

public record LessonAssetRecord(
		Long id,
		Long lessonId,
		String kind,
		String title,
		String name,
		String url,
		String description,
		String imageUrl,
		String siteName,
		String storageKey,
		String mimeType,
		long size,
		Map<String, Object> metadata,
		LocalDateTime createdAt
) {

}
