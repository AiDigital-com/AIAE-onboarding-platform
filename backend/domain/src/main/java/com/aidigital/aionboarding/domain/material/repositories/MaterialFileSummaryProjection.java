package com.aidigital.aionboarding.domain.material.repositories;

import java.time.LocalDateTime;

/**
 * Bounded per-attachment projection for Library material search: identity and display fields
 * only, omitting the OpenAI file-upload internals carried by the full {@code MaterialFile} entity.
 */
public record MaterialFileSummaryProjection(
		Long materialId,
		Long id,
		String originalName,
		String storageKey,
		String mimeType,
		Long sizeBytes,
		String kindCode,
		LocalDateTime createdAt
) {

}
