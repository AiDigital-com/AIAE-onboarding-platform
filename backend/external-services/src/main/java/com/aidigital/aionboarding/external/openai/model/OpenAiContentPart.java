package com.aidigital.aionboarding.external.openai.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single content part within an {@link OpenAiOutputItem}.
 *
 * @param type item content type (e.g. {@code "output_text"})
 * @param text extracted text value; present when {@code type} is {@code "output_text"}
 */
public record OpenAiContentPart(
    @JsonProperty("type") String type,
    @JsonProperty("text") String text
) {
}
