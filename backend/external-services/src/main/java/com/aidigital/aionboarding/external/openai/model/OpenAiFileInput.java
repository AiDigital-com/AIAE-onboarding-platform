package com.aidigital.aionboarding.external.openai.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single file input item for an OpenAI Responses API structured request (D-09).
 *
 * @param type   item type; always {@code "input_file"}
 * @param fileId the OpenAI Files API file ID
 */
public record OpenAiFileInput(
		@JsonProperty("type") String type,
		@JsonProperty("file_id") String fileId
) {

}
