package com.aidigital.aionboarding.service.lessonactivity.models;

import java.util.List;

/**
 * One quiz question parsed from a persisted activity's raw JSONB payload, typed at the boundary
 * so {@link com.aidigital.aionboarding.service.lessonactivity.services.LessonActivityGradingService}
 * never needs to see the raw {@code Map<String, Object>} the payload column stores.
 *
 * @param type           question type value, normalized to
 *                       single_choice/multiple_choice/true_false/fill_in_blanks_with_options
 * @param question       question text
 * @param options        answer options shown to the learner
 * @param correctAnswers correct answer value(s)
 * @param explanation    optional explanation shown after answering
 */
public record QuizQuestionItemRecord(
    String type,
    String question,
    List<String> options,
    List<String> correctAnswers,
    String explanation
) { }
