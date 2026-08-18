package com.aidigital.aionboarding.service.lessonactivity.services.impl;

import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.lessonactivity.models.QuizAnswerResultRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.QuizGradingResultRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.QuizQuestionItemRecord;
import com.aidigital.aionboarding.service.lessonactivity.services.LessonActivityGradingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LessonActivityGradingServiceImpl implements LessonActivityGradingService {

	@Override
	public QuizGradingResultRecord gradeQuiz(List<QuizQuestionItemRecord> items,
											 List<List<String>> submittedAnswers) {
		List<List<String>> answers = submittedAnswers == null ? List.of() : submittedAnswers;
		int correctCount = 0;
		List<QuizAnswerResultRecord> results = new ArrayList<>();

		for (int index = 0; index < items.size(); index += 1) {
			QuizQuestionItemRecord item = items.get(index);
			List<String> options = item.options();
			List<String> correctAnswers = item.correctAnswers();
			if (options.isEmpty() || correctAnswers.isEmpty()) {
				throw new AppException(ErrorReason.C002, "This quiz has an invalid question and cannot be graded.");
			}

			List<String> selectedAnswers = index < answers.size() && answers.get(index) != null
					? answers.get(index)
					: List.of();
			Set<String> selectedSet = new LinkedHashSet<>();
			for (String selected : selectedAnswers) {
				if (selected != null && !selected.isBlank()) {
					selectedSet.add(selected.trim());
				}
			}
			Set<String> correctSet = new LinkedHashSet<>(correctAnswers);
			boolean isCorrect = !selectedSet.isEmpty() && selectedSet.equals(correctSet);
			if (isCorrect) {
				correctCount += 1;
			}

			results.add(new QuizAnswerResultRecord(
					item.type(),
					item.question(),
					options,
					List.copyOf(selectedSet),
					correctAnswers,
					isCorrect,
					item.explanation()
			));
		}

		int score = items.isEmpty() ? 0 : Math.round((correctCount * 100f) / items.size());
		return new QuizGradingResultRecord(score, score >= 80, correctCount, items.size(), results);
	}
}
