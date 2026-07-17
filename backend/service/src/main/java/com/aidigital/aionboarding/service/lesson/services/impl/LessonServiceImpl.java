package com.aidigital.aionboarding.service.lesson.services.impl;

import com.aidigital.aionboarding.domain.common.dictionary.LessonPublicationStatusCode;
import com.aidigital.aionboarding.domain.common.dictionary.LessonStatusCode;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonPublicationStatus;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapLesson;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.lesson.enums.LessonStatusAction;
import com.aidigital.aionboarding.service.lesson.models.CreateLessonAssetInput;
import com.aidigital.aionboarding.service.lesson.models.CreateLessonInput;
import com.aidigital.aionboarding.service.lesson.models.LessonAssetDeleteResultRecord;
import com.aidigital.aionboarding.service.lesson.models.LessonAssetResultRecord;
import com.aidigital.aionboarding.service.lesson.models.LessonDetailRecord;
import com.aidigital.aionboarding.service.lesson.models.LessonListQuery;
import com.aidigital.aionboarding.service.lesson.models.LessonSearchSummaryRecord;
import com.aidigital.aionboarding.service.lesson.models.LessonSummaryRecord;
import com.aidigital.aionboarding.service.lesson.models.LessonVisibilityFilter;
import com.aidigital.aionboarding.service.lesson.models.UpdateLessonContentInput;
import com.aidigital.aionboarding.service.lesson.services.LessonAssetService;
import com.aidigital.aionboarding.service.lesson.services.LessonInitialGenerationService;
import com.aidigital.aionboarding.service.lesson.services.LessonService;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonEntityService;
import com.aidigital.aionboarding.service.lesson.support.LessonDetailEnricher;
import com.aidigital.aionboarding.service.lesson.support.LessonHtmlSanitizer;
import com.aidigital.aionboarding.service.lesson.support.LessonRecordAssembler;
import com.aidigital.aionboarding.service.lessonactivity.support.LessonActivityAccessPolicy;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import com.aidigital.aionboarding.service.roadmap.services.entity.RoadmapEntityService;
import com.aidigital.aionboarding.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LessonServiceImpl implements LessonService {

    private static final int MAX_ROADMAP_TITLES_SHOWN = 3;

    private final CurrentTime currentTime;
    private final RoadmapEntityService roadmapEntityService;
    private final PermissionService permissionService;
    private final LessonRecordAssembler lessonMapper;
    private final LessonAssetService lessonAssetService;
    private final LessonInitialGenerationService lessonInitialGenerationService;
    private final LessonEntityService lessonEntityService;
    private final LessonActivityAccessPolicy lessonActivityAccessPolicy;
    private final LessonDetailEnricher lessonDetailEnricher;
    private final StorageService storageService;
    private final LessonHtmlSanitizer lessonHtmlSanitizer;

    /** Returns a bounded page of lesson summaries visible to the viewer based on ownership and publication status. */
    @Override
    @Transactional(readOnly = true)
    public Page<LessonSearchSummaryRecord> getAllLessons(AppUser viewer, LessonListQuery query, int page, int size) {
        LessonVisibilityFilter visibility = new LessonVisibilityFilter(
            viewer.isAdmin(),
            permissionService.userHasPermission(viewer, PermissionKeys.LESSONS_MANAGE),
            viewer.internalId()
        );
        return lessonEntityService.searchSummaries(query, visibility, page, size).map(lessonMapper::toListItemRecord);
    }

    /** Counts lessons visible to the viewer matching the given filter. */
    @Override
    @Transactional(readOnly = true)
    public long countLessons(AppUser viewer, LessonListQuery query) {
        LessonVisibilityFilter visibility = new LessonVisibilityFilter(
            viewer.isAdmin(),
            permissionService.userHasPermission(viewer, PermissionKeys.LESSONS_MANAGE),
            viewer.internalId()
        );
        return lessonEntityService.countSummaries(query, visibility);
    }

    /**
     * Returns the detail record for a lesson the viewer is allowed to see.
     *
     * <p>This method has no {@code @Transactional} annotation so that the HeyGen network call
     * inside {@code refreshTeacherVideoIfNeeded} runs outside any DB transaction. The lesson is
     * loaded by {@link LessonEntityService#getReference(Long)} which opens its own short
     * read-only transaction and closes it before returning.
     *
     * @throws AppException C001 if not found or not visible
     * @throws AppException C001 if viewer is not enrolled (unenrolled non-manager on published lesson)
     */
    @Override
    public LessonDetailRecord getLesson(AppUser viewer, Long id) {
        Lesson lesson = lessonEntityService.findByIdWithFetches(id);
        if (!canView(viewer, lesson)) {
            throw new AppException(ErrorReason.C001, id);
        }
        requireLearnerAccess(viewer, lesson);

        return lessonDetailEnricher.toEnrichedDetailRecord(viewer, lesson);
    }

    /**
     * Returns only a lesson's current generation status, reusing the same visibility rule as
     * {@link #getLesson}, for lightweight polling while a lesson is generating.
     *
     * @throws AppException C001 if not found or not visible
     */
    @Override
    @Transactional(readOnly = true)
    public String getLessonGenerationStatus(AppUser viewer, Long id) {
        Lesson lesson = lessonEntityService.getReference(id);
        if (!canView(viewer, lesson)) {
            throw new AppException(ErrorReason.C001, id);
        }
        return lesson.getStatus().getCode();
    }

    /**
     * Creates a lesson (AI-generated or manual) after checking LESSONS_CREATE permission.
     * <p>
     * The generated lesson is reloaded with eager JOIN FETCHes on {@code status} and
     * {@code publicationStatus} before assembly to avoid proxy initialisation exceptions on the
     * detached entity returned by the generation service. This reload runs in its own short
     * read-only transaction, preserving the intentional design of no DB transaction spanning the
     * OpenAI generation call.
     *
     * @param viewer the authenticated user creating the lesson
     * @param input  lesson creation parameters
     * @return the assembled {@link LessonSummaryRecord} for the newly created lesson
     */
    @Override
    public LessonSummaryRecord createLesson(AppUser viewer, CreateLessonInput input) {
        permissionService.requirePermission(viewer, PermissionKeys.LESSONS_CREATE);
        Lesson generatedLesson = lessonInitialGenerationService.generate(viewer, input);
        Lesson fullyLoaded = lessonEntityService.findByIdWithFetches(generatedLesson.getId());
        return lessonMapper.toSummaryRecord(fullyLoaded);
    }

    /** Updates editable content fields of a lesson the viewer manages. */
    @Override
    @Transactional
    public LessonDetailRecord updateLessonContent(AppUser viewer, Long id, UpdateLessonContentInput input) {
        Lesson lesson = requireManageable(viewer, id);
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
        requirePublishableContent(lesson);
        lessonEntityService.clearFailureIfPresent(lesson);
        lesson.setUpdatedAt(currentTime.utcDateTime());
        return lessonMapper.toDetailRecord(lessonEntityService.save(lesson));
    }

    /** Publishes, archives, or restores a lesson after checking LESSONS_PUBLISH_ARCHIVE permission. */
    @Override
    @Transactional
    public LessonDetailRecord changeLessonStatus(AppUser viewer, Long id, LessonStatusAction action) {
        permissionService.requirePermission(viewer, PermissionKeys.LESSONS_PUBLISH_ARCHIVE);
        Lesson lesson = requireManageable(viewer, id);
        switch (action) {
            case PUBLISH -> {
                if (!LessonStatusCode.READY.equals(lesson.getStatus().getCode())) {
                    throw new AppException(ErrorReason.C002, "Lesson must be ready before publishing.");
                }
                requirePublishableContent(lesson);
                lesson.setPublicationStatus(publication(LessonPublicationStatusCode.PUBLISHED));
                lesson.setPublishedAt(currentTime.utcDateTime());
            }
            case ARCHIVE -> lesson.setPublicationStatus(publication(LessonPublicationStatusCode.ARCHIVED));
            case RESTORE -> lesson.setPublicationStatus(publication(LessonPublicationStatusCode.PRIVATE));
        }
        lesson.setUpdatedAt(currentTime.utcDateTime());
        return lessonMapper.toDetailRecord(lessonEntityService.save(lesson));
    }

    /** Deletes a lesson the viewer manages if it is not referenced by any roadmap. */
    @Override
    @Transactional
    public void deleteLesson(AppUser viewer, Long id) {
        permissionService.requirePermission(viewer, PermissionKeys.LESSONS_MANAGE);
        Lesson lesson = requireManageable(viewer, id);
        List<RoadmapLesson> usages = roadmapEntityService.findByIdLessonId(id);
        if (!usages.isEmpty()) {
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
        lessonEntityService.delete(lesson);
    }

    /** Creates a cover-image or file asset for the given lesson. */
    @Override
    @Transactional
    public LessonAssetResultRecord createAsset(AppUser viewer, Long lessonId, CreateLessonAssetInput input) {
        return lessonAssetService.createAsset(viewer, lessonId, input);
    }

    /** Removes an asset from a lesson. */
    @Override
    @Transactional
    public LessonAssetDeleteResultRecord deleteAsset(AppUser viewer, Long lessonId, Long assetId) {
        return lessonAssetService.deleteAsset(viewer, lessonId, assetId);
    }

    /**
     * Loads the lesson and verifies the viewer can manage it.
     *
     * @throws AppException C001/C004 if not found or not manageable
     */
    Lesson requireManageable(AppUser viewer, Long id) {
        Lesson lesson = lessonEntityService.getReference(id);
        if (!permissionService.canManageExistingLesson(viewer, lesson.getCreatedByUser() == null
            ? null : lesson.getCreatedByUser().getId())) {
            throw new AppException(ErrorReason.C004);
        }
        return lesson;
    }

    /**
     * Ensures the lesson has a non-blank title and body before save/publish.
     *
     * @throws AppException C002 when title or content is missing
     */
    void requirePublishableContent(Lesson lesson) {
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

    String firstNonBlank(String first, String second) {
        String left = stringVal(first).trim();
        if (!left.isEmpty()) {
            return left;
        }
        return stringVal(second).trim();
    }

    String stripHtmlToText(String value) {
        return stringVal(value)
            .replaceAll("(?is)<[^>]+>", " ")
            .replace("&nbsp;", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }

    /**
     * Returns true if the viewer is allowed to see the lesson.
     *
     * @param viewer authenticated viewer
     * @param lesson lesson entity to check
     * @return {@code true} when the lesson is visible to the viewer
     */
    boolean canView(AppUser viewer, Lesson lesson) {
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
     */
    void requireLearnerAccess(AppUser viewer, Lesson lesson) {
        if (viewer.isAdmin()) {
            return;
        }
        if (permissionService.userHasPermission(viewer, PermissionKeys.LESSONS_MANAGE)) {
            return;
        }
        lessonActivityAccessPolicy.requireEnrollment(viewer, lesson.getId());
    }

    /**
     * Loads the publication-status entity for the given code.
     *
     * @param code publication status code
     * @return publication status entity
     */
    LessonPublicationStatus publication(String code) {
        return lessonEntityService.findPublicationStatus(code);
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
