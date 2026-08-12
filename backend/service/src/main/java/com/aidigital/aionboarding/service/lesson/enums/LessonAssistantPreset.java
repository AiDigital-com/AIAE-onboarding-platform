package com.aidigital.aionboarding.service.lesson.enums;

/**
 * Assistant response mode requested by the learner for a lesson-assistant question.
 */
public enum LessonAssistantPreset {
    REGULAR("regular"),
    SMALL_PORTIONS("small_portions");

    private final String value;

    LessonAssistantPreset(String value) {
        this.value = value;
    }

    /**
     * Returns the API value for this preset.
     *
     * @return preset value
     */
    public String value() {
        return value;
    }

}
