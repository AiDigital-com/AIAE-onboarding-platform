package com.aidigital.aionboarding.service.lessonactivity.support;

import com.aidigital.aionboarding.domain.learning.entities.UserLesson;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.learning.models.LessonEnrollmentRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.ActivityAttemptRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.ActivityCompletionResultRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.ActivityProgressRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.LessonActivityRecord;
import com.aidigital.aionboarding.service.lessonactivity.services.LessonActivityAssemblyService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Builds the response after an activity changes progress, and refreshes lesson completion state
 * as a side effect when every activity is now passed. Split out of
 * {@code LessonActivityProgressServiceImpl} to keep that class's line count within budget.
 */
@Component
@RequiredArgsConstructor
public class LessonActivityCompletionSupport {

    private final LessonActivityAccessPolicy accessPolicy;
    private final LessonActivityAssemblyService assemblyService;
    private final LessonActivityRecordAssembler lessonActivityMapper;
    private final LessonActivityProgressPersistence progressPersistence;
    private final CurrentTime currentTime;

    /**
     * Builds the response after an activity changes progress and refreshes lesson completion state.
     *
     * @param viewer   the authenticated user whose progress just changed
     * @param lesson   the lesson the activity belongs to
     * @param progress the updated progress record for the changed activity
     * @param attempt  the quiz attempt record, or {@code null} for non-quiz activities
     * @return the assembled completion result
     */
    public ActivityCompletionResultRecord buildCompletionResult(
            AppUser viewer,
            Lesson lesson,
            ActivityProgressRecord progress,
            ActivityAttemptRecord attempt
    ) {
        Long lessonId = lesson.getId();
        Long lessonCreatedByUserId = lesson.getCreatedByUser() == null ? null : lesson.getCreatedByUser().getId();
        List<LessonActivityRecord> activities = accessPolicy.redactQuizAnswersUnlessManager(
                assemblyService.getLessonActivitiesForUser(lessonId, viewer.internalId()),
                viewer,
                lessonCreatedByUserId
        );
        boolean lessonCompleted = !activities.isEmpty()
                && activities.stream().allMatch(lessonActivityMapper::isActivityPassed);
        LessonEnrollmentRecord enrollment = lessonCompleted
                ? setLessonCompletionForUser(viewer.internalId(), lessonId, true)
                : getLessonEnrollmentForUser(viewer.internalId(), lessonId);

        return new ActivityCompletionResultRecord(progress, activities, lessonCompleted, enrollment, attempt);
    }

    /**
     * Updates a user's lesson completion timestamp after verifying activity completion.
     *
     * @param userId      the learner whose enrollment is updated
     * @param lessonId    the lesson being marked complete/incomplete
     * @param isCompleted {@code true} to mark complete, {@code false} to clear completion
     * @return the updated enrollment record, or {@code null} when the user has no enrollment
     * @throws AppException C002 when marking complete while an activity has not been passed
     */
    public LessonEnrollmentRecord setLessonCompletionForUser(Long userId, Long lessonId, boolean isCompleted) {
        if (isCompleted) {
            List<LessonActivityRecord> activities = assemblyService.getLessonActivitiesForUser(lessonId, userId);
            if (!activities.isEmpty() && activities.stream().anyMatch(activity -> !lessonActivityMapper.isActivityPassed(activity))) {
                throw new AppException(
                        ErrorReason.C002,
                        "Complete all lesson activities before marking this lesson complete."
                );
            }
        }

        UserLesson enrollment = progressPersistence.findUserLesson(userId, lessonId)
                .orElse(null);
        if (enrollment == null) {
            return null;
        }
        enrollment.setCompletedAt(isCompleted ? currentTime.utcDateTime() : null);
        progressPersistence.saveUserLesson(enrollment);
        return getLessonEnrollmentForUser(userId, lessonId);
    }

    /**
     * Returns the current lesson enrollment record for the user when one exists.
     *
     * @param userId   the learner
     * @param lessonId the lesson
     * @return the enrollment record, or {@code null} when the user has no enrollment
     */
    public LessonEnrollmentRecord getLessonEnrollmentForUser(Long userId, Long lessonId) {
        return progressPersistence.findUserLesson(userId, lessonId)
                .map(lessonActivityMapper::toEnrollmentRecord)
                .orElse(null);
    }
}
