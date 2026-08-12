package com.aidigital.aionboarding.service.learning.support;

import com.aidigital.aionboarding.domain.learning.entities.UserRoadmap;
import com.aidigital.aionboarding.domain.roadmap.entities.Roadmap;
import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapTeamAssignment;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.learning.models.RoadmapAssignmentEnrollmentRecord;
import com.aidigital.aionboarding.service.learning.services.LearningEnrollmentService;
import com.aidigital.aionboarding.service.roadmap.services.entity.RoadmapTeamAssignmentEntityService;
import com.aidigital.aionboarding.service.team.services.entity.TeamEntityService;
import com.aidigital.aionboarding.service.user.services.entity.UserEntityService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Creates and keeps in sync a roadmap's standing assignment to a team, enrolling the team's
 * current members.
 */
@Component
@RequiredArgsConstructor
public class RoadmapTeamAssignmentWorkflow {

    private final UserEntityService userEntityService;
    private final TeamEntityService teamEntityService;
    private final RoadmapTeamAssignmentEntityService roadmapTeamAssignmentEntityService;
    private final LearningEnrollmentService learningEnrollmentService;
    private final LearningEnrollmentSupport learningEnrollmentSupport;
    private final CurrentTime currentTime;

    /**
     * Creates and persists a new standing roadmap-team assignment record.
     *
     * @param actor      user performing the assignment
     * @param roadmap    roadmap being assigned
     * @param leadUserId team lead identifying the target team
     * @return the persisted assignment
     */
    public RoadmapTeamAssignment createRoadmapTeamAssignment(AppUser actor, Roadmap roadmap, Long leadUserId) {
        RoadmapTeamAssignment assignment = new RoadmapTeamAssignment();
        assignment.setRoadmap(roadmap);
        assignment.setLeadUser(userEntityService.getReference(leadUserId));
        assignment.setAssignedByUser(userEntityService.getReference(actor.internalId()));
        LocalDateTime now = currentTime.utcDateTime();
        assignment.setCreatedAt(now);
        assignment.setUpdatedAt(now);
        return roadmapTeamAssignmentEntityService.save(assignment);
    }

    /**
     * Enrolls the team lead and every current team member into the roadmap, re-syncing any
     * members added since the last call without creating duplicate enrollment.
     *
     * @param leadUserId team lead identifying the target team
     * @param roadmapId  roadmap identifier
     * @return enrollment outcome for the lead and each enrolled member
     */
    public List<RoadmapAssignmentEnrollmentRecord> syncGroupRoadmapEnrollment(Long leadUserId, Long roadmapId) {
        List<Long> targetUserIds = new ArrayList<>();
        targetUserIds.add(leadUserId);
        teamEntityService.findByLeadUserIdWithMember(leadUserId).stream()
            .map(member -> member.getId().getMemberUserId())
            .filter(memberUserId -> !targetUserIds.contains(memberUserId))
            .forEach(targetUserIds::add);
        if (targetUserIds.isEmpty()) {
            return List.of();
        }
        List<UserRoadmap> enrollmentRows = learningEnrollmentService.enrollUsersInRoadmap(targetUserIds, roadmapId);
        List<RoadmapAssignmentEnrollmentRecord> enrollments = new ArrayList<>();
        for (int i = 0; i < targetUserIds.size(); i++) {
            enrollments.add(learningEnrollmentSupport.toRoadmapAssignmentEnrollment(enrollmentRows.get(i), targetUserIds.get(i)));
        }
        return enrollments;
    }
}
