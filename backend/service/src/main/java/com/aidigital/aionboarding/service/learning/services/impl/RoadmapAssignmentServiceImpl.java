package com.aidigital.aionboarding.service.learning.services.impl;

import com.aidigital.aionboarding.domain.learning.entities.UserRoadmap;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.learning.models.LearningAssigneeRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapAssignmentEnrollmentRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapAssignmentResultRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapEnrollmentResultRecord;
import com.aidigital.aionboarding.service.learning.services.RoadmapAssignmentService;
import com.aidigital.aionboarding.service.learning.services.RoadmapEnrollmentService;
import com.aidigital.aionboarding.service.learning.services.entity.LearningEnrollmentEntityService;
import com.aidigital.aionboarding.service.learning.support.LearningAssignmentAccessPolicy;
import com.aidigital.aionboarding.service.learning.support.LearningEnrollmentSupport;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import com.aidigital.aionboarding.service.roadmap.services.entity.RoadmapEntityService;
import com.aidigital.aionboarding.service.roadmap.support.RoadmapAccessPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
    private final LearningAssignmentAccessPolicy learningAssignmentAccessPolicy;
    private final RoadmapAccessPolicy roadmapAccessPolicy;

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

    /**
     * Lists the assignees for a roadmap, restricted to the subset of enrollees the actor may
     * actually manage (every user but themselves for an admin, their own team's members for a
     * team lead). The roadmap's own visibility/authorship plays no part here — this mirrors
     * {@link #assignRoadmap} and {@link #revokeRoadmapAssignments}, which already bound the
     * assignment itself to manageable targets — because otherwise a {@code learning.assign}
     * holder with no connection to a given roadmap could read the names, emails, and enrollment
     * dates of every one of its enrollees, including users well outside their own team.
     */
    @Override
    @Transactional(readOnly = true)
    public List<LearningAssigneeRecord> listRoadmapAssignees(AppUser actor, Long roadmapId) {
        permissionService.requirePermission(actor, PermissionKeys.LEARNING_ASSIGN);
        roadmapEntityService.getReference(roadmapId);
        Set<Long> assignableUserIds = learningAssignmentAccessPolicy.assignableUserIds(actor);
        return learningEnrollmentEntityService.findByRoadmapIdWithUser(roadmapId).stream()
            .filter(enrollment -> assignableUserIds.contains(enrollment.getId().getUserId()))
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

    /**
     * Self-enrolls the caller in a roadmap already visible to them — either they already hold an
     * enrollment (idempotent no-op) or they may manage it. The {@code LEARNING_ENROLL} permission
     * alone is not sufficient: it is held by every Member by default, and without the
     * {@link RoadmapAccessPolicy#requireSelfEnrollable} gate a plain Member could self-grant an
     * arbitrary roadmap (and, since Phase C, every private lesson fanned out from it) by simply
     * posting an id they were never assigned or shown.
     */
    @Override
    @Transactional
    public RoadmapEnrollmentResultRecord enrollRoadmap(AppUser user, Long roadmapId) {
        permissionService.requirePermission(user, PermissionKeys.LEARNING_ENROLL);
        roadmapAccessPolicy.requireSelfEnrollable(user, roadmapId);
        UserRoadmap enrollment = roadmapEnrollmentService.enrollUserInRoadmap(user.internalId(), roadmapId);

        return new RoadmapEnrollmentResultRecord(true, learningEnrollmentSupport.toRoadmapEnrollment(enrollment));
    }

    @Override
    @Transactional
    public void unenrollRoadmap(AppUser user, Long roadmapId) {
        permissionService.requirePermission(user, PermissionKeys.LEARNING_ENROLL);
        roadmapEnrollmentService.unenrollUserFromRoadmap(user.internalId(), roadmapId);
    }
}
