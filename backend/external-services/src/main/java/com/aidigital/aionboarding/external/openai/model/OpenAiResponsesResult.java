package com.aidigital.aionboarding.external.openai.model;

/**
 * Typed result of an OpenAI Responses API call (D-04).
 *
 * @param responseId the {@code id} field from the API response, for persistence and multi-turn chaining
 * @param usage      token usage counts; may be {@code null} if the API omits the field
 * @param text       extracted output text
 */
public record OpenAiResponsesResult(
		String responseId,
		OpenAiResponsesUsage usage,
		String text
) {

}
