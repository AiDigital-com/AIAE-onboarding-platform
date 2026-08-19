package com.aidigital.aionboarding.service.learning.services.impl;

import com.aidigital.aionboarding.domain.roadmap.entities.Roadmap;
import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapTeamAssignment;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.learning.models.RoadmapAssignmentEnrollmentRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapTeamAssignmentRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapTeamAssignmentResultRecord;
import com.aidigital.aionboarding.service.learning.services.RoadmapEnrollmentService;
import com.aidigital.aionboarding.service.learning.services.RoadmapTeamAssignmentService;
import com.aidigital.aionboarding.service.learning.support.LearningAssignmentAccessPolicy;
import com.aidigital.aionboarding.service.learning.support.LearningEnrollmentSupport;
import com.aidigital.aionboarding.service.learning.support.RoadmapTeamAssignmentWorkflow;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import com.aidigital.aionboarding.service.roadmap.services.entity.RoadmapEntityService;
import com.aidigital.aionboarding.service.roadmap.services.entity.RoadmapTeamAssignmentEntityService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Extracted out of {@code RoadmapAssignmentServiceImpl} (see that class's history) because
 * standing team assignment is a distinct responsibility from individual assignment/self-enrollment,
 * with its own collaborator set ({@link RoadmapTeamAssignmentEntityService},
 * {@link RoadmapTeamAssignmentWorkflow}) — splitting it out follows
 * {@code .claude/rules/10-architecture.md}'s "extract validator/policy/helper collaborators
 * instead of adding another private-method cluster" and keeps both classes under the
 * service-contract-quality injected-field limit.
 */
@Service
@RequiredArgsConstructor
public class RoadmapTeamAssignmentServiceImpl implements RoadmapTeamAssignmentService {

    private final PermissionService permissionService;
    private final LearningAssignmentAccessPolicy learningAssignmentAccessPolicy;
    private final RoadmapEntityService roadmapEntityService;
    private final RoadmapTeamAssignmentEntityService roadmapTeamAssignmentEntityService;
    private final RoadmapTeamAssignmentWorkflow roadmapTeamAssignmentWorkflow;
    private final LearningEnrollmentSupport learningEnrollmentSupport;
    private final RoadmapEnrollmentService roadmapEnrollmentService;

    @Override
    @Transactional
    public RoadmapTeamAssignmentResultRecord assignRoadmapToGroup(AppUser actor, Long roadmapId, Long leadUserId) {
        permissionService.requirePermission(actor, PermissionKeys.LEARNING_ASSIGN);
        learningAssignmentAccessPolicy.requireManageableTeam(actor, leadUserId, "You can assign roadmaps only to your own team.");
        Roadmap roadmap = roadmapEntityService.getReference(roadmapId);

        RoadmapTeamAssignment assignment = roadmapTeamAssignmentEntityService
            .findByRoadmapIdAndLeadUserId(roadmapId, leadUserId)
            .orElseGet(() -> roadmapTeamAssignmentWorkflow.createRoadmapTeamAssignment(actor, roadmap, leadUserId));

        List<RoadmapAssignmentEnrollmentRecord> enrollments =
            roadmapTeamAssignmentWorkflow.syncGroupRoadmapEnrollment(leadUserId, roadmapId);
        return new RoadmapTeamAssignmentResultRecord(
            true,
            learningEnrollmentSupport.toRoadmapTeamAssignment(assignment),
            enrollments
        );
    }

    @Override
    @Transactional
    public void unassignRoadmapFromGroup(AppUser actor, Long roadmapId, Long leadUserId) {
        permissionService.requirePermission(actor, PermissionKeys.LEARNING_ASSIGN);
        learningAssignmentAccessPolicy.requireManageableTeam(
            actor, leadUserId, "You can unassign roadmaps only from your own team.");
        roadmapTeamAssignmentEntityService.findByRoadmapIdAndLeadUserId(roadmapId, leadUserId)
            .ifPresent(assignment -> learningAssignmentAccessPolicy.requireCanRevokeTeamAssignment(actor, assignment));
        roadmapTeamAssignmentEntityService.deleteByRoadmapIdAndLeadUserId(roadmapId, leadUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoadmapTeamAssignmentRecord> getRoadmapTeamAssignments(AppUser viewer, Long roadmapId) {
        permissionService.requirePermission(viewer, PermissionKeys.LEARNING_ASSIGN);
        return roadmapTeamAssignmentEntityService.findByRoadmapId(roadmapId).stream()
            .filter(assignment -> permissionService.canManageTeam(viewer, assignment.getLeadUser().getId()))
            .map(learningEnrollmentSupport::toRoadmapTeamAssignment)
            .toList();
    }

    @Override
    @Transactional
    public void syncNewTeamMemberEnrollments(Long leadUserId, Long memberUserId) {
        List<Long> roadmapIds = roadmapTeamAssignmentEntityService.findByLeadUserId(leadUserId).stream()
            .map(assignment -> assignment.getRoadmap().getId())
            .toList();
        for (Long roadmapId : roadmapIds) {
            roadmapEnrollmentService.enrollUsersInRoadmap(List.of(memberUserId), roadmapId);
        }
    }
}
