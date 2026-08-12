package com.aidigital.aionboarding.controllers;

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
import com.aidigital.aionboarding.service.permission.models.PermissionSnapshotRecord;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import com.aidigital.aionboarding.service.permission.support.PermissionManagementPolicy;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import com.aidigital.aionboarding.service.user.services.UserService;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the controller's own responsibility: delegating to
 * {@link PermissionManagementPolicy} and {@link PermissionService}, and mapping the result.
 * The policy's own resolution/authorization logic is covered by
 * {@code PermissionManagementPolicyTest} in the {@code service} module — this class no longer
 * re-tests it through the controller.
 */
@ExtendWith(MockitoExtension.class)
class PermissionsControllerTest {

	@Mock
	private CurrentUserSupport currentUser;
	@Mock
	private PermissionService permissionService;
	@Mock
	private UserService userService;
	@Mock
	private PermissionManagementPolicy permissionManagementPolicy;
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
		when(permissionManagementPolicy.currentViewerRecord(viewer)).thenReturn(viewerRecord);
		when(permissionService.snapshotForUsers(List.of(viewerRecord))).thenReturn(Map.of(1L, snapshot));
		when(permissionApiMapper.toCurrentUserPermissionsResponseV1(snapshot)).thenReturn(expectedBody);

		// When:
		ResponseEntity<CurrentUserPermissionsResponseV1> response = controller.getPermissionSnapshot();

		// Then: bounded to a single-user snapshot lookup, never the org-wide/team-wide resolution
		assertThat(response.getBody()).isSameAs(expectedBody);
		verify(permissionService).snapshotForUsers(List.of(viewerRecord));
		verify(permissionManagementPolicy, org.mockito.Mockito.never()).resolveTargetUsers(any());
	}

	@Test
	void getPermissionManagementSnapshotDelegatesResolutionToThePolicyTest() {
		// Given:
		AppUser viewer = new AppUser(1L, "clerk-1", "admin@test.com", "Admin", UserRoleCode.ADMIN, "Admin", "", "",
				"");
		UserRecord viewerRecord = new UserRecord(1L, "clerk-1", "Admin", "admin@test.com", UserRoleCode.ADMIN, "", "",
				"", null, null, null);
		UserRecord otherUser = new UserRecord(2L, "clerk-2", "Member", "member@test.com", UserRoleCode.MEMBER, "", "",
				"", null, null, null);
		List<UserRecord> resolvedUsers = List.of(viewerRecord, otherUser);
		Map<Long, PermissionSnapshotRecord> snapshots = Map.of(
				1L, new PermissionSnapshotRecord(UserRoleCode.ADMIN, Map.of(), Map.of()),
				2L, new PermissionSnapshotRecord(UserRoleCode.MEMBER, Map.of(), Map.of())
		);
		PermissionSnapshotResponseV1 expectedBody = mock(PermissionSnapshotResponseV1.class);

		when(currentUser.requireUser()).thenReturn(viewer);
		when(permissionManagementPolicy.resolveTargetUsers(viewer)).thenReturn(resolvedUsers);
		when(permissionService.snapshotForUsers(resolvedUsers)).thenReturn(snapshots);
		when(permissionApiMapper.toPermissionSnapshotResponseV1(
				eq(resolvedUsers), eq(snapshots), any(), any())).thenReturn(expectedBody);

		// When:
		ResponseEntity<PermissionSnapshotResponseV1> response = controller.getPermissionManagementSnapshot();

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
		verify(permissionManagementPolicy).resolveTargetUsers(viewer);
		verify(permissionService).snapshotForUsers(resolvedUsers);
	}

	@Test
	void setPermissionOverridesValidatesThenAppliesThroughThePolicyAndServiceTest() {
		// Given:
		AppUser viewer = new AppUser(1L, "clerk-1", "admin@test.com", "Admin", UserRoleCode.ADMIN, "Admin", "", "",
				"");
		UserRecord target = new UserRecord(2L, "clerk-2", "Member", "member@test.com", UserRoleCode.MEMBER, "", "",
				"", null, null, null);
		SetPermissionOverridesRequestV1 request = Instancio.of(SetPermissionOverridesRequestV1.class)
				.set(org.instancio.Select.field("userId"), 2L)
				.create();
		PermissionSnapshotRecord snapshot = new PermissionSnapshotRecord(UserRoleCode.MEMBER, Map.of(), Map.of());
		SetPermissionOverridesResponseV1 expectedBody = mock(SetPermissionOverridesResponseV1.class);

		when(currentUser.requireUser()).thenReturn(viewer);
		when(userService.findById(2L)).thenReturn(Optional.of(target));
		when(permissionService.snapshotForUsers(List.of(target))).thenReturn(Map.of(2L, snapshot));
		when(permissionApiMapper.toSetPermissionOverridesResponseV1(snapshot)).thenReturn(expectedBody);

		// When:
		ResponseEntity<SetPermissionOverridesResponseV1> response = controller.setPermissionOverrides(request);

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
		verify(permissionManagementPolicy).validateCanManageTarget(viewer, target);
		verify(permissionManagementPolicy).requireManagementPermission(viewer, target);
		verify(permissionService).setOverrides(viewer, 2L, request.getOverrides());
	}

	@Test
	void setPermissionOverridesThrowsWhenTargetUserIsMissingTest() {
		// Given:
		AppUser viewer = new AppUser(1L, "clerk-1", "admin@test.com", "Admin", UserRoleCode.ADMIN, "Admin", "", "",
				"");
		SetPermissionOverridesRequestV1 request = Instancio.of(SetPermissionOverridesRequestV1.class)
				.set(org.instancio.Select.field("userId"), 99L)
				.create();
		when(currentUser.requireUser()).thenReturn(viewer);
		when(userService.findById(99L)).thenReturn(Optional.empty());

		// When-Then:
		assertThatThrownBy(() -> controller.setPermissionOverrides(request))
				.isInstanceOf(AppException.class)
				.satisfies(ex -> assertThat(((AppException) ex).getCode()).isEqualTo(ErrorReason.C001.getCode()));
	}
}
