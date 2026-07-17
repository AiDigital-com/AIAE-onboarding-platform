package com.aidigital.aionboarding.service.material.models;

/**
 * Bounded attachment summary for Library material search, omitting the OpenAI file-upload
 * internals carried by {@link MaterialFileRecord}.
 */
public record MaterialFileSummaryRecord(
		Long id,
		String name,
		String storageKey,
		String mimeType,
		Long size,
		String kind
) {

}
