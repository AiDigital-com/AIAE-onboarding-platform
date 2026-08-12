package com.aidigital.aionboarding.service.lesson.services.impl;

import com.aidigital.aionboarding.domain.common.dictionary.LessonPublicationStatusCode;
import com.aidigital.aionboarding.domain.common.dictionary.LessonStatusCode;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonPublicationStatus;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
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
import com.aidigital.aionboarding.service.lesson.support.LessonMutationSupport;
import com.aidigital.aionboarding.service.lesson.support.LessonRecordAssembler;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LessonServiceImpl implements LessonService {

    private final CurrentTime currentTime;
    private final PermissionService permissionService;
    private final LessonRecordAssembler lessonMapper;
    private final LessonAssetService lessonAssetService;
    private final LessonInitialGenerationService lessonInitialGenerationService;
    private final LessonEntityService lessonEntityService;
    private final LessonDetailEnricher lessonDetailEnricher;
    private final LessonMutationSupport lessonMutationSupport;

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
        if (!lessonMutationSupport.canView(viewer, lesson)) {
            throw new AppException(ErrorReason.C001, id);
        }
        lessonMutationSupport.requireLearnerAccess(viewer, lesson);

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
        if (!lessonMutationSupport.canView(viewer, lesson)) {
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
        Lesson lesson = lessonMutationSupport.requireManageable(viewer, id);
        lessonMutationSupport.applyContentUpdate(viewer, lesson, input);
        lessonMutationSupport.requirePublishableContent(lesson);
        lessonEntityService.clearFailureIfPresent(lesson);
        lesson.setUpdatedAt(currentTime.utcDateTime());
        return lessonMapper.toDetailRecord(lessonEntityService.save(lesson));
    }

    /** Publishes, archives, or restores a lesson after checking LESSONS_PUBLISH_ARCHIVE permission. */
    @Override
    @Transactional
    public LessonDetailRecord changeLessonStatus(AppUser viewer, Long id, LessonStatusAction action) {
        permissionService.requirePermission(viewer, PermissionKeys.LESSONS_PUBLISH_ARCHIVE);
        Lesson lesson = lessonMutationSupport.requireManageable(viewer, id);
        switch (action) {
            case PUBLISH -> {
                if (!LessonStatusCode.READY.equals(lesson.getStatus().getCode())) {
                    throw new AppException(ErrorReason.C002, "Lesson must be ready before publishing.");
                }
                lessonMutationSupport.requirePublishableContent(lesson);
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
        Lesson lesson = lessonMutationSupport.requireManageable(viewer, id);
        lessonMutationSupport.requireNoRoadmapUsage(id);
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
     * Loads the publication-status entity for the given code.
     *
     * @param code publication status code
     * @return publication status entity
     */
    LessonPublicationStatus publication(String code) {
        return lessonEntityService.findPublicationStatus(code);
    }
}
