package com.aidigital.aionboarding.service.learning.services.impl;

import com.aidigital.aionboarding.domain.common.dictionary.UserRoleCode;
import com.aidigital.aionboarding.domain.learning.entities.UserLesson;
import com.aidigital.aionboarding.domain.learning.entities.UserRoadmap;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.roadmap.entities.Roadmap;
import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapTeamAssignment;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.learning.models.CompletedRoadmapRecord;
import com.aidigital.aionboarding.service.learning.models.LearningAssigneeRecord;
import com.aidigital.aionboarding.service.learning.models.LessonAssignmentEnrollmentRecord;
import com.aidigital.aionboarding.service.learning.models.LessonAssignmentResultRecord;
import com.aidigital.aionboarding.service.learning.models.LessonEnrollmentResultRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapAssignmentEnrollmentRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapAssignmentResultRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapEnrollmentResultRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapTeamAssignmentRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapTeamAssignmentResultRecord;
import com.aidigital.aionboarding.service.learning.services.LearningEnrollmentService;
import com.aidigital.aionboarding.service.learning.services.LearningService;
import com.aidigital.aionboarding.service.learning.services.RoadmapEnrollmentSyncService;
import com.aidigital.aionboarding.service.learning.services.entity.LearningEnrollmentEntityService;
import com.aidigital.aionboarding.service.learning.support.LearningActivityCompletionPolicy;
import com.aidigital.aionboarding.service.learning.support.LearningEnrollmentSupport;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonEntityService;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import com.aidigital.aionboarding.service.roadmap.services.entity.RoadmapEntityService;
import com.aidigital.aionboarding.service.roadmap.services.entity.RoadmapTeamAssignmentEntityService;
import com.aidigital.aionboarding.service.team.services.TeamService;
import com.aidigital.aionboarding.service.team.services.entity.TeamEntityService;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import com.aidigital.aionboarding.service.user.services.entity.UserEntityService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LearningServiceImpl implements LearningService {

    private final LessonEntityService lessonEntityService;
    private final LearningEnrollmentEntityService learningEnrollmentEntityService;
    private final PermissionService permissionService;
    private final TeamService teamService;
    private final TeamEntityService teamEntityService;
    private final RoadmapEntityService roadmapEntityService;
    private final RoadmapTeamAssignmentEntityService roadmapTeamAssignmentEntityService;
    private final UserEntityService userEntityService;
    private final LearningEnrollmentService learningEnrollmentService;
    private final RoadmapEnrollmentSyncService roadmapEnrollmentSyncService;
    private final LearningEnrollmentSupport learningEnrollmentSupport;
    private final LearningActivityCompletionPolicy learningActivityCompletionPolicy;
    private final CurrentTime currentTime;

    @Override
    @Transactional
    public LessonAssignmentResultRecord assignLesson(AppUser actor, Long lessonId, List<Long> userIds) {
        permissionService.requirePermission(actor, PermissionKeys.LEARNING_ASSIGN);
        List<Long> targetUserIds = learningEnrollmentSupport.normalizeUserIds(userIds);
        if (targetUserIds.isEmpty()) {
            throw new AppException(ErrorReason.C002, "Select at least one team member.");
        }
        requireAssignableTargets(actor, targetUserIds, "You can assign lessons only to manageable users.");

        Lesson lesson = learningEnrollmentService.requireEnrollableLesson(lessonId);
        LocalDateTime enrolledAt = currentTime.utcDateTime();
        List<UserLesson> enrollmentRows =
            learningEnrollmentService.enrollUsersInLesson(targetUserIds, lesson, enrolledAt, false);
        List<LessonAssignmentEnrollmentRecord> enrollments = new ArrayList<>();
        for (int i = 0; i < targetUserIds.size(); i++) {
            enrollments.add(learningEnrollmentSupport.toLessonAssignmentEnrollment(enrollmentRows.get(i), targetUserIds.get(i)));
        }

        return new LessonAssignmentResultRecord(true, enrollments);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LearningAssigneeRecord> listLessonAssignees(AppUser actor, Long lessonId) {
        permissionService.requirePermission(actor, PermissionKeys.LEARNING_ASSIGN);
        learningEnrollmentService.requireEnrollableLesson(lessonId);
        return learningEnrollmentEntityService.findByLessonIdWithUser(lessonId).stream()
            .map(enrollment -> new LearningAssigneeRecord(
                enrollment.getId().getUserId(),
                enrollment.getUser().getName(),
                enrollment.getUser().getEmail(),
                enrollment.getEnrolledAt(),
                enrollment.getCompletedAt() != null
            ))
            .toList();
    }

    @Override
    @Transactional
    public void revokeLessonAssignments(AppUser actor, Long lessonId, List<Long> userIds) {
        permissionService.requirePermission(actor, PermissionKeys.LEARNING_ASSIGN);
        List<Long> targetUserIds = learningEnrollmentSupport.normalizeUserIds(userIds);
        if (targetUserIds.isEmpty()) {
            throw new AppException(ErrorReason.C002, "Select at least one team member.");
        }
        requireAssignableTargets(actor, targetUserIds, "You can revoke lesson assignments only for manageable users.");
        learningEnrollmentService.requireEnrollableLesson(lessonId);
        learningEnrollmentService.unenrollUsersFromLesson(targetUserIds, lessonId);
    }

    @Override
    @Transactional
    public LessonEnrollmentResultRecord enrollLesson(AppUser user, Long lessonId) {
        permissionService.requirePermission(user, PermissionKeys.LEARNING_ENROLL);
        Lesson lesson = learningEnrollmentService.requireEnrollableLesson(lessonId);
        UserLesson enrollment = learningEnrollmentService.enrollUserInLesson(
            user.internalId(),
            lesson,
            currentTime.utcDateTime(),
            false
        );

        return new LessonEnrollmentResultRecord(
            true,
            learningEnrollmentSupport.toLessonEnrollment(enrollment),
            List.of()
        );
    }

    @Override
    @Transactional
    public void unenrollLesson(AppUser user, Long lessonId) {
        permissionService.requirePermission(user, PermissionKeys.LEARNING_ENROLL);
        learningEnrollmentService.unenrollUserFromLesson(user.internalId(), lessonId);
    }

    @Override
    @Transactional
    public LessonEnrollmentResultRecord setLessonCompletion(AppUser user, Long lessonId, boolean completed) {
        permissionService.requirePermission(user, PermissionKeys.LEARNING_COMPLETE);
        Lesson lesson = lessonEntityService.getReference(lessonId);
        if (!learningEnrollmentService.isEnrollable(lesson)) {
            throw new AppException(ErrorReason.C001, lessonId);
        }

        UserLesson.UserLessonId id = learningEnrollmentSupport.userLessonId(user.internalId(), lessonId);
        UserLesson enrollment = learningEnrollmentEntityService.findUserLessonById(id)
            .orElseThrow(() -> new AppException(ErrorReason.C001, "Lesson is not in My Lessons."));

        if (completed) {
            learningActivityCompletionPolicy.ensureAllActivitiesPassed(user.internalId(), lessonId);
            enrollment.setCompletedAt(currentTime.utcDateTime());
        } else {
            enrollment.setCompletedAt(null);
        }
        learningEnrollmentEntityService.save(enrollment);

        List<CompletedRoadmapRecord> completedRoadmaps = enrollment.getCompletedAt() != null
            ? roadmapEnrollmentSyncService.getCompletedRoadmapsForUserLesson(user.internalId(), lessonId)
            : List.of();
        return new LessonEnrollmentResultRecord(
            true,
            learningEnrollmentSupport.toLessonEnrollment(enrollment),
            completedRoadmaps
        );
    }

    @Override
    @Transactional
    public RoadmapAssignmentResultRecord assignRoadmap(AppUser actor, Long roadmapId, List<Long> userIds) {
        permissionService.requirePermission(actor, PermissionKeys.LEARNING_ASSIGN);
        List<Long> targetUserIds = learningEnrollmentSupport.normalizeUserIds(userIds);
        if (targetUserIds.isEmpty()) {
            throw new AppException(ErrorReason.C002, "Select at least one team member.");
        }
        requireAssignableTargets(actor, targetUserIds, "You can assign roadmaps only to manageable users.");

        List<UserRoadmap> enrollmentRows = learningEnrollmentService.enrollUsersInRoadmap(targetUserIds, roadmapId);
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
        requireAssignableTargets(actor, targetUserIds, "You can revoke roadmap assignments only for manageable users.");
        roadmapEntityService.getReference(roadmapId);
        learningEnrollmentService.unenrollUsersFromRoadmap(targetUserIds, roadmapId);
    }

    @Override
    @Transactional
    public RoadmapEnrollmentResultRecord enrollRoadmap(AppUser user, Long roadmapId) {
        permissionService.requirePermission(user, PermissionKeys.LEARNING_ENROLL);
        UserRoadmap enrollment = learningEnrollmentService.enrollUserInRoadmap(user.internalId(), roadmapId);

        return new RoadmapEnrollmentResultRecord(true, learningEnrollmentSupport.toRoadmapEnrollment(enrollment));
    }

    @Override
    @Transactional
    public void unenrollRoadmap(AppUser user, Long roadmapId) {
        permissionService.requirePermission(user, PermissionKeys.LEARNING_ENROLL);
        learningEnrollmentService.unenrollUserFromRoadmap(user.internalId(), roadmapId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompletedRoadmapRecord> getCompletedRoadmapsForUserLesson(Long userId, Long lessonId) {
        return roadmapEnrollmentSyncService.getCompletedRoadmapsForUserLesson(userId, lessonId);
    }

    void requireAssignableTargets(AppUser actor, List<Long> targetUserIds, String forbiddenMessage) {
        Set<Long> assignableIds = new HashSet<>();
        for (UserRecord candidate : teamService.getAssignableLearningUsers(actor)) {
            assignableIds.add(candidate.id());
        }
        if (!assignableIds.containsAll(targetUserIds)) {
            throw new AppException(ErrorReason.C004, forbiddenMessage);
        }
    }

    @Override
    @Transactional
    public RoadmapTeamAssignmentResultRecord assignRoadmapToGroup(AppUser actor, Long roadmapId, Long leadUserId) {
        permissionService.requirePermission(actor, PermissionKeys.LEARNING_ASSIGN);
        requireManageableTeam(actor, leadUserId, "You can assign roadmaps only to your own team.");
        Roadmap roadmap = roadmapEntityService.getReference(roadmapId);

        RoadmapTeamAssignment assignment = roadmapTeamAssignmentEntityService
            .findByRoadmapIdAndLeadUserId(roadmapId, leadUserId)
            .orElseGet(() -> createRoadmapTeamAssignment(actor, roadmap, leadUserId));

        List<RoadmapAssignmentEnrollmentRecord> enrollments = syncGroupRoadmapEnrollment(leadUserId, roadmapId);
        return new RoadmapTeamAssignmentResultRecord(
            true,
            learningEnrollmentSupport.toRoadmapTeamAssignment(assignment),
            enrollments
        );
    }

    RoadmapTeamAssignment createRoadmapTeamAssignment(AppUser actor, Roadmap roadmap, Long leadUserId) {
        RoadmapTeamAssignment assignment = new RoadmapTeamAssignment();
        assignment.setRoadmap(roadmap);
        assignment.setLeadUser(userEntityService.getReference(leadUserId));
        assignment.setAssignedByUser(userEntityService.getReference(actor.internalId()));
        LocalDateTime now = currentTime.utcDateTime();
        assignment.setCreatedAt(now);
        assignment.setUpdatedAt(now);
        return roadmapTeamAssignmentEntityService.save(assignment);
    }

    List<RoadmapAssignmentEnrollmentRecord> syncGroupRoadmapEnrollment(Long leadUserId, Long roadmapId) {
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

    @Override
    @Transactional
    public void unassignRoadmapFromGroup(AppUser actor, Long roadmapId, Long leadUserId) {
        permissionService.requirePermission(actor, PermissionKeys.LEARNING_ASSIGN);
        requireManageableTeam(actor, leadUserId, "You can unassign roadmaps only from your own team.");
        roadmapTeamAssignmentEntityService.findByRoadmapIdAndLeadUserId(roadmapId, leadUserId)
            .ifPresent(assignment -> requireCanRevokeTeamAssignment(actor, assignment));
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
            learningEnrollmentService.enrollUsersInRoadmap(List.of(memberUserId), roadmapId);
        }
    }

    void requireManageableTeam(AppUser actor, Long leadUserId, String forbiddenMessage) {
        if (!permissionService.canManageTeam(actor, leadUserId)) {
            throw new AppException(ErrorReason.C004, forbiddenMessage);
        }
    }

    void requireCanRevokeTeamAssignment(AppUser actor, RoadmapTeamAssignment assignment) {
        User assignedByUser = assignment.getAssignedByUser();
        if (assignedByUser == null || assignedByUser.getId() == null || assignedByUser.getId().equals(actor.internalId())) {
            return;
        }
        if (roleRank(actor.roleCode()) <= roleRank(assignedByUser.getRole().getCode())) {
            throw new AppException(ErrorReason.C004, "You cannot revoke an assignment created by a higher role.");
        }
    }

    int roleRank(String roleCode) {
        return switch (roleCode) {
            case UserRoleCode.ADMIN -> 3;
            case UserRoleCode.TEAMLEAD -> 2;
            default -> 1;
        };
    }

}
