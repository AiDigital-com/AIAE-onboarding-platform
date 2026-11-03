package com.aidigital.aionboarding.service.learning.models;

import java.time.LocalDateTime;
import java.util.List;

/**
 * One learner currently enrolled in a lesson or roadmap, for assignment management UIs.
 *
 * @param userId        enrolled user id
 * @param name          display name
 * @param email         email address
 * @param enrolledAt    when the enrollment was created
 * @param isCompleted   completion state when known, otherwise {@code null}
 * @param autoAssigned  whether the enrollment came from the roadmap's "auto-assign to new users" rule
 * @param viaGroupNames groups whose standing roadmap assignment currently covers this learner; empty for lessons
 */
public record LearningAssigneeRecord(
    Long userId,
    String name,
    String email,
    LocalDateTime enrolledAt,
    Boolean isCompleted,
    boolean autoAssigned,
    List<String> viaGroupNames
) { }
