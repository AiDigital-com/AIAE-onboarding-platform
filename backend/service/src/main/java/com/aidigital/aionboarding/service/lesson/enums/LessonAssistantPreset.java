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

    /**
     * Converts an API preset value to the service enum, defaulting to {@code REGULAR} when the
     * value is missing or unrecognized.
     *
     * @param value API preset value, possibly {@code null}
     * @return matching preset, defaulting to {@code REGULAR}
     */
    public static LessonAssistantPreset fromValue(String value) {
        for (LessonAssistantPreset preset : values()) {
            if (preset.value.equalsIgnoreCase(value)) {
                return preset;
            }
        }
        return REGULAR;
    }
}
