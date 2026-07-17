package com.aidigital.aionboarding.service.lessonactivity.models;

import java.time.LocalDateTime;
import java.util.Map;

public record ActivityProgressViewRecord(
    String status,
    Integer score,
    Map<String, Object> metadata,
    LocalDateTime startedAt,
    LocalDateTime completedAt,
    boolean isCompleted
) { }
