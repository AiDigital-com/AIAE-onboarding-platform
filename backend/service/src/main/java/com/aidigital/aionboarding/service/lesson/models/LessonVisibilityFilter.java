package com.aidigital.aionboarding.service.lesson.models;

/**
 * Security context resolved once per request and pushed into the lesson list query, mirroring
 * {@code LessonServiceImpl.canView}: a lesson is visible when the viewer is an admin, when the
 * viewer manages lessons and authored it, or when it is published.
 *
 * @param admin whether the viewer is an admin
 * @param canManageOwnLessons whether the viewer holds the lessons-manage permission
 * @param viewerUserId the viewer's internal user id
 */
public record LessonVisibilityFilter(boolean admin, boolean canManageOwnLessons, Long viewerUserId) {
}
