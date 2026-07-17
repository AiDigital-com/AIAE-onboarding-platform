package com.aidigital.aionboarding.service.lesson.models;

import java.time.LocalDateTime;
import java.util.List;

public record LessonSummaryRecord(
    Long id,
    String title,
    String status,
    String publicationStatus,
    String contentMarkdown,
    String contentHtml,
    String coverImageStorageKey,
    String coverImageOriginalName,
    String coverImageMimeType,
    String createdBy,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<String> tags
) { }
