package com.aidigital.aionboarding.controllers;

import com.aidigital.aionboarding.api.v1.AdminApi;
import com.aidigital.aionboarding.api.v1.model.AdminUserStatsResponseV1;
import com.aidigital.aionboarding.api.v1.model.AdminUsersListResponseV1;
import com.aidigital.aionboarding.api.v1.model.AssignUserRoleRequestV1;
import com.aidigital.aionboarding.api.v1.model.TeamLeadAdminViewV1;
import com.aidigital.aionboarding.api.v1.model.TeamLeadEmailRequestV1;
import com.aidigital.aionboarding.api.v1.model.UserProfileV1;
import com.aidigital.aionboarding.api.v1.model.UserRoleCodeV1;
import com.aidigital.aionboarding.mappers.team.TeamApiMapper;
import com.aidigital.aionboarding.mappers.user.UserApiMapper;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import com.aidigital.aionboarding.service.team.services.TeamService;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import com.aidigital.aionboarding.service.user.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminController implements AdminApi {

	private final CurrentUserSupport currentUser;
	private final TeamService teamService;
	private final UserService userService;
	private final TeamApiMapper teamApiMapper;
	private final UserApiMapper userApiMapper;

	@Override
	@PreAuthorize("@perm.has('" + PermissionKeys.ADMIN_MANAGE_ROLES + "')")
	@Transactional(readOnly = true)
	public ResponseEntity<TeamLeadAdminViewV1> getTeamLeadAdminView() {
		AppUser viewer = currentUser.requireUser();
		return ResponseEntity.ok(teamApiMapper.toTeamLeadAdminViewV1(
				teamService.getTeams(),
				teamService.getTeamCandidateUsers(viewer)
		));
	}

	@Override
	@PreAuthorize("@perm.has('" + PermissionKeys.ADMIN_MANAGE_ROLES + "')")
	@Transactional
	public ResponseEntity<UserProfileV1> promoteTeamLead(TeamLeadEmailRequestV1 request) {
		UserRecord user = teamService.promoteTeamLeadByEmail(request.getEmail())
				.orElseThrow(() -> new AppException(ErrorReason.C001, request.getEmail()));
		return ResponseEntity.ok(userApiMapper.toUserProfileV1(user));
	}

	@Override
	@PreAuthorize("@perm.has('" + PermissionKeys.ADMIN_MANAGE_ROLES + "')")
	@Transactional
	public ResponseEntity<UserProfileV1> demoteTeamLead(TeamLeadEmailRequestV1 request) {
		UserRecord user = teamService.demoteTeamLeadByEmail(request.getEmail())
				.orElseThrow(() -> new AppException(ErrorReason.C001, request.getEmail()));
		return ResponseEntity.ok(userApiMapper.toUserProfileV1(user));
	}

	@Override
	@PreAuthorize("@perm.has('" + PermissionKeys.ADMIN_MANAGE_ROLES + "')")
	@Transactional(readOnly = true)
	public ResponseEntity<AdminUsersListResponseV1> listAdminUsers(String search, UserRoleCodeV1 role, Integer page,
																   Integer size) {
		int pageIndex = page == null ? 0 : page;
		int pageSize = size == null ? 20 : size;
		String roleCode = role == null ? null : role.getValue();
		return ResponseEntity.ok(
				userApiMapper.toAdminUsersListResponseV1(userService.listUsers(roleCode, search, pageIndex, pageSize))
		);
	}

	@Override
	@PreAuthorize("@perm.has('" + PermissionKeys.ADMIN_MANAGE_ROLES + "')")
	@Transactional(readOnly = true)
	public ResponseEntity<AdminUserStatsResponseV1> getAdminUserStats() {
		return ResponseEntity.ok(userApiMapper.toAdminUserStatsResponseV1(userService.getAdminUserStats()));
	}

	@Override
	@PreAuthorize("@perm.has('" + PermissionKeys.ADMIN_MANAGE_ROLES + "')")
	@Transactional
	public ResponseEntity<UserProfileV1> assignUserRole(Long id, AssignUserRoleRequestV1 request) {
		AppUser viewer = currentUser.requireUser();
		UserRecord updated = userService.assignRole(viewer, id, request.getRoleCode().getValue());
		return ResponseEntity.ok(userApiMapper.toUserProfileV1(updated));
	}
}
