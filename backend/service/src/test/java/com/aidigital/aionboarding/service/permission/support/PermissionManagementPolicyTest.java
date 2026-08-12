package com.aidigital.aionboarding.service.permission.support;

import com.aidigital.aionboarding.domain.common.dictionary.UserRoleCode;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import com.aidigital.aionboarding.service.permission.services.TeamLeadershipService;
import com.aidigital.aionboarding.service.team.services.TeamService;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import com.aidigital.aionboarding.service.user.services.UserService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionManagementPolicyTest {

	@Mock
	private UserService userService;
	@Mock
	private TeamService teamService;
	@Mock
	private PermissionService permissionService;
	@Mock
	private TeamLeadershipService teamLeadershipService;

	@InjectMocks
	private PermissionManagementPolicy policy;

	@Nested
	class ResolveTargetUsers {

		@Test
		void shouldResolveEveryWorkspaceUserForAnAdminTest() {
			// Given:
			AppUser viewer = new AppUser(1L, "clerk-1", "admin@test.com", "Admin", UserRoleCode.ADMIN, "Admin", "", "",
					"");
			UserRecord viewerRecord = new UserRecord(1L, "clerk-1", "Admin", "admin@test.com", UserRoleCode.ADMIN, "",
					"", "", null, null, null);
			UserRecord otherUser = new UserRecord(2L, "clerk-2", "Member", "member@test.com", UserRoleCode.MEMBER, "",
					"", "", null, null, null);
			when(userService.findById(1L)).thenReturn(Optional.of(viewerRecord));
			when(userService.getAllUsers()).thenReturn(List.of(viewerRecord, otherUser));

			// When:
			List<UserRecord> result = policy.resolveTargetUsers(viewer);

			// Then:
			assertThat(result).containsExactly(viewerRecord, otherUser);
			verify(teamService, never()).getAssignableLearningUsers(viewer);
		}

		@Test
		void shouldResolveAssignableTeamForATeamLeadWithPermissionTest() {
			// Given:
			AppUser viewer = new AppUser(3L, "clerk-3", "lead@test.com", "Lead", UserRoleCode.TEAMLEAD, "Lead", "", "",
					"");
			UserRecord viewerRecord = new UserRecord(3L, "clerk-3", "Lead", "lead@test.com", UserRoleCode.TEAMLEAD, "",
					"", "", null, null, null);
			UserRecord member = new UserRecord(4L, "clerk-4", "Member", "member@test.com", UserRoleCode.MEMBER, "", "",
					"", null, null, null);
			when(userService.findById(3L)).thenReturn(Optional.of(viewerRecord));
			when(permissionService.userHasPermission(viewer, PermissionKeys.PERMISSIONS_MANAGE_TEAM_MEMBERS))
					.thenReturn(true);
			when(teamService.getAssignableLearningUsers(viewer)).thenReturn(List.of(member));

			// When:
			List<UserRecord> result = policy.resolveTargetUsers(viewer);

			// Then:
			assertThat(result).containsExactly(viewerRecord, member);
			verify(userService, never()).getAllUsers();
		}

		@Test
		void shouldResolveOnlyTheViewerForATeamLeadWithoutPermissionTest() {
			// Given:
			AppUser viewer = new AppUser(3L, "clerk-3", "lead@test.com", "Lead", UserRoleCode.TEAMLEAD, "Lead", "", "",
					"");
			UserRecord viewerRecord = new UserRecord(3L, "clerk-3", "Lead", "lead@test.com", UserRoleCode.TEAMLEAD, "",
					"", "", null, null, null);
			when(userService.findById(3L)).thenReturn(Optional.of(viewerRecord));
			when(permissionService.userHasPermission(viewer, PermissionKeys.PERMISSIONS_MANAGE_TEAM_MEMBERS))
					.thenReturn(false);

			// When:
			List<UserRecord> result = policy.resolveTargetUsers(viewer);

			// Then:
			assertThat(result).containsExactly(viewerRecord);
			verify(teamService, never()).getAssignableLearningUsers(viewer);
			verify(userService, never()).getAllUsers();
		}
	}

	@Nested
	class CurrentViewerRecord {

		@Test
		void shouldReturnThePersistedRecordWhenOneExistsTest() {
			// Given:
			AppUser viewer = new AppUser(1L, "clerk-1", "admin@test.com", "Admin", UserRoleCode.ADMIN, "Admin", "", "",
					"");
			UserRecord persisted = new UserRecord(1L, "clerk-1", "Admin", "admin@test.com", UserRoleCode.ADMIN, "", "",
					"", null, null, null);
			when(userService.findById(1L)).thenReturn(Optional.of(persisted));

			// When:
			UserRecord result = policy.currentViewerRecord(viewer);

			// Then:
			assertThat(result).isSameAs(persisted);
		}

		@Test
		void shouldBuildAFallbackRecordFromClaimsWhenNonePersistedTest() {
			// Given:
			AppUser viewer = new AppUser(9L, "clerk-9", "new@test.com", "New User", UserRoleCode.MEMBER, "New User",
					null, null, null);
			when(userService.findById(9L)).thenReturn(Optional.empty());

			// When:
			UserRecord result = policy.currentViewerRecord(viewer);

			// Then:
			assertThat(result.id()).isEqualTo(9L);
			assertThat(result.email()).isEqualTo("new@test.com");
			assertThat(result.roleCode()).isEqualTo(UserRoleCode.MEMBER);
		}
	}

	@Nested
	class ValidateCanManageTarget {

		@Test
		void shouldRejectAnAdminManagingAnotherAdminTest() {
			// Given:
			AppUser admin = new AppUser(1L, "clerk-1", "admin@test.com", "Admin", UserRoleCode.ADMIN, "Admin", "", "",
					"");
			UserRecord otherAdmin = new UserRecord(2L, "clerk-2", "Other", "other@test.com", UserRoleCode.ADMIN, "", "",
					"", null, null, null);

			// When-Then:
			assertThatThrownBy(() -> policy.validateCanManageTarget(admin, otherAdmin))
					.isInstanceOf(AppException.class);
		}

		@Test
		void shouldAllowAnAdminManagingAMemberTest() {
			// Given:
			AppUser admin = new AppUser(1L, "clerk-1", "admin@test.com", "Admin", UserRoleCode.ADMIN, "Admin", "", "",
					"");
			UserRecord member = new UserRecord(2L, "clerk-2", "Member", "member@test.com", UserRoleCode.MEMBER, "", "",
					"", null, null, null);

			// When-Then:
			assertThatCode(() -> policy.validateCanManageTarget(admin, member)).doesNotThrowAnyException();
		}

		@Test
		void shouldAllowATeamLeadManagingTheirOwnMemberTest() {
			// Given:
			AppUser lead = new AppUser(3L, "clerk-3", "lead@test.com", "Lead", UserRoleCode.TEAMLEAD, "Lead", "", "",
					"");
			UserRecord member = new UserRecord(4L, "clerk-4", "Member", "member@test.com", UserRoleCode.MEMBER, "", "",
					"", null, null, null);
			when(teamLeadershipService.isTeamLeadForMember(3L, 4L)).thenReturn(true);

			// When-Then:
			assertThatCode(() -> policy.validateCanManageTarget(lead, member)).doesNotThrowAnyException();
		}

		@Test
		void shouldRejectATeamLeadManagingAMemberTheyDoNotLeadTest() {
			// Given:
			AppUser lead = new AppUser(3L, "clerk-3", "lead@test.com", "Lead", UserRoleCode.TEAMLEAD, "Lead", "", "",
					"");
			UserRecord member = new UserRecord(4L, "clerk-4", "Member", "member@test.com", UserRoleCode.MEMBER, "", "",
					"", null, null, null);
			when(teamLeadershipService.isTeamLeadForMember(3L, 4L)).thenReturn(false);

			// When-Then:
			assertThatThrownBy(() -> policy.validateCanManageTarget(lead, member))
					.isInstanceOf(AppException.class);
		}

		@Test
		void shouldRejectAnOrdinaryMemberManagingAnyoneTest() {
			// Given:
			AppUser member = new AppUser(5L, "clerk-5", "member@test.com", "Member", UserRoleCode.MEMBER, "Member", "",
					"", "");
			UserRecord target = new UserRecord(6L, "clerk-6", "Target", "target@test.com", UserRoleCode.MEMBER, "", "",
					"", null, null, null);

			// When-Then:
			assertThatThrownBy(() -> policy.validateCanManageTarget(member, target))
					.isInstanceOf(AppException.class);
		}
	}

	@Nested
	class RequireManagementPermission {

		@Test
		void shouldRequireTeamleadManagementPermissionForAnAdminTest() {
			// Given:
			AppUser admin = new AppUser(1L, "clerk-1", "admin@test.com", "Admin", UserRoleCode.ADMIN, "Admin", "", "",
					"");
			UserRecord target = new UserRecord(2L, "clerk-2", "Target", "target@test.com", UserRoleCode.TEAMLEAD, "",
					"", "", null, null, null);

			// When:
			policy.requireManagementPermission(admin, target);

			// Then:
			verify(permissionService).requirePermission(admin, PermissionKeys.PERMISSIONS_MANAGE_TEAMLEADS);
		}

		@Test
		void shouldRequireTeamMemberManagementPermissionForATeamLeadManagingAMemberTest() {
			// Given:
			AppUser lead = new AppUser(3L, "clerk-3", "lead@test.com", "Lead", UserRoleCode.TEAMLEAD, "Lead", "", "",
					"");
			UserRecord member = new UserRecord(4L, "clerk-4", "Member", "member@test.com", UserRoleCode.MEMBER, "", "",
					"", null, null, null);

			// When:
			policy.requireManagementPermission(lead, member);

			// Then:
			verify(permissionService).requirePermission(lead, PermissionKeys.PERMISSIONS_MANAGE_TEAM_MEMBERS);
		}

		@Test
		void shouldRejectATeamLeadManagingANonMemberTargetTest() {
			// Given:
			AppUser lead = new AppUser(3L, "clerk-3", "lead@test.com", "Lead", UserRoleCode.TEAMLEAD, "Lead", "", "",
					"");
			UserRecord otherLead = new UserRecord(5L, "clerk-5", "Lead2", "lead2@test.com", UserRoleCode.TEAMLEAD, "",
					"", "", null, null, null);

			// When-Then:
			assertThatThrownBy(() -> policy.requireManagementPermission(lead, otherLead))
					.isInstanceOf(AppException.class);
		}
	}
}
