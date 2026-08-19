package com.aidigital.aionboarding.service.learning.services;

import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.learning.models.LearningAssigneeRecord;
import com.aidigital.aionboarding.service.learning.models.LessonAssignmentResultRecord;
import com.aidigital.aionboarding.service.learning.models.LessonEnrollmentResultRecord;

import java.util.List;

/**
 * Orchestrates lesson enrollment, assignment, and completion for learners. Roadmap enrollment
 * and individual assignment live in {@link RoadmapAssignmentService}; standing team assignment
 * lives in {@link RoadmapTeamAssignmentService}.
 */
public interface LearningService {

    /**
     * Assigns a published ready lesson to one or more manageable team members.
     *
     * @param actor user performing the assignment
     * @param lessonId lesson identifier
     * @param userIds target learner identifiers
     * @return assignment outcome with enrollment details per user
     */
    LessonAssignmentResultRecord assignLesson(AppUser actor, Long lessonId, List<Long> userIds);

    /**
     * Lists learners currently enrolled in a lesson (for assignment management).
     *
     * @param actor user listing assignments
     * @param lessonId lesson identifier
     * @return enrolled assignees newest first
     */
    List<LearningAssigneeRecord> listLessonAssignees(AppUser actor, Long lessonId);

    /**
     * Revokes one or more learners' lesson enrollment on behalf of an assigner, in one bulk
     * request.
     *
     * @param actor user performing the revoke
     * @param lessonId lesson identifier
     * @param userIds learners to remove
     */
    void revokeLessonAssignments(AppUser actor, Long lessonId, List<Long> userIds);

    /**
     * Enrolls the authenticated learner in a published ready lesson.
     *
     * @param user authenticated learner
     * @param lessonId lesson identifier
     * @return enrollment outcome for the learner
     */
    LessonEnrollmentResultRecord enrollLesson(AppUser user, Long lessonId);

    /**
     * Removes the authenticated learner from a lesson enrollment.
     *
     * @param user authenticated learner
     * @param lessonId lesson identifier
     */
    void unenrollLesson(AppUser user, Long lessonId);

    /**
     * Marks a lesson complete or incomplete for the authenticated learner.
     *
     * @param user authenticated learner
     * @param lessonId lesson identifier
     * @param completed {@code true} to mark complete, {@code false} to clear completion
     * @return updated enrollment and any roadmaps newly completed by this change
     */
    LessonEnrollmentResultRecord setLessonCompletion(AppUser user, Long lessonId, boolean completed);
}
