package com.aidigital.aionboarding.service.lesson.models;

public record RevisionResultRecord(
    LessonDetailRecord lesson,
    RevisionBriefRecord revisionBrief
) { }
