package com.aidigital.aionboarding.service.lessonactivity.enums;

/**
 * Quiz question type shown to the learner.
 */
public enum QuizQuestionType {
	SINGLE_CHOICE("single_choice"),
	MULTIPLE_CHOICE("multiple_choice"),
	TRUE_FALSE("true_false"),
	FILL_IN_BLANKS_WITH_OPTIONS("fill_in_blanks_with_options");

	private final String value;

	QuizQuestionType(String value) {
		this.value = value;
	}

	/**
	 * Returns the API/storage value for this question type.
	 *
	 * @return question type value
	 */
	public String value() {
		return value;
	}

}
