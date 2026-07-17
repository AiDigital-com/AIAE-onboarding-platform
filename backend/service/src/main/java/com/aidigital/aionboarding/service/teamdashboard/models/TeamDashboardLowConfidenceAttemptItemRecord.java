package com.aidigital.aionboarding.service.teamdashboard.models;

import java.time.LocalDateTime;

public record TeamDashboardLowConfidenceAttemptItemRecord(
    String id,
    String userId,
    String userName,
    String userEmail,
    String avatarStorageKey,
    String avatarColor,
    String activityId,
    String activityTitle,
    int attemptNumber,
    int score,
    boolean passed,
    int correctCount,
    int totalCount,
    LocalDateTime createdAt
) { }
