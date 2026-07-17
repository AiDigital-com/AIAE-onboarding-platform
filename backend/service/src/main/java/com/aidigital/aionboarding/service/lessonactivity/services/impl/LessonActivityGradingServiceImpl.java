package com.aidigital.aionboarding.service.lessonactivity.services.impl;

import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.lessonactivity.enums.QuizQuestionType;
import com.aidigital.aionboarding.service.lessonactivity.models.QuizAnswerResultRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.QuizGradingResultRecord;
import com.aidigital.aionboarding.service.lessonactivity.services.LessonActivityGradingService;
import com.aidigital.aionboarding.service.lessonactivity.support.LessonActivityPayloadAssembler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LessonActivityGradingServiceImpl implements LessonActivityGradingService {

	private final LessonActivityPayloadAssembler payloadAssembler;

	@Override
	public QuizGradingResultRecord gradeQuiz(Map<String, Object> activityPayload,
											 List<List<String>> submittedAnswers) {
		List<Map<String, Object>> items = payloadAssembler.asMapList(activityPayload.get("items"));
		List<List<String>> answers = submittedAnswers == null ? List.of() : submittedAnswers;
		int correctCount = 0;
		List<QuizAnswerResultRecord> results = new ArrayList<>();

		for (int index = 0; index < items.size(); index += 1) {
			Map<String, Object> item = items.get(index);
			List<String> options = payloadAssembler.asStringList(item.get("options"));
			List<String> correctAnswers = payloadAssembler.asStringList(item.get("correctAnswers"));
			String correctAnswer = payloadAssembler.stringVal(item.get("correctAnswer"));
			if (correctAnswers.isEmpty() && !correctAnswer.isBlank()) {
				correctAnswers = List.of(correctAnswer);
			}
			if (options.isEmpty() || correctAnswers.isEmpty()) {
				throw new AppException(ErrorReason.C002, "This quiz has an invalid question and cannot be graded.");
			}

			String type = QuizQuestionType.fromValue(payloadAssembler.stringVal(item.get("type"))).value();
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
					type,
					payloadAssembler.stringVal(item.get("question")),
					options,
					List.copyOf(selectedSet),
					correctAnswers,
					isCorrect,
					payloadAssembler.stringVal(item.get("explanation"))
			));
		}

		int score = items.isEmpty() ? 0 : Math.round((correctCount * 100f) / items.size());
		return new QuizGradingResultRecord(score, score >= 80, correctCount, items.size(), results);
	}
}
