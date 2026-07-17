package com.aidigital.aionboarding.service.lessongen.prompt;

import com.aidigital.aionboarding.service.lessongen.model.ActivityCountLimits;

import java.util.Map;

public final class ActivityPromptConstants {

	public static final String LESSON_ACTIVITY_PROMPT_VERSION = "lesson-activity-v2-flashcard-front-prompts";

	public static final Map<String, ActivityCountLimits> LESSON_ACTIVITY_LIMITS = Map.of(
			"quiz", new ActivityCountLimits(3, 20, 8),
			"flashcards", new ActivityCountLimits(5, 40, 12)
	);

	private ActivityPromptConstants() {
	}
}
