package com.aidigital.aionboarding.service.roadmap.models;

/**
 * Identifies one user's enrollment in one lesson, used to deduplicate enrollment fan-out rows
 * without a database round trip per pair.
 *
 * @param userId   enrolled user's internal id
 * @param lessonId enrolled lesson's id
 */
public record RoadmapLessonEnrollmentKey(Long userId, Long lessonId) {
}
