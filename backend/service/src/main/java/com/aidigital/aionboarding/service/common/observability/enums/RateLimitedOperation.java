package com.aidigital.aionboarding.service.common.observability.enums;

/**
 * Low-cardinality identifier for an operation subject to abuse controls, used only as a
 * {@link SecurityMetrics} tag so per-operation allow/deny rates stay observable without tagging
 * by user, lesson, or any other unbounded value.
 */
public enum RateLimitedOperation {
	AI_LESSON_GENERATION("ai_lesson_generation"),
	AI_LESSON_REVISION("ai_lesson_revision"),
	AI_ACTIVITY_GENERATION("ai_activity_generation"),
	LESSON_ASSISTANT("lesson_assistant"),
	TEACHER_VIDEO("teacher_video"),
	LINK_FETCH("link_fetch"),
	UPLOAD_PRESIGN("upload_presign");

	private final String value;

	RateLimitedOperation(String value) {
		this.value = value;
	}

	/**
	 * Returns the metric tag value for this operation.
	 *
	 * @return operation value
	 */
	public String value() {
		return value;
	}
}
