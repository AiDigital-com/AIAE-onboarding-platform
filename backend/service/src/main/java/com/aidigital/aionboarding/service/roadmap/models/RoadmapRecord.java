package com.aidigital.aionboarding.service.roadmap.models;

import java.time.LocalDateTime;
import java.util.List;

public record RoadmapRecord(
    Long id,
    String title,
    String description,
    List<String> tags,
    List<Long> lessonIds,
    List<RoadmapLessonRecord> lessons,
    Boolean isEnrolled,
    LocalDateTime enrolledAt,
    Boolean viewerCanManage,
    String createdBy,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) { }
