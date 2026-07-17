package com.aidigital.aionboarding.service.lesson.models;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Bounded per-card summary for Library lesson search: content is truncated to a short preview
 * rather than carrying the full lesson body, generation metadata, or revision history.
 */
public record LessonSearchSummaryRecord(
		Long id,
		String title,
		String status,
		String publicationStatus,
		String contentHtmlPreview,
		String contentMarkdownPreview,
		String coverImageStorageKey,
		String coverImageOriginalName,
		String coverImageMimeType,
		List<String> tags,
		String createdBy,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {

}
