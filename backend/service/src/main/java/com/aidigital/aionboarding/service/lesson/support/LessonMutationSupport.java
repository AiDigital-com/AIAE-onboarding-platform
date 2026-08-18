package com.aidigital.aionboarding.service.lesson.support;

import com.aidigital.aionboarding.domain.common.dictionary.LessonPublicationStatusCode;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapLesson;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.lesson.models.UpdateLessonContentInput;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonEntityService;
import com.aidigital.aionboarding.service.lessonactivity.support.LessonActivityAccessPolicy;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import com.aidigital.aionboarding.service.roadmap.services.entity.RoadmapEntityService;
import com.aidigital.aionboarding.service.storage.StorageService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Visibility/manageability checks and input-mutation helpers for {@code LessonServiceImpl},
 * grouped into one collaborator (rather than several single-method ones) because each is used
 * by exactly one of that class's methods and splitting further would only trade Impl fields for
 * an equal number of new collaborator fields without reducing the total.
 */
@Component
@RequiredArgsConstructor
public class LessonMutationSupport {

    private static final int MAX_ROADMAP_TITLES_SHOWN = 3;

    private final PermissionService permissionService;
    private final LessonEntityService lessonEntityService;
    private final LessonActivityAccessPolicy lessonActivityAccessPolicy;
    private final StorageService storageService;
    private final LessonHtmlSanitizer lessonHtmlSanitizer;
    private final RoadmapEntityService roadmapEntityService;

    /**
     * Returns true if the viewer is allowed to see the lesson.
     *
     * @param viewer authenticated viewer
     * @param lesson lesson entity to check
     * @return {@code true} when the lesson is visible to the viewer
     */
    public boolean canView(AppUser viewer, Lesson lesson) {
        if (viewer.isAdmin()) {
            return true;
        }
        if (permissionService.userHasPermission(viewer, PermissionKeys.LESSONS_MANAGE)
            && permissionService.canManageExistingLesson(viewer,
            lesson.getCreatedByUser() == null ? null : lesson.getCreatedByUser().getId())) {
            return true;
        }
        return LessonPublicationStatusCode.PUBLISHED.equals(lesson.getPublicationStatus().getCode());
    }

    /**
     * Enforces enrollment for non-manager viewers. Called only from {@code getLesson()}.
     * Admins and LESSONS_MANAGE holders bypass the enrollment check, regardless of whether they
     * personally authored the lesson — the Library is a shared browsing/management surface, so a
     * lesson visible there (via {@link #canView}'s published-lesson fallback) must also be
     * openable. All other viewers must be enrolled; throws {@code AppException(C001)} if not.
     *
     * @param viewer authenticated viewer
     * @param lesson lesson entity being opened
     */
    public void requireLearnerAccess(AppUser viewer, Lesson lesson) {
        if (viewer.isAdmin()) {
            return;
        }
        if (permissionService.userHasPermission(viewer, PermissionKeys.LESSONS_MANAGE)) {
            return;
        }
        lessonActivityAccessPolicy.requireEnrollment(viewer, lesson.getId());
    }

    /**
     * Loads the lesson and verifies the viewer can manage it.
     *
     * @param viewer authenticated viewer
     * @param id     lesson primary key
     * @return the loaded lesson
     * @throws AppException C001/C004 if not found or not manageable
     */
    public Lesson requireManageable(AppUser viewer, Long id) {
        Lesson lesson = lessonEntityService.getReference(id);
        if (!permissionService.canManageExistingLesson(viewer, lesson.getCreatedByUser() == null
            ? null : lesson.getCreatedByUser().getId())) {
            throw new AppException(ErrorReason.C004);
        }
        return lesson;
    }

