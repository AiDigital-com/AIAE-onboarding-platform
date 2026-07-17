package com.aidigital.aionboarding.service.lessonactivity.services.impl;

import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.lessonactivity.models.QuizAnswerResultRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.QuizGradingResultRecord;
import com.aidigital.aionboarding.service.lessonactivity.support.LessonActivityPayloadAssembler;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class LessonActivityGradingServiceImplTest {

	@Spy
	private LessonActivityPayloadAssembler payloadAssembler = new LessonActivityPayloadAssembler();

	@InjectMocks
	private LessonActivityGradingServiceImpl service;

	private Map<String, Object> singleChoiceItem() {
		Map<String, Object> item = new LinkedHashMap<>();
		item.put("type", "single_choice");
		item.put("question", "What is the capital of France?");
		item.put("options", List.of("Paris", "Berlin", "Madrid"));
		item.put("correctAnswer", "Paris");
		item.put("explanation", "Paris is the capital of France.");
		return item;
	}

	private Map<String, Object> multipleChoiceItem() {
		Map<String, Object> item = new LinkedHashMap<>();
		item.put("type", "multiple_choice");
		item.put("question", "Which are primary colors?");
		item.put("options", List.of("Red", "Blue", "Green", "Yellow"));
		item.put("correctAnswers", List.of("Red", "Blue", "Yellow"));
		item.put("explanation", "Red, blue, and yellow are primary colors.");
		return item;
	}

	private Map<String, Object> activityPayload(Map<String, Object>... items) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("items", List.of(items));
		return payload;
	}

	@Nested
	class MultipleChoiceGrading {

		@Test
		void gradeQuizShouldMarkMultipleChoiceIncorrectWhenOnlyPartOfCorrectSetIsSelectedTest() {
			// Given: 3 correct answers required, learner selects only 1 of them
			Map<String, Object> payload = activityPayload(multipleChoiceItem());
			List<List<String>> submittedAnswers = List.of(List.of("Red"));

			// When:
			QuizGradingResultRecord result = service.gradeQuiz(payload, submittedAnswers);

			// Then:
			assertThat(result.correctCount()).isZero();
			assertThat(result.results().get(0).isCorrect()).isFalse();
		}

		@Test
		void gradeQuizShouldMarkMultipleChoiceCorrectOnlyWhenExactSetIsSelectedTest() {
			// Given: learner selects exactly the correct set, in a different order
			Map<String, Object> payload = activityPayload(multipleChoiceItem());
			List<List<String>> submittedAnswers = List.of(List.of("Yellow", "Red", "Blue"));

			// When:
			QuizGradingResultRecord result = service.gradeQuiz(payload, submittedAnswers);

			// Then:
			assertThat(result.correctCount()).isEqualTo(1);
			assertThat(result.results().get(0).isCorrect()).isTrue();
		}

		@Test
		void gradeQuizShouldMarkMultipleChoiceIncorrectWhenAnExtraWrongOptionIsSelectedTest() {
			// Given: learner selects all correct options plus one wrong option
			Map<String, Object> payload = activityPayload(multipleChoiceItem());
			List<List<String>> submittedAnswers = List.of(List.of("Red", "Blue", "Yellow", "Green"));

			// When:
			QuizGradingResultRecord result = service.gradeQuiz(payload, submittedAnswers);

			// Then:
			assertThat(result.correctCount()).isZero();
			assertThat(result.results().get(0).isCorrect()).isFalse();
		}

		@Test
		void gradeQuizResultShouldExposeAllSelectedAndCorrectAnswersTest() {
			// Given:
			Map<String, Object> payload = activityPayload(multipleChoiceItem());
			List<List<String>> submittedAnswers = List.of(List.of("Red", "Blue"));

			// When:
			QuizGradingResultRecord result = service.gradeQuiz(payload, submittedAnswers);

			// Then:
			QuizAnswerResultRecord item = result.results().get(0);
			assertThat(item.selectedAnswers()).containsExactlyInAnyOrder("Red", "Blue");
			assertThat(item.correctAnswers()).containsExactlyInAnyOrder("Red", "Blue", "Yellow");
		}
	}

	@Nested
	class SingleAnswerGrading {

		@Test
		void gradeQuizShouldMarkSingleChoiceCorrectWhenSelectedAnswerMatchesTest() {
			// Given:
			Map<String, Object> payload = activityPayload(singleChoiceItem());
			List<List<String>> submittedAnswers = List.of(List.of("Paris"));

			// When:
			QuizGradingResultRecord result = service.gradeQuiz(payload, submittedAnswers);

			// Then:
			assertThat(result.correctCount()).isEqualTo(1);
			assertThat(result.results().get(0).isCorrect()).isTrue();
		}

		@Test
		void gradeQuizShouldMarkSingleChoiceIncorrectWhenSelectedAnswerDoesNotMatchTest() {
			// Given:
			Map<String, Object> payload = activityPayload(singleChoiceItem());
			List<List<String>> submittedAnswers = List.of(List.of("Berlin"));

			// When:
			QuizGradingResultRecord result = service.gradeQuiz(payload, submittedAnswers);

			// Then:
			assertThat(result.correctCount()).isZero();
			assertThat(result.results().get(0).isCorrect()).isFalse();
		}

		@Test
		void gradeQuizShouldTreatMissingAnswerForAQuestionAsUnansweredTest() {
			// Given: fewer submitted answers than questions
			Map<String, Object> payload = activityPayload(singleChoiceItem(), multipleChoiceItem());
			List<List<String>> submittedAnswers = List.of(List.of("Paris"));

			// When:
			QuizGradingResultRecord result = service.gradeQuiz(payload, submittedAnswers);

			// Then:
			assertThat(result.correctCount()).isEqualTo(1);
			assertThat(result.totalCount()).isEqualTo(2);
			assertThat(result.results().get(1).isCorrect()).isFalse();
			assertThat(result.results().get(1).selectedAnswers()).isEmpty();
		}
	}

	@Nested
	class ScoreAndPassCalculation {

		@Test
		void gradeQuizShouldScoreEightyPercentAsPassingTest() {
			// Given: 4 of 5 questions correct = 80%
			Map<String, Object> payload = activityPayload(
					singleChoiceItem(), singleChoiceItem(), singleChoiceItem(), singleChoiceItem(),
					singleChoiceItem());
			List<List<String>> submittedAnswers = List.of(
					List.of("Paris"), List.of("Paris"), List.of("Paris"), List.of("Paris"), List.of("Berlin"));

			// When:
			QuizGradingResultRecord result = service.gradeQuiz(payload, submittedAnswers);

			// Then:
			assertThat(result.score()).isEqualTo(80);
			assertThat(result.passed()).isTrue();
		}

		@Test
		void gradeQuizShouldRejectAQuestionWithNoValidCorrectAnswerTest() {
			// Given:
			Map<String, Object> invalidItem = new LinkedHashMap<>();
			invalidItem.put("type", "single_choice");
			invalidItem.put("question", "Broken question");
			invalidItem.put("options", List.of("A", "B"));
			Map<String, Object> payload = activityPayload(invalidItem);

			// When-Then:
			assertThatThrownBy(() -> service.gradeQuiz(payload, List.of(List.of("A"))))
					.isInstanceOf(AppException.class);
		}
	}
}
