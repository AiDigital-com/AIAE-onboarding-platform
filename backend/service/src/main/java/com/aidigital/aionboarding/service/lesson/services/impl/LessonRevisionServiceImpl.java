package com.aidigital.aionboarding.service.lesson.services.impl;

import com.aidigital.aionboarding.domain.common.dictionary.LessonStatusCode;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.lesson.models.LessonDetailRecord;
import com.aidigital.aionboarding.service.lesson.models.ReviseLessonInput;
import com.aidigital.aionboarding.service.lesson.models.RevisionBriefRecord;
import com.aidigital.aionboarding.service.lesson.models.RevisionHistoryEntryRecord;
import com.aidigital.aionboarding.service.lesson.models.RevisionProviderMetadataRecord;
import com.aidigital.aionboarding.service.lesson.models.RevisionResultRecord;
import com.aidigital.aionboarding.service.lesson.prompt.LessonRevisionPromptBuilder;
import com.aidigital.aionboarding.service.lesson.services.LessonRevisionService;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonEntityService;
import com.aidigital.aionboarding.service.lesson.support.LessonRecordAssembler;
import com.aidigital.aionboarding.service.lesson.support.LessonRevisionMetadataMapper;
import com.aidigital.aionboarding.service.lesson.support.LessonRevisionRequestValidator;
import com.aidigital.aionboarding.service.lessongen.model.GeneratedContentResult;
import com.aidigital.aionboarding.service.lessongen.model.GeneratedRevisionBriefResult;
import com.aidigital.aionboarding.service.lessongen.model.LessonGenPrompt;
import com.aidigital.aionboarding.service.lessongen.services.LessonGenService;
import com.aidigital.aionboarding.service.material.models.PreparedMaterialsResult;
import com.aidigital.aionboarding.service.material.services.MaterialPreparationService;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Orchestrates the lesson revision flow: loads the lesson, prepares materials,
 * invokes the planner and writer AI steps, and persists the result.
 * <p>
 * No class-level {@code @Transactional} is declared here. Each persistence step
 * delegates to {@link LessonEntityService} which opens its own short transaction,
 * preventing a database connection from being held across AI network calls.
 */
@Service
@RequiredArgsConstructor
public class LessonRevisionServiceImpl implements LessonRevisionService {

    private final CurrentTime currentTime;
    private final LessonEntityService lessonEntityService;
    private final PermissionService permissionService;
    private final MaterialPreparationService materialPreparationService;
    private final LessonRecordAssembler lessonMapper;
    private final LessonRevisionPromptBuilder lessonRevisionPromptBuilder;
    private final LessonGenService lessonGenService;
    private final LessonRevisionRequestValidator lessonRevisionRequestValidator;
    private final LessonRevisionMetadataMapper lessonRevisionMetadataMapper;

