package com.aidigital.aionboarding.external.openai.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Minimal typed mapping for the OpenAI Files API upload response.
 *
 * @param id uploaded file id
 * @param purpose file purpose
 * @param bytes file size in bytes
 */
public record OpenAiFileUploadResponse(
    @JsonProperty("id") String id,
    @JsonProperty("purpose") String purpose,
    @JsonProperty("bytes") Long bytes
) {
}
