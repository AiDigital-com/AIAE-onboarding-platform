package com.aidigital.aionboarding.service.lessonactivity.models;

import java.time.LocalDateTime;
import java.util.Map;

public record ActivityProgressRecord(
		Long activityId,
		Long lessonId,
		String status,
		Integer score,
		LocalDateTime completedAt,
		Map<String, Object> metadata
) {

}
