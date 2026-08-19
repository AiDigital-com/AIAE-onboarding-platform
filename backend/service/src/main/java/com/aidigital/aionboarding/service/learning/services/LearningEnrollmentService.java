package com.aidigital.aionboarding.service.learning.services;

import com.aidigital.aionboarding.domain.learning.entities.UserLesson;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.learning.models.LessonEnrollmentRecord;
import com.aidigital.aionboarding.service.learning.models.MyLessonSummaryRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Creates and updates lesson enrollment rows for learners. Roadmap enrollment and its
 * per-lesson fan-out live in {@link RoadmapEnrollmentService}.
 */
public interface LearningEnrollmentService {

    /**
     * Returns a bounded page of the current user's enrolled published lessons as lean
     * summaries — never the full lesson body, materials, assets, or generation metadata —
     * with enrollment status and activity counts, incomplete-first then newest-enrolled-first.
     *
     * @param viewer   authenticated user
     * @param pageable page and size request
     * @return the user's lesson summary page
     */
    Page<MyLessonSummaryRecord> getMyLessons(AppUser viewer, Pageable pageable);

    /**
     * Returns the current user's enrollment for a lesson.
     *
     * @param viewer authenticated user
     * @param lessonId lesson identifier
     * @return enrollment record
     * @throws com.aidigital.aionboarding.service.common.error.AppException when the lesson or enrollment
     *     is missing or the lesson is not visible
     */
    LessonEnrollmentRecord getLessonEnrollment(AppUser viewer, Long lessonId);

    /**
     * Returns the current user's enrollment when present without treating absence as an error.
     *
     * @param viewer authenticated user
     * @param lessonId lesson identifier
     * @return optional enrollment record
     * @throws com.aidigital.aionboarding.service.common.error.AppException when the lesson is missing
     */
    Optional<LessonEnrollmentRecord> findLessonEnrollment(AppUser viewer, Long lessonId);

    /**
     * Loads a lesson that is ready and published (Public) for self-service enrollment. Use this
     * only for the learner-initiated self-enroll path; a Team Lead/Admin assigning, listing, or
     * revoking assignees, a roadmap's lesson fan-out, and completion must use
     * {@link #requireLearnableLesson(Long)} instead so private (assigned-only) lessons remain
     * usable on those paths.
     *
     * @param lessonId lesson identifier
     * @return self-enrollable lesson entity
     * @throws com.aidigital.aionboarding.service.common.error.AppException when the lesson is missing or not self-enrollable
     */
    Lesson requireSelfEnrollableLesson(Long lessonId);

    /**
     * Loads a lesson that is ready and either published (Public) or private (assigned-only), for
     * assignment, revoke, assignee listing, roadmap fan-out, and completion.
     *
     * @param lessonId lesson identifier
     * @return learnable lesson entity
     * @throws com.aidigital.aionboarding.service.common.error.AppException when the lesson is missing or not learnable
     */
    Lesson requireLearnableLesson(Long lessonId);

    /**
     * Checks whether a lesson is ready and published (Public) — the Library self-service
     * enrollment path only. A private lesson is never self-enrollable, even though it remains
     * assignable; see {@link #isLearnable(Lesson)}.
     *
     * @param lesson lesson entity
     * @return {@code true} when the lesson can be self-enrolled
     */
    boolean isSelfEnrollable(Lesson lesson);

    /**
     * Checks whether a lesson is ready and either published (Public) or private (assigned-only).
     * Used by assignment, revoke, assignee listing, roadmap fan-out, and completion, so a Team
     * Lead/Admin can still assign and a learner can still complete a private lesson.
     *
     * @param lesson lesson entity
     * @return {@code true} when the lesson is learnable
     */
    boolean isLearnable(Lesson lesson);

    /**
     * Enrolls a user in a lesson, optionally updating an existing enrollment timestamp.
     *
     * @param userId learner identifier
     * @param lesson lesson entity
     * @param enrolledAt enrollment timestamp
     * @param updateExisting whether to refresh an existing enrollment row
     * @return persisted enrollment row
     */
    UserLesson enrollUserInLesson(Long userId, Lesson lesson, LocalDateTime enrolledAt, boolean updateExisting);

    /**
     * Enrolls several users in one lesson using one lookup for existing enrollments and one
     * batch save for new or refreshed rows.
     *
     * @param userIds learners to enroll
     * @param lesson lesson entity
     * @param enrolledAt enrollment timestamp
     * @param updateExisting whether to refresh existing enrollment rows
     * @return enrollment rows ordered like {@code userIds}
     */
    List<UserLesson> enrollUsersInLesson(
        Collection<Long> userIds,
        Lesson lesson,
        LocalDateTime enrolledAt,
        boolean updateExisting
    );

    /**
     * Removes a user's lesson enrollment when present.
     *
     * @param userId learner identifier
     * @param lessonId lesson identifier
     */
    void unenrollUserFromLesson(Long userId, Long lessonId);

    /**
     * Removes several users' lesson enrollment using one set-based bulk delete, regardless of
     * user count.
     *
     * @param userIds  learners to revoke
     * @param lessonId lesson identifier
     */
    void unenrollUsersFromLesson(Collection<Long> userIds, Long lessonId);

    /**
     * Returns the set of lesson IDs, among the given lesson IDs, that the user is enrolled in.
     * Bounded by {@code lessonIds} (e.g. one results page) rather than the user's total
     * lifetime enrollment count.
     *
     * @param userId    learner identifier
     * @param lessonIds lesson IDs to restrict to
     * @return set of enrolled lesson IDs among {@code lessonIds} (empty if none)
     */
    Set<Long> getEnrolledLessonIds(Long userId, Collection<Long> lessonIds);
}
