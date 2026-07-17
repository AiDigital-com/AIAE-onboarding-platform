package com.aidigital.aionboarding.service.lessonactivity.models;

import java.util.List;

public record LessonWithActivitiesRecord(
    Long id,
    String title,
    String description,
    String status,
    String publicationStatus,
    String contentMarkdown,
    String contentHtml,
    List<LessonActivityRecord> activities
) { }
