package com.aidigital.aionboarding.service.roadmap.models;

import java.time.LocalDateTime;

public record RoadmapLessonRecord(
		Long id,
		String title,
		String description,
		String status,
		LocalDateTime createdAt,
		Integer sortOrder,
		Boolean isCompleted
) {

}
