package com.aidigital.aionboarding.service.learning.models;

import java.time.LocalDateTime;

public record LessonAssignmentEnrollmentRecord(
    Long userId,
    Long lessonId,
    LocalDateTime enrolledAt,
    LocalDateTime completedAt,
    boolean isCompleted
) { }
