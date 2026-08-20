package com.aidigital.aionboarding.service.learning.services;

import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.learning.models.RoadmapTeamAssignmentRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapTeamAssignmentResultRecord;

import java.util.List;

/**
 * Orchestrates a roadmap's standing assignment to a team, identified by its lead's user id:
 * creating/reusing the assignment, removing it, listing it, and keeping newly added team members
 * enrolled. Extracted out of {@link RoadmapAssignmentService}, which orchestrates individual
 * roadmap assignment and self-enrollment instead — a separate responsibility with its own set of
 * collaborators.
 */
public interface RoadmapTeamAssignmentService {

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
