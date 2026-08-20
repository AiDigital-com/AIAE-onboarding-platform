package com.aidigital.aionboarding.service.lesson.enums;

/**
 * Supported publication-state transitions for a lesson.
 */
public enum LessonStatusAction {
    PUBLISH("publish"),
    UNPUBLISH("unpublish"),
    ARCHIVE("archive"),
    RESTORE("restore");

    private final String value;

    LessonStatusAction(String value) {
        this.value = value;
    }

    /**
     * Returns the API/storage value for this action.
     *
     * @return action value
     */
    public String value() {
        return value;
    }
}
