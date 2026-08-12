package com.aidigital.aionboarding.controllers;

import com.aidigital.aionboarding.api.v1.PermissionsApi;
import com.aidigital.aionboarding.api.v1.model.CurrentUserPermissionsResponseV1;
import com.aidigital.aionboarding.api.v1.model.PermissionSnapshotResponseV1;
import com.aidigital.aionboarding.api.v1.model.SetPermissionOverridesRequestV1;
import com.aidigital.aionboarding.api.v1.model.SetPermissionOverridesResponseV1;
import com.aidigital.aionboarding.mappers.common.PermissionDefinitionApiMapper;
import com.aidigital.aionboarding.mappers.common.PermissionDefinitionRegistry;
import com.aidigital.aionboarding.mappers.permission.PermissionApiMapper;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.permission.models.PermissionSnapshotRecord;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import com.aidigital.aionboarding.service.permission.support.PermissionManagementPolicy;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import com.aidigital.aionboarding.service.user.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PermissionsController implements PermissionsApi {

    private final CurrentUserSupport currentUser;
    private final PermissionService permissionService;
    private final UserService userService;
    private final PermissionManagementPolicy permissionManagementPolicy;
    private final PermissionApiMapper permissionApiMapper;
    private final PermissionDefinitionApiMapper permissionDefinitionApiMapper;
    private final PermissionDefinitionRegistry permissionDefinitionRegistry;

    /**
     * Returns the authenticated caller's own effective permissions only. Deliberately bypasses
     * {@link PermissionManagementPolicy#resolveTargetUsers(AppUser)} — that resolves every user
     * the caller may manage (all workspace users for an admin, or a full team for a team lead),
     * which made every authenticated shell load pay a cost proportional to organization size
     * just to gate the sidebar. This endpoint's query/response cost is O(1) regardless of
     * workspace size; use {@link #getPermissionManagementSnapshot()} for the Admin/Team
     * Permissions management view.
     *
     * @return the caller's own permission snapshot
     */
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<CurrentUserPermissionsResponseV1> getPermissionSnapshot() {
        AppUser viewer = currentUser.requireUser();
        UserRecord viewerRecord = permissionManagementPolicy.currentViewerRecord(viewer);
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
        List<UserRecord> users = permissionManagementPolicy.resolveTargetUsers(viewer);
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
        permissionManagementPolicy.validateCanManageTarget(viewer, target);
        permissionManagementPolicy.requireManagementPermission(viewer, target);
        permissionService.setOverrides(viewer, request.getUserId(), request.getOverrides());
        return ResponseEntity.ok(permissionApiMapper.toSetPermissionOverridesResponseV1(
            permissionService.snapshotForUsers(List.of(target)).get(target.id())
        ));
    }
}
