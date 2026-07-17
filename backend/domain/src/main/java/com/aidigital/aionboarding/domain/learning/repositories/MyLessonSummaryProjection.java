package com.aidigital.aionboarding.domain.learning.repositories;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Bounded per-card projection for "My Lessons": lesson summary fields plus one user's
 * enrollment, with content deliberately truncated to a short preview rather than carrying the
 * full lesson body, materials, assets, or generation metadata.
 */
public record MyLessonSummaryProjection(
    Long lessonId,
    String title,
    String description,
    String statusCode,
    String publicationStatusCode,
    String coverImageStorageKey,
    String coverImageOriginalName,
    String coverImageMimeType,
    String contentHtmlPreview,
    String contentMarkdownPreview,
    List<String> tags,
    String createdBy,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime enrolledAt,
    LocalDateTime completedAt
) { }
