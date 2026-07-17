package com.aidigital.aionboarding.service.lessonactivity.models;

import java.util.List;

/**
 * Author-submitted or model-generated quiz question before validation/normalization.
 *
 * @param type question type value; missing or unrecognized values are treated as multiple_choice
 * @param question question text
 * @param options candidate answer options
 * @param correctAnswer primary correct answer value (legacy single-correct field)
 * @param correctAnswers optional multi-correct answer values for multiple_choice
 * @param explanation optional explanation shown after answering
 */
public record QuizItemRecord(
    String type,
    String question,
    List<String> options,
    String correctAnswer,
    List<String> correctAnswers,
    String explanation
) { }
