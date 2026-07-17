package com.aidigital.aionboarding.service.lessonactivity.support;

import com.aidigital.aionboarding.domain.common.dictionary.ActivityTypeCode;
import com.aidigital.aionboarding.domain.common.dictionary.LessonPublicationStatusCode;
import com.aidigital.aionboarding.domain.common.dictionary.LessonStatusCode;
import com.aidigital.aionboarding.domain.common.dictionary.entities.ActivityProgressStatus;
import com.aidigital.aionboarding.domain.common.dictionary.entities.ActivityType;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.service.common.dictionary.DictionaryLookupService;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.learning.services.entity.LearningEnrollmentEntityService;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonEntityService;
import com.aidigital.aionboarding.service.lessonactivity.models.LessonActivityRecord;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LessonActivityAccessPolicy {

    private final LessonEntityService lessonEntityService;
    private final LearningEnrollmentEntityService learningEnrollmentEntityService;
    private final DictionaryLookupService dictionaryLookupService;
    private final PermissionService permissionService;
    private final LessonActivityPayloadAssembler payloadAssembler;

    /**
     * Loads the lesson eagerly initialising {@code status}/{@code publicationStatus}/
     * {@code createdByUser} (via {@link LessonEntityService#findByIdWithFetches(Long)}, not
     * {@code getReference}) since every check in this policy reads one of those associations,
     * and some callers (e.g. AI generation flows) have no ambient transaction to resolve a lazy
     * proxy later.
     *
     * @param lessonId the lesson primary key
     * @return the lesson with status/publicationStatus/createdByUser initialised
     * @throws AppException C001 if no lesson with the given ID exists
     */
    public Lesson requireLesson(Long lessonId) {
        return lessonEntityService.findByIdWithFetches(lessonId);
    }

    public Lesson requirePublishedReadyLesson(Long lessonId) {
        Lesson lesson = requireLesson(lessonId);
        if (!LessonStatusCode.READY.equals(lesson.getStatus().getCode())
            || !LessonPublicationStatusCode.PUBLISHED.equals(lesson.getPublicationStatus().getCode())) {
            throw new AppException(ErrorReason.C001, "Lesson not found.");
        }
        return lesson;
    }

    public void requireActivityManager(AppUser viewer, Lesson lesson) {
        Long createdByUserId = lesson.getCreatedByUser() == null ? null : lesson.getCreatedByUser().getId();
        if (!viewer.isAdmin() && !permissionService.canManageExistingLesson(viewer, createdByUserId)) {
            throw new AppException(ErrorReason.C004);
        }
    }

    public void requireEnrollment(AppUser viewer, Long lessonId) {
        if (learningEnrollmentEntityService.findUserLessonByUserIdAndLessonId(viewer.internalId(), lessonId).isEmpty()) {
            throw new AppException(ErrorReason.C001, "Lesson is not in My Lessons.");
        }
    }

    public ActivityType requireActivityType(String code) {
        return dictionaryLookupService.getActivityTypeReference(code);
    }

    public ActivityProgressStatus progressStatus(String code) {
        return dictionaryLookupService.getActivityProgressStatusReference(code);
    }

    /**
     * Checks whether the viewer may see a lesson's quiz correct-answer keys before submitting an
     * attempt. Only admins and the lesson's own creator see them upfront (for review/editing) —
     * a team lead who can manage lessons in general but did not author this one does not.
     * Everyone else only sees correctness feedback after submitting, via the separate graded
     * attempt result.
     *
     * @param viewer authenticated viewer
     * @param lessonCreatedByUserId the lesson's creator/author user id, or {@code null} if unset
     * @return {@code true} when the viewer may see raw correct-answer data for this lesson
     */
    public boolean canSeeQuizAnswersForLesson(AppUser viewer, Long lessonCreatedByUserId) {
        return permissionService.canManageExistingLesson(viewer, lessonCreatedByUserId);
    }

    /**
     * Strips correct-answer keys from a quiz activity's payload unless the viewer created this
     * specific lesson (or is an admin). Non-quiz activities are returned unchanged.
     *
     * @param activity activity record to redact
     * @param viewer authenticated viewer
     * @param lessonCreatedByUserId the lesson's creator/author user id, or {@code null} if unset
     * @return the same record when the viewer may see answers or the activity is not a quiz,
     *     otherwise a copy with correct-answer keys removed
     */
    public LessonActivityRecord redactQuizAnswersUnlessManager(
        LessonActivityRecord activity, AppUser viewer, Long lessonCreatedByUserId
    ) {
        if (canSeeQuizAnswersForLesson(viewer, lessonCreatedByUserId) || !ActivityTypeCode.QUIZ.equals(activity.type())) {
            return activity;
        }
        List<Map<String, Object>> items = payloadAssembler.asMapList(activity.payload().get("items"));
        if (items.isEmpty()) {
            return activity;
        }
        List<Map<String, Object>> redactedItems = items.stream()
            .map(item -> {
                Map<String, Object> redacted = new LinkedHashMap<>(item);
                redacted.remove("correctAnswer");
                redacted.remove("correctAnswers");
                return redacted;
            })
            .toList();
        Map<String, Object> redactedPayload = new LinkedHashMap<>(activity.payload());
        redactedPayload.put("items", redactedItems);
        return new LessonActivityRecord(
            activity.id(),
            activity.lessonId(),
            activity.type(),
            activity.title(),
            activity.itemCount(),
            redactedPayload,
            activity.generationMetadata(),
            activity.createdBy(),
            activity.createdAt(),
            activity.progress()
        );
    }

    /**
     * Applies {@link #redactQuizAnswersUnlessManager(LessonActivityRecord, AppUser, Long)} to
     * every activity in the list.
     *
     * @param activities activity records to redact
     * @param viewer authenticated viewer
     * @param lessonCreatedByUserId the lesson's creator/author user id, or {@code null} if unset
     * @return redacted activity records in the same order
     */
    public List<LessonActivityRecord> redactQuizAnswersUnlessManager(
        List<LessonActivityRecord> activities, AppUser viewer, Long lessonCreatedByUserId
    ) {
        return activities.stream()
            .map(activity -> redactQuizAnswersUnlessManager(activity, viewer, lessonCreatedByUserId))
            .toList();
    }
}
