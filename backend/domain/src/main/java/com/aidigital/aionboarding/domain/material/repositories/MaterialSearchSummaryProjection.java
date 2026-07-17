package com.aidigital.aionboarding.domain.material.repositories;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Bounded per-card projection for Library material search: base material fields only, with the
 * full body text replaced by a presence flag rather than shipped to the client.
 */
public record MaterialSearchSummaryProjection(
    Long id,
    String title,
    String description,
    boolean hasText,
    String coverImageStorageKey,
    String coverImageOriginalName,
    String coverImageMimeType,
    Long createdByUserId,
    String createdBy,
    List<String> tags,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) { }