    /**
     * Applies the editable-field updates from an {@link UpdateLessonContentInput} onto the given
     * lesson in place, sanitizing HTML content and confirming any newly referenced cover-image
     * upload.
     *
     * @param viewer the authenticated user making the update, used to confirm cover-image uploads
     * @param lesson the lesson entity to mutate
     * @param input  the requested field updates
     */
    public void applyContentUpdate(AppUser viewer, Lesson lesson, UpdateLessonContentInput input) {
        input.title().ifPresent(title -> lesson.setTitle(stringVal(title)));
        input.contentMarkdown().ifPresent(markdown -> lesson.setContentMarkdown(stringVal(markdown)));
        input.contentHtml().ifPresent(html -> lesson.setContentHtml(lessonHtmlSanitizer.sanitize(stringVal(html))));
        input.tags().ifPresent(tags -> lesson.setTags(new ArrayList<>(tags)));
        input.coverImageStorageKey().ifPresent(key -> {
            String value = stringVal(key);
            if (!value.isBlank() && !value.equals(lesson.getCoverImageStorageKey())) {
                storageService.confirmUpload(viewer, value);
            }
            lesson.setCoverImageStorageKey(value);
        });
        input.coverImageOriginalName().ifPresent(name -> lesson.setCoverImageOriginalName(stringVal(name)));
        input.coverImageMimeType().ifPresent(type -> lesson.setCoverImageMimeType(stringVal(type)));
    }

    /**
     * Ensures the lesson has a non-blank title and body before save/publish.
     *
     * @param lesson the lesson to validate
     * @throws AppException C002 when title or content is missing or too long
     */
    public void requirePublishableContent(Lesson lesson) {
        String title = stringVal(lesson.getTitle()).trim();
        if (title.isEmpty()) {
            throw new AppException(ErrorReason.C002, "Title is required.");
        }
        if (title.length() > 100) {
            throw new AppException(ErrorReason.C002, "Title must be at most 100 characters.");
        }
        String plainContent = stripHtmlToText(
            firstNonBlank(lesson.getContentHtml(), lesson.getContentMarkdown())
        );
        if (plainContent.isEmpty()) {
            throw new AppException(ErrorReason.C002, "Lesson content is required.");
        }
    }

    /**
     * Returns the first non-blank (trimmed) value between the two candidates.
     *
     * @param first  first candidate value
     * @param second fallback candidate value
     * @return the first non-blank value, or empty string when both are blank
     */
    String firstNonBlank(String first, String second) {
        String left = stringVal(first).trim();
        if (!left.isEmpty()) {
            return left;
        }
        return stringVal(second).trim();
    }

    /**
     * Strips HTML tags and collapses whitespace, leaving plain text.
     *
     * @param value raw HTML/markdown-adjacent value
     * @return plain text with tags removed and whitespace collapsed
     */
    String stripHtmlToText(String value) {
        return stringVal(value)
            .replaceAll("(?is)<[^>]+>", " ")
            .replace("&nbsp;", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }

    /**
     * Asserts that a lesson is not referenced by any roadmap, since a roadmap-referenced lesson
     * cannot be deleted.
     *
     * @param lessonId the lesson being considered for deletion
     * @throws AppException C006 when the lesson is used by one or more roadmaps
     */
    public void requireNoRoadmapUsage(Long lessonId) {
        List<RoadmapLesson> usages = roadmapEntityService.findByIdLessonId(lessonId);
        if (usages.isEmpty()) {
            return;
        }
        String titles = usages.stream()
            .map(rl -> rl.getRoadmap().getTitle())
            .distinct()
            .limit(MAX_ROADMAP_TITLES_SHOWN)
            .reduce((a, b) -> a + "\", \"" + b)
            .orElse("");
        String suffix = usages.size() > MAX_ROADMAP_TITLES_SHOWN ? " and more" : "";
        throw new AppException(ErrorReason.C006,
            "Lesson is used in roadmap" + (usages.size() == 1 ? " \"" : "s \"") + titles + "\"" + suffix);
    }

    /**
     * Converts a nullable value to a non-null string.
     *
     * @param value raw value
     * @return string value or empty string
     */
    String stringVal(String value) {
        return value == null ? "" : value;
    }
}
