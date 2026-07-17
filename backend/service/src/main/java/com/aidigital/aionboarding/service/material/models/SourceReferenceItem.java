package com.aidigital.aionboarding.service.material.models;

import java.util.Map;

/**
 * A single source reference carried through the preparation pipeline.
 *
 * <p>The {@code data} map preserves the existing per-source-reference shape built by
 * {@code MaterialPreparationMapBuilder} (keys such as {@code title}, {@code links},
 * {@code linkAssets}, {@code youtubeUrls}, {@code youtubeVideos},
 * {@code youtubeTranscripts}, and {@code attachments}). It is a transitional carrier
 * retained until Phase 3/5 complete the full typed migration.
 *
 * @param id           the material id
 * @param sourceNumber the 1-based source number
 * @param data         the transitional source-reference data map
 */
public record SourceReferenceItem(
		Long id,
		int sourceNumber,
		Map<String, Object> data
) {

}
