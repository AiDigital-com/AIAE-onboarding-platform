package com.aidigital.aionboarding.service.learning.services;

import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.learning.models.LearningAssigneeRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapAssignmentResultRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapEnrollmentResultRecord;

import java.util.List;

/**
 * Orchestrates roadmap enrollment and individual assignment for learners. Standing team
 * assignment (assigning a roadmap to a whole team and keeping new members enrolled) lives in
 * {@link RoadmapTeamAssignmentService} — a separate responsibility with its own collaborators.
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
}
