package com.aidigital.aionboarding.service.lessonactivity.models;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record ActivityAttemptRecord(
    Long id,
    Long userId,
    Long lessonId,
    Long activityId,
    String type,
    int attemptNumber,
    int score,
    boolean passed,
    int correctCount,
    int totalCount,
    List<List<String>> submittedAnswers,
    List<QuizAnswerResultRecord> results,
    Map<String, Object> metadata,
    LocalDateTime createdAt
) { }
