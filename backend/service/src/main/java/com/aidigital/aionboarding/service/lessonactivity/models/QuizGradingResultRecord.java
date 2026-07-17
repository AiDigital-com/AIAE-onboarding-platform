package com.aidigital.aionboarding.service.lessonactivity.models;

import java.util.List;

public record QuizGradingResultRecord(
		int score,
		boolean passed,
		int correctCount,
		int totalCount,
		List<QuizAnswerResultRecord> results
) {

}
