package com.aidigital.aionboarding.service.lessonactivity.models;

public record ActivityPromptRecord(
		String version,
		String cacheKey,
		String instructions,
		String input
) {

}
