package com.aidigital.aionboarding.service.learning.services;

import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.learning.models.LearningAssigneeRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapAssignmentResultRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapEnrollmentResultRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapTeamAssignmentRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapTeamAssignmentResultRecord;

import java.util.List;

/**
 * Orchestrates roadmap enrollment, individual assignment, and standing team assignment for
 * learners.
 */
public interface RoadmapAssignmentService {

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
