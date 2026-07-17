package com.aidigital.aionboarding.service.lesson.models;

public record LessonRevisionPromptRecord(
    String version,
    String cacheKey,
    String instructions,
    String input
) { }
