package com.aidigital.aionboarding.controllers;

import com.aidigital.aionboarding.api.v1.model.CurrentUserPermissionsResponseV1;
import com.aidigital.aionboarding.api.v1.model.PermissionSnapshotResponseV1;
import com.aidigital.aionboarding.domain.common.dictionary.UserRoleCode;
import com.aidigital.aionboarding.mappers.common.PermissionDefinitionApiMapper;
import com.aidigital.aionboarding.mappers.common.PermissionDefinitionRegistry;
import com.aidigital.aionboarding.mappers.permission.PermissionApiMapper;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import com.aidigital.aionboarding.service.permission.models.PermissionSnapshotRecord;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import com.aidigital.aionboarding.service.team.services.TeamService;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import com.aidigital.aionboarding.service.user.services.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionsControllerTest {

	@Mock
	private CurrentUserSupport currentUser;
	@Mock
	private PermissionService permissionService;
	@Mock
	private UserService userService;
	@Mock
	private TeamService teamService;
	@Mock
	private PermissionApiMapper permissionApiMapper;
	@Mock
	private PermissionDefinitionApiMapper permissionDefinitionApiMapper;
	@Mock
	private PermissionDefinitionRegistry permissionDefinitionRegistry;

	@InjectMocks
	private PermissionsController controller;

	@Test
	void getPermissionSnapshotDoesNotResolveManagedUsersTest() {
		// Given: an admin viewer, whose management scope would otherwise be every user in the workspace
		AppUser viewer = new AppUser(1L, "clerk-1", "admin@test.com", "Admin", UserRoleCode.ADMIN, "Admin", "", "",
				"");
		UserRecord viewerRecord = new UserRecord(1L, "clerk-1", "Admin", "admin@test.com", UserRoleCode.ADMIN, "", "",
				"", null, null, null);
		PermissionSnapshotRecord snapshot = new PermissionSnapshotRecord(UserRoleCode.ADMIN, Map.of("admin" +
				".manage_roles", true), Map.of());
		CurrentUserPermissionsResponseV1 expectedBody = mock(CurrentUserPermissionsResponseV1.class);

		when(currentUser.requireUser()).thenReturn(viewer);
		when(userService.findById(1L)).thenReturn(Optional.of(viewerRecord));
		when(permissionService.snapshotForUsers(List.of(viewerRecord))).thenReturn(Map.of(1L, snapshot));
		when(permissionApiMapper.toCurrentUserPermissionsResponseV1(snapshot)).thenReturn(expectedBody);

		// When:
		ResponseEntity<CurrentUserPermissionsResponseV1> response = controller.getPermissionSnapshot();

		// Then: bounded to a single-user snapshot lookup, never the org-wide/team-wide resolution
		assertThat(response.getBody()).isSameAs(expectedBody);
		verify(permissionService).snapshotForUsers(List.of(viewerRecord));
		verifyNoInteractions(teamService);
		verify(userService, never()).getAllUsers();
	}

	@Test
	void getPermissionManagementSnapshotResolvesAllUsersForAdminTest() {
		// Given: an admin viewer, whose management scope is every user in the workspace
		AppUser viewer = new AppUser(1L, "clerk-1", "admin@test.com", "Admin", UserRoleCode.ADMIN, "Admin", "", "",
				"");
		UserRecord viewerRecord = new UserRecord(1L, "clerk-1", "Admin", "admin@test.com", UserRoleCode.ADMIN, "", "",
				"", null, null, null);
		UserRecord otherUser = new UserRecord(2L, "clerk-2", "Member", "member@test.com", UserRoleCode.MEMBER, "", "",
				"", null, null, null);
		Map<Long, PermissionSnapshotRecord> snapshots = Map.of(
				1L, new PermissionSnapshotRecord(UserRoleCode.ADMIN, Map.of(), Map.of()),
				2L, new PermissionSnapshotRecord(UserRoleCode.MEMBER, Map.of(), Map.of())
		);
		PermissionSnapshotResponseV1 expectedBody = mock(PermissionSnapshotResponseV1.class);

		when(currentUser.requireUser()).thenReturn(viewer);
		when(userService.findById(1L)).thenReturn(Optional.of(viewerRecord));
		when(userService.getAllUsers()).thenReturn(List.of(viewerRecord, otherUser));
		when(permissionService.snapshotForUsers(List.of(viewerRecord, otherUser))).thenReturn(snapshots);
		when(permissionApiMapper.toPermissionSnapshotResponseV1(
				eq(List.of(viewerRecord, otherUser)), eq(snapshots), any(), any())).thenReturn(expectedBody);

		// When:
		ResponseEntity<PermissionSnapshotResponseV1> response = controller.getPermissionManagementSnapshot();

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
		verify(userService).getAllUsers();
		verify(permissionService).snapshotForUsers(List.of(viewerRecord, otherUser));
	}

	@Test
	void getPermissionManagementSnapshotResolvesTeamForTeamLeadWithPermissionTest() {
		// Given: a team lead holding permissions.manage_team_members
		AppUser viewer = new AppUser(3L, "clerk-3", "lead@test.com", "Lead", UserRoleCode.TEAMLEAD, "Lead", "", "",
				"");
		UserRecord viewerRecord = new UserRecord(3L, "clerk-3", "Lead", "lead@test.com", UserRoleCode.TEAMLEAD, "", ""
				, "", null, null, null);
		UserRecord member = new UserRecord(4L, "clerk-4", "Member", "member@test.com", UserRoleCode.MEMBER, "", "", ""
				, null, null, null);
		PermissionSnapshotResponseV1 expectedBody = mock(PermissionSnapshotResponseV1.class);

		when(currentUser.requireUser()).thenReturn(viewer);
		when(userService.findById(3L)).thenReturn(Optional.of(viewerRecord));
		when(permissionService.userHasPermission(viewer, PermissionKeys.PERMISSIONS_MANAGE_TEAM_MEMBERS)).thenReturn(true);
		when(teamService.getAssignableLearningUsers(viewer)).thenReturn(List.of(member));
		when(permissionService.snapshotForUsers(any())).thenReturn(Map.of());
		when(permissionApiMapper.toPermissionSnapshotResponseV1(any(), any(), any(), any())).thenReturn(expectedBody);

		// When:
		ResponseEntity<PermissionSnapshotResponseV1> response = controller.getPermissionManagementSnapshot();

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
		ArgumentCaptor<List<UserRecord>> usersCaptor = ArgumentCaptor.forClass(List.class);
		verify(permissionApiMapper).toPermissionSnapshotResponseV1(usersCaptor.capture(), any(), any(), any());
		assertThat(usersCaptor.getValue()).containsExactly(viewerRecord, member);
		verify(userService, never()).getAllUsers();
	}
}
