package com.aidigital.aionboarding.external.openai.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * A single output item from the OpenAI Responses API response body.
 *
 * @param type    item type (e.g. {@code "message"})
 * @param content list of content parts contained in this item
 */
public record OpenAiOutputItem(
    @JsonProperty("type") String type,
    @JsonProperty("content") List<OpenAiContentPart> content
) {
}
