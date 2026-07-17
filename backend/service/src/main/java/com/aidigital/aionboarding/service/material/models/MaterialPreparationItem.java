package com.aidigital.aionboarding.service.material.models;

import java.util.Map;

/**
 * A single prepared material carried through the preparation pipeline.
 *
 * <p>The {@code data} map preserves the existing per-material shape built by
 * {@code MaterialPreparationMapBuilder} (keys such as {@code title},
 * {@code description}, {@code text}, {@code links}, {@code youtubeUrls},
 * {@code youtubeVideos}, {@code youtubeTranscripts}, {@code linkAssets}, and
 * {@code attachments}). It is a transitional carrier retained until Phase 3/5
 * complete the full typed migration.
 *
 * @param id           the material id
 * @param sourceNumber the 1-based source number
 * @param data         the transitional material data map
 */
public record MaterialPreparationItem(
    Long id,
    int sourceNumber,
    Map<String, Object> data
) { }
