package com.aidigital.aionboarding.service.learning.models;

import com.aidigital.aionboarding.service.lessonactivity.models.LessonActivityCountsRecord;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Bounded per-card "My Lessons" summary: lesson fields plus one user's enrollment and activity
 * counts, deliberately omitting full lesson content, materials, assets, and generation metadata.
 */
public record MyLessonSummaryRecord(
		Long id,
		String title,
		String description,
		String status,
		String publicationStatus,
		String coverImageStorageKey,
		String coverImageOriginalName,
		String coverImageMimeType,
		String contentHtmlPreview,
		String contentMarkdownPreview,
		boolean hasTeacherVideo,
		List<String> tags,
		String createdBy,
		LocalDateTime createdAt,
		LocalDateTime updatedAt,
		LessonEnrollmentRecord enrollment,
		LessonActivityCountsRecord activityCounts
) {

}
