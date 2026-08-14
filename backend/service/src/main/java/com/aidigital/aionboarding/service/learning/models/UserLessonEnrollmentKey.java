package com.aidigital.aionboarding.service.learning.models;

/**
 * Composite lookup key for grouping {@code UserLesson} rows by (user, lesson) pair, used when
 * batch-resolving existing enrollments before an insert-or-update fan-out.
 */
public record UserLessonEnrollmentKey(Long userId, Long lessonId) {

}
