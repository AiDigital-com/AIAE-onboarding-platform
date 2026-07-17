package com.aidigital.aionboarding.external.openai.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Minimal typed mapping for the OpenAI Responses API response body.
 *
 * @param id     response identifier returned by the API (used for chaining and persistence)
 * @param usage  token usage counts; may be {@code null} if omitted by the API
 * @param output generated output items
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenAiResponsesResponse(
		@JsonProperty("id") String id,
		@JsonProperty("usage") OpenAiResponsesUsage usage,
		@JsonProperty("output") List<OpenAiOutputItem> output
) {

}
