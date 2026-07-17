package com.aidigital.aionboarding.service.lesson.enums;

import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;

/**
 * Supported publication-state transitions for a lesson.
 */
public enum LessonStatusAction {
    PUBLISH("publish"),
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

    /**
     * Converts an API action value to the service enum.
     *
     * @param value API action value
     * @return matching action
     */
    public static LessonStatusAction fromValue(String value) {
        for (LessonStatusAction action : values()) {
            if (action.value.equals(value)) {
                return action;
            }
        }
        throw new AppException(ErrorReason.C002, "Unsupported lesson status action.");
    }
}
