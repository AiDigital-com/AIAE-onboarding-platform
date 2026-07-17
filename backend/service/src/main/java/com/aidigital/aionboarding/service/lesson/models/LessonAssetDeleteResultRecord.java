package com.aidigital.aionboarding.service.lesson.models;

/**
 * Refreshed lesson detail returned after a lesson asset is removed.
 *
 * @param lesson lesson detail after the asset removal has been applied
 */
public record LessonAssetDeleteResultRecord(
    LessonDetailRecord lesson
) { }
