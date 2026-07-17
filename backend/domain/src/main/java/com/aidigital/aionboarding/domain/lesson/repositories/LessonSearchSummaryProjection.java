package com.aidigital.aionboarding.domain.lesson.repositories;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Bounded per-card projection for Library lesson search: summary fields only, with content
 * deliberately truncated to a short preview rather than carrying the full lesson body,
 * generation metadata, or revision history.
 */
public record LessonSearchSummaryProjection(
    Long id,
    String title,
    String statusCode,
    String publicationStatusCode,
    String contentHtmlPreview,
    String contentMarkdownPreview,
    String coverImageStorageKey,
    String coverImageOriginalName,
    String coverImageMimeType,
    List<String> tags,
    String createdBy,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) { }
