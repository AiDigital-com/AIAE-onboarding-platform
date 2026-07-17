package com.aidigital.aionboarding.service.lessonactivity.services;

import com.aidigital.aionboarding.service.lessonactivity.models.QuizGradingResultRecord;

import java.util.List;
import java.util.Map;

/**
 * Scores quiz activity submissions against stored question payloads.
 */
public interface LessonActivityGradingService {

	/**
	 * Grades submitted answers against the quiz payload.
	 *
	 * @param activityPayload  persisted quiz activity payload containing question items
	 * @param submittedAnswers learner answers in question order; each entry is the list of
	 *                         option(s) selected for that question
	 * @return score, pass flag, per-question results, and counts
	 */
	QuizGradingResultRecord gradeQuiz(Map<String, Object> activityPayload, List<List<String>> submittedAnswers);
}
