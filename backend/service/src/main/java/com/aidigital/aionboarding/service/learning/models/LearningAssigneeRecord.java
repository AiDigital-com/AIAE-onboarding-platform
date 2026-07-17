package com.aidigital.aionboarding.service.learning.models;

import java.time.LocalDateTime;

/**
 * One learner currently enrolled in a lesson or roadmap, for assignment management UIs.
 */
public record LearningAssigneeRecord(
    Long userId,
    String name,
    String email,
    LocalDateTime enrolledAt,
    Boolean isCompleted
) { }