    /**
     * Revises an existing READY lesson using a two-step AI process (planner then writer)
     * and persists the updated content.
     * <p>
     * The lesson is loaded via {@link LessonEntityService#findByIdWithFetches(Long)} (not
     * {@code getReference}) because this method has no ambient transaction (see class header),
     * so {@code status}/{@code publicationStatus}/{@code createdByUser} must already be eagerly
     * initialised via JOIN FETCH before the session that loaded them closes.
     *
     * @param viewer   the authenticated user performing the revision
     * @param lessonId the ID of the lesson to revise
     * @param request  the revision parameters
     * @return the updated lesson detail and the revision brief produced by the planner
     * @throws AppException C002 if the lesson is not in READY status
     */
    @Override
    public RevisionResultRecord reviseLesson(AppUser viewer, Long lessonId, ReviseLessonInput request) {
        permissionService.requirePermission(viewer, PermissionKeys.LESSONS_MANAGE);
        Lesson lesson = lessonEntityService.findByIdWithFetches(lessonId);
        if (!LessonStatusCode.READY.equals(lesson.getStatus().getCode())) {
            throw new AppException(ErrorReason.C002, "Only READY lessons can be revised.");
        }
        LessonRevisionRequestValidator.ValidatedRevisionRequest validated =
            lessonRevisionRequestValidator.validate(viewer, lesson, request);

        LessonDetailRecord lessonRecord = lessonMapper.toDetailRecord(lesson);
        PreparedMaterialsResult preparedMaterials = materialPreparationService.prepareForLesson(lessonId);

        var plannerPrompt = lessonRevisionPromptBuilder.buildLessonRevisionPlannerPrompt(
            lessonRecord, preparedMaterials, validated.revisionRequest(), validated.selectedOptions());
        LessonGenPrompt plannerGenPrompt = new LessonGenPrompt(
            plannerPrompt.version(), plannerPrompt.cacheKey(),
            plannerPrompt.instructions(), plannerPrompt.input());
        GeneratedRevisionBriefResult plannerGenResult =
            lessonGenService.generateLessonRevisionBrief(plannerGenPrompt);

        Map<String, Object> briefMap = plannerGenResult.brief();
        RevisionBriefRecord revisionBrief = lessonRevisionMetadataMapper.buildRevisionBrief(briefMap);

        var writerPrompt = lessonRevisionPromptBuilder.buildLessonRevisionWriterPrompt(
            lessonRecord, preparedMaterials, validated.revisionRequest(), validated.selectedOptions(), revisionBrief);
        LessonGenPrompt writerGenPrompt = new LessonGenPrompt(
            writerPrompt.version(), writerPrompt.cacheKey(),
            writerPrompt.instructions(), writerPrompt.input());
        GeneratedContentResult writerGenResult =
            lessonGenService.generateLessonContent(writerGenPrompt);

        RevisionProviderMetadataRecord plannerProviderMetadata =
            lessonRevisionMetadataMapper.buildProviderMetadata(plannerGenResult.metadata());
        RevisionProviderMetadataRecord writerProviderMetadata =
            lessonRevisionMetadataMapper.buildProviderMetadata(writerGenResult.metadata());

		RevisionHistoryEntryRecord revisionEntry = new RevisionHistoryEntryRecord(
				currentTime.utcDateTime().toString(),
				validated.revisionRequest(),
				validated.selectedOptions(),
				revisionBrief,
				plannerPrompt,
				writerPrompt,
				plannerProviderMetadata,
				writerProviderMetadata
		);

        Lesson saved = saveRevisionWithRetry(lesson, lessonId, writerGenResult.content(), revisionEntry);

        return new RevisionResultRecord(lessonMapper.toDetailRecord(saved), revisionBrief);
    }

    /**
     * Applies the revised content and saves it, retrying once against a freshly reloaded lesson
     * if a concurrent editor's change already advanced this row's optimistic-locking version.
     * The two AI calls that produced {@code revisedContent} and {@code revisionEntry} are
     * expensive, so a benign single race should not discard their output; a second conflict is
     * left to propagate as a client-visible 409 rather than risk silently overwriting a third
     * concurrent change.
     *
     * @param lesson         the lesson loaded at the start of this revision, before either AI call
     * @param lessonId       lesson primary key, used to reload fresh state on conflict
     * @param revisedContent the writer AI's revised HTML/markdown content
     * @param revisionEntry  the revision history entry to merge in
     * @return the saved lesson
     * @throws ObjectOptimisticLockingFailureException if the retry attempt also conflicts
     */
    Lesson saveRevisionWithRetry(
        Lesson lesson, Long lessonId, String revisedContent, RevisionHistoryEntryRecord revisionEntry
    ) {
        try {
            return applyRevisionAndSave(lesson, revisedContent, revisionEntry);
        } catch (ObjectOptimisticLockingFailureException conflict) {
            Lesson freshLesson = lessonEntityService.findByIdWithFetches(lessonId);
            return applyRevisionAndSave(freshLesson, revisedContent, revisionEntry);
        }
    }

    /**
     * Applies the revised content to the given lesson instance, merges the revision history
     * entry into its generation metadata, and saves it.
     *
     * @param lesson         the lesson instance to update
     * @param revisedContent the writer AI's revised HTML/markdown content
     * @param revisionEntry  the revision history entry to merge in
     * @return the saved lesson
     */
    Lesson applyRevisionAndSave(Lesson lesson, String revisedContent, RevisionHistoryEntryRecord revisionEntry) {
        lessonRevisionMetadataMapper.applyRevisedContent(lesson, revisedContent);
        Map<String, Object> updatedMetadata = lessonRevisionMetadataMapper.mergeRevisionEntry(lesson, revisionEntry);
        return lessonEntityService.saveRevised(lesson, updatedMetadata);
    }
}
