package com.aidigital.aionboarding.service.lessonactivity.services;

import com.aidigital.aionboarding.service.lessonactivity.models.QuizGradingResultRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.QuizQuestionItemRecord;

import java.util.List;

/**
 * Scores quiz activity submissions against stored question payloads.
 */
public interface LessonActivityGradingService {

	/**
	 * Grades submitted answers against the quiz's typed question items. Callers parse the
	 * persisted payload through
	 * {@link com.aidigital.aionboarding.service.lessonactivity.support.LessonActivityPayloadAssembler#parseQuizItems}
	 * first, so this contract never exposes the raw JSONB map.
	 *
	 * @param items             quiz question items in stored order
	 * @param submittedAnswers  learner answers in question order; each entry is the list of
	 *                          option(s) selected for that question
	 * @return score, pass flag, per-question results, and counts
	 */
	QuizGradingResultRecord gradeQuiz(List<QuizQuestionItemRecord> items, List<List<String>> submittedAnswers);
}
