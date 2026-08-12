package com.aidigital.aionboarding.service.permission.support;

import com.aidigital.aionboarding.domain.common.dictionary.UserRoleCode;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import com.aidigital.aionboarding.service.permission.services.TeamLeadershipService;
import com.aidigital.aionboarding.service.team.services.TeamService;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import com.aidigital.aionboarding.service.user.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves which users a viewer may manage for the Admin/Team Permissions screens, and enforces
 * who may change a target user's permission overrides. Extracted as a dedicated policy
 * collaborator, per {@code .claude/rules/10-architecture.md}, rather than left as controller-side
 * orchestration.
 */
@Component
@RequiredArgsConstructor
public class PermissionManagementPolicy {

    private final UserService userService;
    private final TeamService teamService;
    private final PermissionService permissionService;
    private final TeamLeadershipService teamLeadershipService;

    /**
     * Resolves every user the viewer may manage: the whole workspace for an admin, the viewer's
     * assignable team for a team lead with {@code permissions.manage_team_members}, or just the
     * viewer otherwise.
     *
     * @param viewer authenticated caller
     * @return distinct, viewer-first list of manageable users
     */
    public List<UserRecord> resolveTargetUsers(AppUser viewer) {
        Map<Long, UserRecord> usersById = new LinkedHashMap<>();
        usersById.put(viewer.internalId(), currentViewerRecord(viewer));

        if (viewer.isAdmin()) {
            userService.getAllUsers().forEach(user -> usersById.put(user.id(), user));
            return new ArrayList<>(usersById.values());
        }
        if (viewer.isTeamLead()
                && permissionService.userHasPermission(viewer, PermissionKeys.PERMISSIONS_MANAGE_TEAM_MEMBERS)) {
            teamService.getAssignableLearningUsers(viewer).forEach(user -> usersById.put(user.id(), user));
        }
        return new ArrayList<>(usersById.values());
    }

    /**
     * Loads the viewer's own persisted user record, falling back to a record built from claims
     * when none is persisted yet.
     *
     * @param viewer authenticated caller
     * @return the viewer's user record
     */
    public UserRecord currentViewerRecord(AppUser viewer) {
        return userService.findById(viewer.internalId()).orElseGet(() -> new UserRecord(
                viewer.internalId(),
                viewer.clerkUserId(),
                viewer.name(),
                viewer.email(),
                viewer.roleCode(),
                viewer.position(),
                viewer.avatarStorageKey(),
                viewer.avatarColor(),
                null,
                null,
                null
        ));
    }

    /**
     * Validates that the viewer may manage the target user's permissions at all, independent of
     * whether the specific permission being changed is allowed.
     *
     * @param viewer authenticated caller
     * @param target user whose overrides are being changed
     * @throws AppException with {@link ErrorReason#C004} when the viewer may not manage the target
     */
    public void validateCanManageTarget(AppUser viewer, UserRecord target) {
        if (viewer.isAdmin()) {
            if (UserRoleCode.ADMIN.equals(target.roleCode())) {
                throw new AppException(ErrorReason.C004);
            }
            return;
        }
        if (viewer.isTeamLead()
                && UserRoleCode.MEMBER.equals(target.roleCode())
                && teamLeadershipService.isTeamLeadForMember(viewer.internalId(), target.id())) {
            return;
        }
        throw new AppException(ErrorReason.C004);
    }

    /**
     * Asserts the viewer holds the specific management permission required for the target's
     * role, after {@link #validateCanManageTarget} has already confirmed the relationship.
     *
     * @param viewer authenticated caller
     * @param target user whose overrides are being changed
     * @throws AppException with {@link ErrorReason#C004} when the viewer lacks the permission
     */
    public void requireManagementPermission(AppUser viewer, UserRecord target) {
        if (viewer.isAdmin()) {
            permissionService.requirePermission(viewer, PermissionKeys.PERMISSIONS_MANAGE_TEAMLEADS);
            return;
        }
        if (viewer.isTeamLead() && UserRoleCode.MEMBER.equals(target.roleCode())) {
            permissionService.requirePermission(viewer, PermissionKeys.PERMISSIONS_MANAGE_TEAM_MEMBERS);
            return;
        }
        throw new AppException(ErrorReason.C004);
    }
}
