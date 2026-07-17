package com.aidigital.aionboarding.service.lesson.models;

public record TeacherVideoRecord(
    String provider,
    String prompt,
    String avatarId,
    String voiceId,
    String sessionId,
    String videoId,
    String status,
    String createdAt,
    Integer durationLimitSeconds,
    String checkedAt,
    String videoUrl,
    String thumbnailUrl,
    Object duration,
    String completedAt,
    String failedAt
) { }
