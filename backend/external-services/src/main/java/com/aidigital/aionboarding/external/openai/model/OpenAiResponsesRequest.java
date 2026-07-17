package com.aidigital.aionboarding.external.openai.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request body for the OpenAI Responses API ({@code POST /v1/responses}).
 *
 * @param model              model identifier
 * @param instructions       top-level system instruction (D-01 — must NOT be placed in the {@code input} array)
 * @param input              plain-text user message or structured input items
 * @param promptCacheKey     optional cache key for server-side prompt caching (D-03)
 * @param store              optional flag to persist the response in OpenAI storage
 * @param previousResponseId optional ID of a prior response for multi-turn chaining
 * @param maxOutputTokens    optional output token cap; omitted when {@code null} (D-02 — callers must explicitly
 *                           supply)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OpenAiResponsesRequest(
		@JsonProperty("model") String model,
		@JsonProperty("instructions") String instructions,
		@JsonProperty("input") Object input,
		@JsonProperty("prompt_cache_key") String promptCacheKey,
		@JsonProperty("store") Boolean store,
		@JsonProperty("previous_response_id") String previousResponseId,
		@JsonProperty("max_output_tokens") Integer maxOutputTokens
) {

}
