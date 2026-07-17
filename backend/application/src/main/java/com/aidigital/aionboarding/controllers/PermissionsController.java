package com.aidigital.aionboarding.controllers;

import com.aidigital.aionboarding.api.v1.PermissionsApi;
import com.aidigital.aionboarding.api.v1.model.CurrentUserPermissionsResponseV1;
import com.aidigital.aionboarding.api.v1.model.PermissionSnapshotResponseV1;
import com.aidigital.aionboarding.api.v1.model.SetPermissionOverridesRequestV1;
import com.aidigital.aionboarding.api.v1.model.SetPermissionOverridesResponseV1;
import com.aidigital.aionboarding.domain.common.dictionary.UserRoleCode;
import com.aidigital.aionboarding.mappers.common.PermissionDefinitionApiMapper;
import com.aidigital.aionboarding.mappers.common.PermissionDefinitionRegistry;
import com.aidigital.aionboarding.mappers.permission.PermissionApiMapper;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import com.aidigital.aionboarding.service.permission.models.PermissionSnapshotRecord;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import com.aidigital.aionboarding.service.team.services.TeamService;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import com.aidigital.aionboarding.service.user.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PermissionsController implements PermissionsApi {

    private final CurrentUserSupport currentUser;
    private final PermissionService permissionService;
    private final UserService userService;
    private final TeamService teamService;
    private final PermissionApiMapper permissionApiMapper;
    private final PermissionDefinitionApiMapper permissionDefinitionApiMapper;
    private final PermissionDefinitionRegistry permissionDefinitionRegistry;

    /**
     * Returns the authenticated caller's own effective permissions only. Deliberately bypasses
     * {@link #resolveTargetUsers(AppUser)} — that resolves every user the caller may manage
     * (all workspace users for an admin, or a full team for a team lead), which made every
     * authenticated shell load pay a cost proportional to organization size just to gate the
     * sidebar. This endpoint's query/response cost is O(1) regardless of workspace size; use
     * {@link #getPermissionManagementSnapshot()} for the Admin/Team Permissions management view.
     *
     * @return the caller's own permission snapshot
     */
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<CurrentUserPermissionsResponseV1> getPermissionSnapshot() {
        AppUser viewer = currentUser.requireUser();
        UserRecord viewerRecord = currentViewerRecord(viewer);
        PermissionSnapshotRecord snapshot = permissionService.snapshotForUsers(List.of(viewerRecord)).get(viewer.internalId());
        return ResponseEntity.ok(permissionApiMapper.toCurrentUserPermissionsResponseV1(snapshot));
    }

    /**
     * Returns effective and override permissions for every user the caller may manage, for the
     * Admin/Team Permissions management screens. Cost scales with the caller's management scope
     * (whole workspace for an admin, one team for a team lead) — never called on app bootstrap.
     *
     * @return the management-scoped permission snapshot
     */
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<PermissionSnapshotResponseV1> getPermissionManagementSnapshot() {
        AppUser viewer = currentUser.requireUser();
        List<UserRecord> users = resolveTargetUsers(viewer);
        return ResponseEntity.ok(permissionApiMapper.toPermissionSnapshotResponseV1(
            users,
            permissionService.snapshotForUsers(users),
            permissionDefinitionApiMapper,
            permissionDefinitionRegistry
        ));
    }

    @Override
    @Transactional
    public ResponseEntity<SetPermissionOverridesResponseV1> setPermissionOverrides(
        SetPermissionOverridesRequestV1 request
    ) {
        AppUser viewer = currentUser.requireUser();
        UserRecord target = userService.findById(request.getUserId())
            .orElseThrow(() -> new AppException(ErrorReason.C001, request.getUserId()));
        validateCanManageTarget(viewer, target);
        requireManagementPermission(viewer, target);
        permissionService.setOverrides(viewer, request.getUserId(), request.getOverrides());
        return ResponseEntity.ok(permissionApiMapper.toSetPermissionOverridesResponseV1(
            permissionService.snapshotForUsers(List.of(target)).get(target.id())
        ));
    }

    List<UserRecord> resolveTargetUsers(AppUser viewer) {
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

    UserRecord currentViewerRecord(AppUser viewer) {
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

    void validateCanManageTarget(AppUser viewer, UserRecord target) {
        if (viewer.isAdmin()) {
            if (UserRoleCode.ADMIN.equals(target.roleCode())) {
                throw new AppException(ErrorReason.C004);
            }
            return;
        }
        if (viewer.isTeamLead()
            && UserRoleCode.MEMBER.equals(target.roleCode())
            && permissionService.isTeamLeadForMember(viewer.internalId(), target.id())) {
            return;
        }
        throw new AppException(ErrorReason.C004);
    }

    void requireManagementPermission(AppUser viewer, UserRecord target) {
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
