package com.aidigital.aionboarding.service.learning.services.impl;

import com.aidigital.aionboarding.domain.learning.entities.UserRoadmap;
import com.aidigital.aionboarding.domain.roadmap.entities.Roadmap;
import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapTeamAssignment;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.learning.models.LearningAssigneeRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapAssignmentEnrollmentRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapAssignmentResultRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapEnrollmentResultRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapTeamAssignmentRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapTeamAssignmentResultRecord;
import com.aidigital.aionboarding.service.learning.services.RoadmapAssignmentService;
import com.aidigital.aionboarding.service.learning.services.RoadmapEnrollmentService;
import com.aidigital.aionboarding.service.learning.services.entity.LearningEnrollmentEntityService;
import com.aidigital.aionboarding.service.learning.support.LearningAssignmentAccessPolicy;
import com.aidigital.aionboarding.service.learning.support.LearningEnrollmentSupport;
import com.aidigital.aionboarding.service.learning.support.RoadmapTeamAssignmentWorkflow;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import com.aidigital.aionboarding.service.roadmap.services.entity.RoadmapEntityService;
import com.aidigital.aionboarding.service.roadmap.services.entity.RoadmapTeamAssignmentEntityService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoadmapAssignmentServiceImpl implements RoadmapAssignmentService {

    private final PermissionService permissionService;
    private final LearningEnrollmentEntityService learningEnrollmentEntityService;
    private final RoadmapEnrollmentService roadmapEnrollmentService;
    private final LearningEnrollmentSupport learningEnrollmentSupport;
    private final RoadmapEntityService roadmapEntityService;
    private final RoadmapTeamAssignmentEntityService roadmapTeamAssignmentEntityService;
    private final LearningAssignmentAccessPolicy learningAssignmentAccessPolicy;
    private final RoadmapTeamAssignmentWorkflow roadmapTeamAssignmentWorkflow;

    @Override
    @Transactional
    public RoadmapAssignmentResultRecord assignRoadmap(AppUser actor, Long roadmapId, List<Long> userIds) {
        permissionService.requirePermission(actor, PermissionKeys.LEARNING_ASSIGN);
        List<Long> targetUserIds = learningEnrollmentSupport.normalizeUserIds(userIds);
        if (targetUserIds.isEmpty()) {
            throw new AppException(ErrorReason.C002, "Select at least one team member.");
        }
        learningAssignmentAccessPolicy.requireAssignableTargets(
            actor, targetUserIds, "You can assign roadmaps only to manageable users.");

        List<UserRoadmap> enrollmentRows = roadmapEnrollmentService.enrollUsersInRoadmap(targetUserIds, roadmapId);
        List<RoadmapAssignmentEnrollmentRecord> enrollments = new ArrayList<>();
        for (int i = 0; i < targetUserIds.size(); i++) {
            enrollments.add(learningEnrollmentSupport.toRoadmapAssignmentEnrollment(enrollmentRows.get(i), targetUserIds.get(i)));
        }

        return new RoadmapAssignmentResultRecord(true, enrollments);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LearningAssigneeRecord> listRoadmapAssignees(AppUser actor, Long roadmapId) {
        permissionService.requirePermission(actor, PermissionKeys.LEARNING_ASSIGN);
        roadmapEntityService.getReference(roadmapId);
        return learningEnrollmentEntityService.findByRoadmapIdWithUser(roadmapId).stream()
            .map(enrollment -> new LearningAssigneeRecord(
                enrollment.getId().getUserId(),
                enrollment.getUser().getName(),
                enrollment.getUser().getEmail(),
                enrollment.getEnrolledAt(),
                null
            ))
            .toList();
    }

    @Override
    @Transactional
    public void revokeRoadmapAssignments(AppUser actor, Long roadmapId, List<Long> userIds) {
        permissionService.requirePermission(actor, PermissionKeys.LEARNING_ASSIGN);
        List<Long> targetUserIds = learningEnrollmentSupport.normalizeUserIds(userIds);
        if (targetUserIds.isEmpty()) {
            throw new AppException(ErrorReason.C002, "Select at least one team member.");
        }
        learningAssignmentAccessPolicy.requireAssignableTargets(
            actor, targetUserIds, "You can revoke roadmap assignments only for manageable users.");
        roadmapEntityService.getReference(roadmapId);
        roadmapEnrollmentService.unenrollUsersFromRoadmap(targetUserIds, roadmapId);
    }

    @Override
    @Transactional
    public RoadmapEnrollmentResultRecord enrollRoadmap(AppUser user, Long roadmapId) {
        permissionService.requirePermission(user, PermissionKeys.LEARNING_ENROLL);
        UserRoadmap enrollment = roadmapEnrollmentService.enrollUserInRoadmap(user.internalId(), roadmapId);

        return new RoadmapEnrollmentResultRecord(true, learningEnrollmentSupport.toRoadmapEnrollment(enrollment));
    }

    @Override
    @Transactional
    public void unenrollRoadmap(AppUser user, Long roadmapId) {
        permissionService.requirePermission(user, PermissionKeys.LEARNING_ENROLL);
        roadmapEnrollmentService.unenrollUserFromRoadmap(user.internalId(), roadmapId);
    }

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
