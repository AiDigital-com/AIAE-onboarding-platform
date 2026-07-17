package com.aidigital.aionboarding.service.lesson.enums;

/**
 * Versioned lesson creation mode.
 *
 * <p>{@code GENERATE} triggers AI generation from prepared materials.
 * {@code CREATE_MANUAL} persists pre-authored HTML directly.
 */
public enum LessonCreationModeV1 {
    GENERATE("generate"),
    CREATE_MANUAL("create-manual");

    private final String code;

    LessonCreationModeV1(String code) {
        this.code = code;
    }

    /** @return the API wire value for this mode */
    public String code() {
        return code;
    }
}
