package com.aidigital.aionboarding.domain.lessonactivity.repositories;

/**
 * One (lesson, activity type) row count, for batched per-lesson activity-type summaries that
 * avoid loading full {@code LessonActivity} rows (including their JSONB payload) per card.
 */
public interface LessonActivityTypeCountProjection {

    Long getLessonId();

    String getTypeCode();

    long getActivityCount();
}
