package com.aidigital.aionboarding.service.lesson.enums;

import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import org.springframework.stereotype.Component;

/**
 * Resolves a lesson status action's wire value to its enum constant. Kept as an injectable
 * instance method, rather than a static factory on {@link LessonStatusAction} itself, so backend
 * beans never carry hand-written static methods.
 */
@Component
public class LessonStatusActionResolver {

    /**
     * Converts an API action value to the service enum.
     *
     * @param value API action value
     * @return matching action
     * @throws AppException with {@link ErrorReason#C002} when the value matches no known action
     */
    public LessonStatusAction resolve(String value) {
        for (LessonStatusAction action : LessonStatusAction.values()) {
            if (action.value().equals(value)) {
                return action;
            }
        }
        throw new AppException(ErrorReason.C002, "Unsupported lesson status action.");
    }
}
