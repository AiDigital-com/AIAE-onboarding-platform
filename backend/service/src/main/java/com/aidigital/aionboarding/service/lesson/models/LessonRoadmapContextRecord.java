package com.aidigital.aionboarding.service.lesson.models;

public record LessonRoadmapContextRecord(
    Long roadmapId,
    String roadmapTitle,
    int positionInRoadmap,
    int totalLessons,
    Long previousLessonId,
    Long nextLessonId
) { }
