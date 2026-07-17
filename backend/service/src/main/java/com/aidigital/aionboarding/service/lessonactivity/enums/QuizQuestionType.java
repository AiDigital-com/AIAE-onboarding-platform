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

	/**
	 * Converts an API/storage question type value to the service enum, defaulting to
	 * {@code MULTIPLE_CHOICE} when the value is missing or unrecognized. This keeps quiz payloads
	 * persisted before question types existed rendering and grading as multiple choice.
	 *
	 * @param value API/storage question type value, possibly {@code null}
	 * @return matching question type, defaulting to {@code MULTIPLE_CHOICE}
	 */
	public static QuizQuestionType fromValue(String value) {
		for (QuizQuestionType type : values()) {
			if (type.value.equalsIgnoreCase(value)) {
				return type;
			}
		}
		return MULTIPLE_CHOICE;
	}
}
