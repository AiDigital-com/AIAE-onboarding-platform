package com.aidigital.aionboarding.service.lessonactivity.models;

import java.time.LocalDateTime;
import java.util.Map;

public record LessonActivityRecord(
		Long id,
		Long lessonId,
		String type,
		String title,
		int itemCount,
		Map<String, Object> payload,
		Map<String, Object> generationMetadata,
		String createdBy,
		LocalDateTime createdAt,
		ActivityProgressViewRecord progress
) {

}
