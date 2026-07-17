package com.aidigital.aionboarding.service.lessonactivity.support;

import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.domain.common.dictionary.ActivityProgressStatusCode;
import com.aidigital.aionboarding.domain.learning.entities.UserLesson;
import com.aidigital.aionboarding.domain.lessonactivity.entities.LessonActivity;
import com.aidigital.aionboarding.domain.lessonactivity.entities.UserLessonActivityAttempt;
import com.aidigital.aionboarding.domain.lessonactivity.entities.UserLessonActivityProgress;
import com.aidigital.aionboarding.domain.lessonactivity.repositories.UserLessonActivityAttemptRepository;
import com.aidigital.aionboarding.domain.lessonactivity.repositories.UserLessonActivityProgressRepository;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.service.learning.services.entity.LearningEnrollmentEntityService;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonEntityService;
import com.aidigital.aionboarding.service.user.services.entity.UserEntityService;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class LessonActivityProgressPersistence {

    private final UserLessonActivityProgressRepository progressRepository;
    private final UserLessonActivityAttemptRepository attemptRepository;
    private final LearningEnrollmentEntityService learningEnrollmentEntityService;
    private final UserEntityService userEntityService;
    private final LessonEntityService lessonEntityService;
    private final LessonActivityAccessPolicy accessPolicy;
    private final CurrentTime currentTime;

    public UserLessonActivityProgressRepository progressRepository() {
        return progressRepository;
    }

    public UserLessonActivityAttemptRepository attemptRepository() {
        return attemptRepository;
    }

    /**
     * Loads a single user's lesson enrollment for the given lesson.
     *
     * @param userId   the learner's user id
     * @param lessonId the owning lesson's primary key
     * @return the matching enrollment row, if one exists
     */
    public Optional<UserLesson> findUserLesson(Long userId, Long lessonId) {
        return learningEnrollmentEntityService.findUserLessonByUserIdAndLessonId(userId, lessonId);
    }

    /**
     * Persists changes to a lesson enrollment row.
     *
     * @param userLesson the lesson enrollment to persist
     * @return the saved {@link UserLesson}
     */
    public UserLesson saveUserLesson(UserLesson userLesson) {
        return learningEnrollmentEntityService.save(userLesson);
    }

    /**
     * Loads a proxy reference to a user, for attaching to a new attempt/progress row
     * without an extra round trip.
     *
     * @param userId the user primary key
     * @return a {@link User} proxy for the given ID
     */
    public User getUserReference(Long userId) {
        return userEntityService.getReference(userId);
    }

    /**
     * Loads a single user's progress on a single activity.
     *
     * @param userId     the learner's user id
     * @param activityId the activity's primary key
     * @return the matching progress row, if one exists
     */
    @Transactional(readOnly = true)
    public Optional<UserLessonActivityProgress> findByUserIdAndActivityId(Long userId, Long activityId) {
        return progressRepository.findByUserIdAndActivityId(userId, activityId);
    }

    /**
     * Loads a single user's progress across every activity in a lesson.
     *
     * @param userId   the learner's user id
     * @param lessonId the owning lesson's primary key
     * @return every progress row for the given user and lesson
     */
    @Transactional(readOnly = true)
    public List<UserLessonActivityProgress> findByUserIdAndLessonId(Long userId, Long lessonId) {
        return progressRepository.findByUserIdAndLessonId(userId, lessonId);
    }

    /**
     * Loads one learner's progress across several lessons.
     *
     * @param userId    learner identifier
     * @param lessonIds lesson identifiers
     * @return matching progress rows
     */
    @Transactional(readOnly = true)
    public List<UserLessonActivityProgress> findByUserIdAndLessonIds(Long userId, Collection<Long> lessonIds) {
        if (lessonIds == null || lessonIds.isEmpty()) {
            return List.of();
        }
        return progressRepository.findByUserIdAndLessonIds(userId, lessonIds);
    }

    public UserLessonActivityProgress loadOrCreateProgress(Long userId, LessonActivity activity, Long lessonId) {
        return progressRepository.findByUserIdAndActivityId(userId, activity.getId())
            .orElseGet(() -> {
                UserLessonActivityProgress progress = new UserLessonActivityProgress();
                UserLessonActivityProgress.UserLessonActivityProgressId id =
                    new UserLessonActivityProgress.UserLessonActivityProgressId();
                id.setUserId(userId);
                id.setActivityId(activity.getId());
                progress.setId(id);
                progress.setUser(userEntityService.getReference(userId));
                progress.setActivity(activity);
                progress.setLesson(lessonEntityService.getReference(lessonId));
                progress.setStatus(accessPolicy.progressStatus(ActivityProgressStatusCode.NOT_STARTED));
                progress.setMetadata(new HashMap<>());
                progress.setStartedAt(currentTime.utcDateTime());
                progress.setUpdatedAt(currentTime.utcDateTime());
                return progress;
            });
    }

    /**
     * Assigns the next attempt number and inserts the attempt row in its own, independent
     * transaction. Two concurrent quiz submissions for the same (user, activity) can both read
     * the same {@code MAX(attemptNumber) + 1} before either commits; the table's unique
     * constraint on (user, activity, attemptNumber) then rejects the loser's insert. Running the
     * insert in {@link Propagation#REQUIRES_NEW} means that failure aborts only this short
     * transaction rather than poisoning the caller's whole completeQuiz transaction (Postgres
     * aborts the entire enclosing transaction on a constraint violation), so the caller can catch
     * the failure and simply call this method again to retry with a freshly recomputed number.
     *
     * @param attemptEntity fully-populated attempt row, missing only its attempt number
     * @return the persisted attempt, with a guaranteed-unique attempt number
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UserLessonActivityAttempt insertAttemptWithNextNumber(UserLessonActivityAttempt attemptEntity) {
        attemptEntity.setAttemptNumber(
            attemptRepository.nextAttemptNumber(attemptEntity.getUser().getId(), attemptEntity.getActivity().getId()));
        return attemptRepository.save(attemptEntity);
    }

    public void clearLessonCompletion(Long userId, Long lessonId) {
        learningEnrollmentEntityService.findUserLessonByUserIdAndLessonId(userId, lessonId).ifPresent(enrollment -> {
            enrollment.setCompletedAt(null);
            learningEnrollmentEntityService.save(enrollment);
        });
    }

}
