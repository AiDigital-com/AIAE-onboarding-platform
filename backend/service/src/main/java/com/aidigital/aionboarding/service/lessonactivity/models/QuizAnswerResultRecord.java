package com.aidigital.aionboarding.service.lessonactivity.models;

import java.util.List;

/**
 * Graded result for one quiz question, including enough context to re-render the question and its
 * outcome in the learner review UI.
 *
 * @param type question type value, normalized to single_choice/multiple_choice/true_false/fill_in_blanks_with_options
 * @param question question text
 * @param options answer options shown to the learner
 * @param selectedAnswers answer(s) selected by the learner
 * @param correctAnswers correct answer value(s)
 * @param isCorrect whether the submitted answer set exactly matches the correct answer set
 * @param explanation optional explanation shown after answering
 */
public record QuizAnswerResultRecord(
    String type,
    String question,
    List<String> options,
    List<String> selectedAnswers,
    List<String> correctAnswers,
    boolean isCorrect,
    String explanation
) { }
