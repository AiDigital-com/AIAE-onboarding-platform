package com.aidigital.aionboarding.service.lessonactivity.support;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class QuizQuestionValidatorTest {

	private final QuizQuestionValidator validator = new QuizQuestionValidator();

	@Test
	void normalizeMultipleChoiceQuestionShouldReturnNormalizedPayloadTest() {
		// Given:
		List<String> options = List.of("Paris", "Berlin", "Madrid");

		// When:
		Map<String, Object> normalized = validator.normalize(
				"multiple_choice", " What is the capital of France? ", options, "Paris", "It is Paris.");

		// Then:
		assertThat(normalized).isNotNull();
		assertThat(normalized.get("type")).isEqualTo("multiple_choice");
		assertThat(normalized.get("question")).isEqualTo("What is the capital of France?");
		assertThat(normalized.get("options")).isEqualTo(options);
		assertThat(normalized.get("correctAnswer")).isEqualTo("Paris");
		assertThat(normalized.get("explanation")).isEqualTo("It is Paris.");
	}

	@Test
	void normalizeSingleChoiceQuestionShouldReturnNormalizedPayloadTest() {
		// Given:
		List<String> options = List.of("Paris", "Berlin", "Madrid");

		// When:
		Map<String, Object> normalized = validator.normalize(
				"single_choice", " What is the capital of France? ", options, "Paris", "It is Paris.");

		// Then:
		assertThat(normalized).isNotNull();
		assertThat(normalized.get("type")).isEqualTo("single_choice");
		assertThat(normalized.get("options")).isEqualTo(options);
		assertThat(normalized.get("correctAnswer")).isEqualTo("Paris");
		assertThat(normalized.get("correctAnswers")).isNull();
	}

	@Test
	void normalizeSingleChoiceQuestionWithMultipleCorrectAnswersShouldKeepOnlyFirstTest() {
		// Given:
		List<String> options = List.of("A", "B", "C", "D");

		// When:
		Map<String, Object> normalized = validator.normalize(
				"single_choice", "Pick the right one", options, "A", List.of("A", "C"), "");

		// Then:
		assertThat(normalized).isNotNull();
		assertThat(normalized.get("correctAnswer")).isEqualTo("A");
		assertThat(normalized.get("correctAnswers")).isNull();
	}

	@Test
	void normalizeMultipleChoiceWithMultipleCorrectAnswersShouldStoreCorrectAnswersTest() {
		// Given:
		List<String> options = List.of("A", "B", "C", "D");

		// When:
		Map<String, Object> normalized = validator.normalize(
				"multiple_choice", "Select all true statements", options, "A", List.of("A", "C"), "");

		// Then:
		assertThat(normalized).isNotNull();
		assertThat(normalized.get("correctAnswer")).isEqualTo("A");
		assertThat(normalized.get("correctAnswers")).isEqualTo(List.of("A", "C"));
	}

	@Test
	void normalizeMultipleChoiceWithDuplicateOptionsShouldReturnNullTest() {
		// When:
		Map<String, Object> normalized = validator.normalize(
				"multiple_choice", "Pick one", List.of("Same", "same", "Other"), "Same", "");

		// Then:
		assertThat(normalized).isNull();
		assertThat(validator.describeFailure(
				"multiple_choice", "Pick one", List.of("Same", "same", "Other"), "Same"))
				.isEqualTo("option text must be unique within a question");
	}

	@Test
	void normalizeQuestionWithMissingTypeShouldDefaultToMultipleChoiceTest() {
		// When:
		Map<String, Object> normalized = validator.normalize(
				null, "What is the capital of France?", List.of("Paris", "Berlin"), "Paris", "");

		// Then:
		assertThat(normalized).isNotNull();
		assertThat(normalized.get("type")).isEqualTo("multiple_choice");
	}

	@Test
	void normalizeMultipleChoiceQuestionWithUnmatchedCorrectAnswerShouldReturnNullTest() {
		// When:
		Map<String, Object> normalized = validator.normalize(
				"multiple_choice", "What is the capital of France?", List.of("Paris", "Berlin"), "Rome", "");

		// Then:
		assertThat(normalized).isNull();
	}

	@Test
	void normalizeQuestionWithBlankTextShouldReturnNullTest() {
		// When:
		Map<String, Object> normalized = validator.normalize(
				"multiple_choice", "   ", List.of("Paris", "Berlin"), "Paris", "");

		// Then:
		assertThat(normalized).isNull();
	}

	@Test
	void normalizeTrueFalseQuestionShouldForceCanonicalOptionsAndAnswerTest() {
		// When:
		Map<String, Object> normalized = validator.normalize(
				"true_false", "The sky is blue.", List.of("yes", "no"), "false", "");

		// Then:
		assertThat(normalized).isNotNull();
		assertThat(normalized.get("options")).isEqualTo(List.of("True", "False"));
		assertThat(normalized.get("correctAnswer")).isEqualTo("False");
	}

	@Test
	void normalizeTrueFalseQuestionWithInvalidCorrectAnswerShouldReturnNullTest() {
		// When:
		Map<String, Object> normalized = validator.normalize(
				"true_false", "The sky is blue.", List.of("True", "False"), "maybe", "");

		// Then:
		assertThat(normalized).isNull();
	}

	@Test
	void normalizeFillInBlanksQuestionWithoutBlankMarkerShouldReturnNullTest() {
		// When:
		Map<String, Object> normalized = validator.normalize(
				"fill_in_blanks_with_options", "The capital of France is Paris.",
				List.of("Paris", "Berlin"), "Paris", "");

		// Then:
		assertThat(normalized).isNull();
	}

	@Test
	void normalizeFillInBlanksQuestionShouldReturnNormalizedPayloadTest() {
		// When:
		Map<String, Object> normalized = validator.normalize(
				"fill_in_blanks_with_options", "The capital of France is _____.",
				List.of("Paris", "Berlin"), "Paris", "It is Paris.");

		// Then:
		assertThat(normalized).isNotNull();
		assertThat(normalized.get("type")).isEqualTo("fill_in_blanks_with_options");
		assertThat(normalized.get("options")).isEqualTo(List.of("Paris", "Berlin"));
		assertThat(normalized.get("correctAnswer")).isEqualTo("Paris");
	}

	@Test
	void normalizeFillInBlanksQuestionShouldAcceptNonStandardUnderscoreCountsTest() {
		// Given-When: models are inconsistent about the exact underscore count in the marker
		Map<String, Object> normalizedThree = validator.normalize(
				"fill_in_blanks_with_options", "The capital of France is ___.",
				List.of("Paris", "Berlin"), "Paris", "");
		Map<String, Object> normalizedSix = validator.normalize(
				"fill_in_blanks_with_options", "The capital of France is ______.",
				List.of("Paris", "Berlin"), "Paris", "");

		// Then:
		assertThat(normalizedThree).isNotNull();
		assertThat(normalizedSix).isNotNull();
	}

	@Test
	void normalizeFillInBlanksQuestionWithTwoBlankMarkersShouldReturnNullTest() {
		// When:
		Map<String, Object> normalized = validator.normalize(
				"fill_in_blanks_with_options", "The capital of _____ is _____.",
				List.of("Paris", "Berlin"), "Paris", "");

		// Then:
		assertThat(normalized).isNull();
	}

	@Test
	void normalizeQuestionWithFewerThanTwoOptionsShouldReturnNullTest() {
		// When:
		Map<String, Object> normalized = validator.normalize(
				"multiple_choice", "What is the capital of France?", List.of("Paris"), "Paris", "");

		// Then:
		assertThat(normalized).isNull();
	}

	@Test
	void describeFailureForBlankQuestionShouldExplainTest() {
		// When:
		String reason = validator.describeFailure("multiple_choice", "   ", List.of("Paris", "Berlin"), "Paris");

		// Then:
		assertThat(reason).isEqualTo("add a question");
	}

	@Test
	void describeFailureForFillInBlanksWithoutMarkerShouldExplainTest() {
		// When:
		String reason = validator.describeFailure(
				"fill_in_blanks_with_options", "The capital of France is Paris.", List.of("Paris", "Berlin"), "Paris");

		// Then:
		assertThat(reason).isEqualTo("include exactly one blank marker, e.g. " + QuizQuestionValidator.FILL_IN_BLANKS_MARKER);
	}

	@Test
	void describeFailureForTooFewOptionsShouldExplainTest() {
		// When:
		String reason = validator.describeFailure(
				"multiple_choice", "What is the capital of France?", List.of("Paris"), "Paris");

		// Then:
		assertThat(reason).isEqualTo("add at least two answer options");
	}

	@Test
	void describeFailureForUnmatchedCorrectAnswerShouldExplainTest() {
		// When:
		String reason = validator.describeFailure(
				"true_false", "The sky is blue.", List.of("True", "False"), "maybe");

		// Then:
		assertThat(reason).isEqualTo("select the correct answer");
	}

	@Test
	void describeFailureForValidQuestionShouldReturnNullTest() {
		// When:
		String reason = validator.describeFailure(
				"multiple_choice", "What is the capital of France?", List.of("Paris", "Berlin"), "Paris");

		// Then:
		assertThat(reason).isNull();
	}
}
