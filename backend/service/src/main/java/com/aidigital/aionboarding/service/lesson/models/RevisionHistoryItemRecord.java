package com.aidigital.aionboarding.service.lesson.models;

import java.util.List;
import java.util.Map;

public record RevisionHistoryItemRecord(
		String revisedAt,
		String revisionRequest,
		List<String> selectedOptions,
		RevisionBriefRecord revisionBrief,
		Map<String, Object> plannerPrompt,
		Map<String, Object> writerPrompt,
		Map<String, Object> planner,
		Map<String, Object> writer
) {

}
