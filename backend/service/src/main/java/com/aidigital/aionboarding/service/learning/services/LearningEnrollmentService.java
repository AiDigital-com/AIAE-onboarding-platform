package com.aidigital.aionboarding.service.learning.services;

import com.aidigital.aionboarding.domain.learning.entities.UserLesson;
import com.aidigital.aionboarding.domain.learning.entities.UserRoadmap;
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
 * Creates and updates lesson and roadmap enrollment rows for learners.
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
     * Loads a lesson that is ready and published for enrollment.
     *
     * @param lessonId lesson identifier
     * @return enrollable lesson entity
     * @throws com.aidigital.aionboarding.service.common.error.AppException when the lesson is missing or not enrollable
     */
    Lesson requireEnrollableLesson(Long lessonId);

    /**
     * Checks whether a lesson is ready and published.
     *
     * @param lesson lesson entity
     * @return {@code true} when the lesson can be enrolled
     */
    boolean isEnrollable(Lesson lesson);

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
     * Enrolls a user in a roadmap and fans out roadmap lesson enrollments.
     *
     * @param userId learner identifier
     * @param roadmapId roadmap identifier
     * @return persisted roadmap enrollment row
     * @throws com.aidigital.aionboarding.service.common.error.AppException when the roadmap is missing
     */
    UserRoadmap enrollUserInRoadmap(Long userId, Long roadmapId);

    /**
     * Enrolls several users in one roadmap and fans out its lessons using batch enrollment
     * lookups and saves.
     *
     * @param userIds learners to enroll
     * @param roadmapId roadmap identifier
     * @return roadmap enrollment rows ordered like {@code userIds}
     */
    List<UserRoadmap> enrollUsersInRoadmap(Collection<Long> userIds, Long roadmapId);

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
     * Removes a user's roadmap enrollment when present.
     *
     * @param userId learner identifier
     * @param roadmapId roadmap identifier
     */
    void unenrollUserFromRoadmap(Long userId, Long roadmapId);

    /**
     * Removes several users' roadmap enrollments and their roadmap-derived lesson enrollments
     * using set-based bulk deletes, regardless of user or roadmap-lesson count.
     *
     * @param userIds learners to revoke
     * @param roadmapId roadmap identifier
     */
    void unenrollUsersFromRoadmap(Collection<Long> userIds, Long roadmapId);

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
