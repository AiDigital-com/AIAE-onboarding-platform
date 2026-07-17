package com.aidigital.aionboarding.service.lesson.support;

import com.aidigital.aionboarding.domain.common.dictionary.LessonStatusCode;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.lesson.models.ReviseLessonInput;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LessonRevisionRequestValidator {

    private final PermissionService permissionService;
    private final com.aidigital.aionboarding.service.lesson.prompt.LessonRevisionPromptBuilder lessonRevisionPromptBuilder;

    public ValidatedRevisionRequest validate(AppUser viewer, Lesson lesson, ReviseLessonInput request) {
        if (!permissionService.canManageExistingLesson(viewer,
            lesson.getCreatedByUser() == null ? null : lesson.getCreatedByUser().getId())) {
            throw new AppException(ErrorReason.C004);
        }
        if (!LessonStatusCode.READY.equals(lesson.getStatus().getCode())) {
            throw new AppException(ErrorReason.C002, "Only ready lessons can be revised.");
        }
        if (lesson.getContentHtml() == null || lesson.getContentHtml().isBlank()) {
            throw new AppException(ErrorReason.C002, "Lesson content is empty.");
        }

        String revisionRequest = stringVal(request.revisionRequest()).trim();
        List<String> selectedOptions = sanitizeRevisionOptions(request.selectedOptions());
        if (revisionRequest.isBlank() && selectedOptions.isEmpty()) {
            throw new AppException(ErrorReason.C002,
                "Add revision notes or select at least one revision option.");
        }
        return new ValidatedRevisionRequest(revisionRequest, selectedOptions);
    }

    List<String> sanitizeRevisionOptions(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        return raw.stream()
            .map(String::valueOf)
            .filter(lessonRevisionPromptBuilder.allowedRevisionOptions()::contains)
            .distinct()
            .toList();
    }

    String stringVal(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    public record ValidatedRevisionRequest(String revisionRequest, List<String> selectedOptions) { }
}
