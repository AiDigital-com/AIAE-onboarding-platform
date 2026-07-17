package com.aidigital.aionboarding.external.openai.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Token usage reported by the OpenAI Responses API.
 *
 * @param inputTokens  prompt token count
 * @param outputTokens completion token count
 * @param totalTokens  sum of input and output tokens
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenAiResponsesUsage(
		@JsonProperty("input_tokens") Integer inputTokens,
		@JsonProperty("output_tokens") Integer outputTokens,
		@JsonProperty("total_tokens") Integer totalTokens
) {

}
