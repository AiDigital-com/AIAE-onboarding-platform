package com.aidigital.aionboarding.service.lessonactivity.enums;

import org.springframework.stereotype.Component;

/**
 * Resolves a quiz question type's wire/storage value to its enum constant. Kept as an injectable
 * instance method, rather than a static factory on {@link QuizQuestionType} itself, so backend
 * beans never carry hand-written static methods.
 */
@Component
public class QuizQuestionTypeResolver {

    /**
     * Converts an API/storage question type value to the service enum, defaulting to
     * {@code MULTIPLE_CHOICE} when the value is missing or unrecognized. This keeps quiz payloads
     * persisted before question types existed rendering and grading as multiple choice.
     *
     * @param value API/storage question type value, possibly {@code null}
     * @return matching question type, defaulting to {@code MULTIPLE_CHOICE}
     */
    public QuizQuestionType resolve(String value) {
        for (QuizQuestionType type : QuizQuestionType.values()) {
            if (type.value().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return QuizQuestionType.MULTIPLE_CHOICE;
    }
}
