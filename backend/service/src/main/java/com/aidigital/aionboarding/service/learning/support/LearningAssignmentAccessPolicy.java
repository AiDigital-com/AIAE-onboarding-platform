package com.aidigital.aionboarding.service.learning.support;

import com.aidigital.aionboarding.domain.common.dictionary.UserRoleCode;
import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapTeamAssignment;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import com.aidigital.aionboarding.service.team.services.TeamService;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Enforces who an actor may target for lesson/roadmap assignment and team-roadmap
 * administration, shared by the lesson and roadmap assignment services.
 */
@Component
@RequiredArgsConstructor
public class LearningAssignmentAccessPolicy {

    private final TeamService teamService;
    private final PermissionService permissionService;

    /**
     * Asserts that every target user id is manageable (assignable) by the actor.
     *
     * @param actor             user performing the assignment/revoke
     * @param targetUserIds     normalized target learner identifiers
     * @param forbiddenMessage  message to raise when a target is not manageable
     * @throws AppException with reason {@code C004} when any target is not manageable
     */
    public void requireAssignableTargets(AppUser actor, List<Long> targetUserIds, String forbiddenMessage) {
        if (!assignableUserIds(actor).containsAll(targetUserIds)) {
            throw new AppException(ErrorReason.C004, forbiddenMessage);
        }
    }

    /**
     * Resolves the set of user ids the actor may assign, revoke, or otherwise see
     * learning-assignment data for: every user except themselves for an admin, their own team's
     * members for a team lead, and no one for anyone else. Used both to validate assignment
     * targets and to bound roster-style reads (e.g. listing a roadmap's or lesson's assignees) to
     * only the users the actor is actually permitted to manage.
     *
     * @param actor user whose assignable-learner set is being resolved
     * @return the assignable user ids for this actor
     */
    public Set<Long> assignableUserIds(AppUser actor) {
        Set<Long> assignableIds = new HashSet<>();
        for (UserRecord candidate : teamService.getAssignableLearningUsers(actor)) {
            assignableIds.add(candidate.id());
        }
        return assignableIds;
    }

    /**
     * Asserts that the actor may manage the team led by the given lead user.
     *
     * @param actor             user performing the team-roadmap action
     * @param leadUserId        team lead identifying the target team
     * @param forbiddenMessage  message to raise when the actor may not manage the team
     * @throws AppException with reason {@code C004} when the actor may not manage the team
     */
    public void requireManageableTeam(AppUser actor, Long leadUserId, String forbiddenMessage) {
        if (!permissionService.canManageTeam(actor, leadUserId)) {
            throw new AppException(ErrorReason.C004, forbiddenMessage);
        }
    }

    /**
     * Asserts that the actor may revoke a standing roadmap-team assignment, which is allowed for
     * the assignment's own creator or for any actor with a strictly higher role than the creator.
     *
     * @param actor      user performing the revoke
     * @param assignment standing team-roadmap assignment being revoked
     * @throws AppException with reason {@code C004} when the actor's role is not higher than the creator's
     */
    public void requireCanRevokeTeamAssignment(AppUser actor, RoadmapTeamAssignment assignment) {
        User assignedByUser = assignment.getAssignedByUser();
        if (assignedByUser == null || assignedByUser.getId() == null || assignedByUser.getId().equals(actor.internalId())) {
            return;
        }
        if (roleRank(actor.roleCode()) <= roleRank(assignedByUser.getRole().getCode())) {
            throw new AppException(ErrorReason.C004, "You cannot revoke an assignment created by a higher role.");
        }
    }

    /**
     * Ranks a role code for comparison purposes, higher meaning more privileged.
     *
     * @param roleCode role code to rank
     * @return relative rank, admin highest, team lead next, everyone else lowest
     */
    int roleRank(String roleCode) {
        return switch (roleCode) {
            case UserRoleCode.ADMIN -> 3;
            case UserRoleCode.TEAMLEAD -> 2;
            default -> 1;
        };
    }
}
