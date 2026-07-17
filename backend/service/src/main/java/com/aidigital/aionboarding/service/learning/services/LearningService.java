package com.aidigital.aionboarding.service.learning.services;

import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.learning.models.CompletedRoadmapRecord;
import com.aidigital.aionboarding.service.learning.models.LearningAssigneeRecord;
import com.aidigital.aionboarding.service.learning.models.LessonAssignmentResultRecord;
import com.aidigital.aionboarding.service.learning.models.LessonEnrollmentResultRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapAssignmentResultRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapEnrollmentResultRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapTeamAssignmentRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapTeamAssignmentResultRecord;

import java.util.List;

/**
 * Orchestrates lesson and roadmap enrollment, assignment, completion, and unenrollment for learners.
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

    /**
     * Assigns a roadmap to one or more manageable team members.
     *
     * @param actor user performing the assignment
     * @param roadmapId roadmap identifier
     * @param userIds target learner identifiers
     * @return assignment outcome with enrollment details per user
     */
    RoadmapAssignmentResultRecord assignRoadmap(AppUser actor, Long roadmapId, List<Long> userIds);

    /**
     * Lists learners currently enrolled in a roadmap (for assignment management).
     *
     * @param actor user listing assignments
     * @param roadmapId roadmap identifier
     * @return enrolled assignees newest first
     */
    List<LearningAssigneeRecord> listRoadmapAssignees(AppUser actor, Long roadmapId);

    /**
     * Revokes one or more learners' roadmap enrollment on behalf of an assigner, in one bulk
     * request.
     *
     * @param actor user performing the revoke
     * @param roadmapId roadmap identifier
     * @param userIds learners to remove
     */
    void revokeRoadmapAssignments(AppUser actor, Long roadmapId, List<Long> userIds);

    /**
     * Enrolls the authenticated learner in a roadmap and its enrollable lessons.
     *
     * @param user authenticated learner
     * @param roadmapId roadmap identifier
     * @return enrollment outcome for the learner
     */
    RoadmapEnrollmentResultRecord enrollRoadmap(AppUser user, Long roadmapId);

    /**
     * Removes the authenticated learner from a roadmap enrollment.
     *
     * @param user authenticated learner
     * @param roadmapId roadmap identifier
     */
    void unenrollRoadmap(AppUser user, Long roadmapId);

    /**
     * Returns roadmaps the user has fully completed that include the given lesson.
     *
     * @param userId learner identifier
     * @param lessonId lesson identifier
     * @return completed roadmaps sorted by title
     */
    List<CompletedRoadmapRecord> getCompletedRoadmapsForUserLesson(Long userId, Long lessonId);

    /**
     * Assigns a roadmap to a team, identified by its lead's user id, and enrolls every current
     * team member. Calling this again for the same roadmap and team re-syncs enrollment for any
     * members added since the last call without creating a duplicate assignment record.
     *
     * @param actor user performing the assignment
     * @param roadmapId roadmap identifier
     * @param leadUserId team lead identifier that identifies the target team
     * @return the standing assignment record and enrollment details for each enrolled member
     */
    RoadmapTeamAssignmentResultRecord assignRoadmapToGroup(AppUser actor, Long roadmapId, Long leadUserId);

    /**
     * Removes a roadmap's standing assignment to a team. Existing member enrollments and their
     * progress are left untouched; only future automatic enrollment for that team is stopped.
     *
     * @param actor user performing the unassignment
     * @param roadmapId roadmap identifier
     * @param leadUserId team lead identifier that identifies the target team
     */
    void unassignRoadmapFromGroup(AppUser actor, Long roadmapId, Long leadUserId);

    /**
     * Lists a roadmap's standing team assignments visible to the viewer (all teams for an admin,
     * only the viewer's own team otherwise).
     *
     * @param viewer authenticated viewer
     * @param roadmapId roadmap identifier
     * @return visible team assignments for the roadmap
     */
    List<RoadmapTeamAssignmentRecord> getRoadmapTeamAssignments(AppUser viewer, Long roadmapId);

    /**
     * Enrolls a newly added team member into every roadmap already standing-assigned to that team.
     *
     * @param leadUserId team lead identifier that identifies the team the member joined
     * @param memberUserId newly added member identifier
     */
    void syncNewTeamMemberEnrollments(Long leadUserId, Long memberUserId);
}
