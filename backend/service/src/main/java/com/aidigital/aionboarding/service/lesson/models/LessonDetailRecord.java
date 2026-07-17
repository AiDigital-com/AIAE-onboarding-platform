package com.aidigital.aionboarding.service.lesson.models;

import com.aidigital.aionboarding.service.material.models.MaterialRecord;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record LessonDetailRecord(
		Long id,
		String title,
		String description,
		String status,
		String publicationStatus,
		boolean isPublished,
		boolean isArchived,
		String userInstructions,
		String depth,
		String tone,
		String desiredFormat,
		String contentFormat,
		String contentMarkdown,
		String contentHtml,
		String coverImageStorageKey,
		String coverImageOriginalName,
		String coverImageMimeType,
		List<String> tags,
		Map<String, Object> generationMetadata,
		List<RevisionHistoryItemRecord> revisionHistory,
		String errorMessage,
		String createdBy,
		Long createdByUserId,
		LocalDateTime createdAt,
		LocalDateTime updatedAt,
		LocalDateTime publishedAt,
		List<Long> materialIds,
		List<MaterialRecord> sourceReferences,
		List<LessonAssetRecord> lessonAssets,
		LessonRoadmapContextRecord roadmapContext
) {

}
