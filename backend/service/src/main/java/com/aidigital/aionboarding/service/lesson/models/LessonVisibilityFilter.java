package com.aidigital.aionboarding.service.lesson.models;

/**
 * Security context resolved once per request and pushed into the lesson list query, mirroring
 * {@code LessonMutationSupport.canView}: a lesson is visible when the viewer is an admin, when
 * the viewer manages lessons and authored it, when it is published (Public), or when it is
 * private (assigned-only) and the viewer holds an existing enrollment ({@code user_lessons} row)
 * for it. Archived lessons are never visible through this predicate, even to an enrolled viewer.
 *
 * @param admin whether the viewer is an admin
 * @param canManageOwnLessons whether the viewer holds the lessons-manage permission
 * @param viewerUserId the viewer's internal user id
 */
public record LessonVisibilityFilter(boolean admin, boolean canManageOwnLessons, Long viewerUserId) {
}
