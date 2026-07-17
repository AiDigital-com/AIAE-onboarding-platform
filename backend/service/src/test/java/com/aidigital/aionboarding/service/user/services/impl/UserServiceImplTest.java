package com.aidigital.aionboarding.service.user.services.impl;

import com.aidigital.aionboarding.domain.common.dictionary.UserRoleCode;
import com.aidigital.aionboarding.domain.common.dictionary.entities.UserRole;
import com.aidigital.aionboarding.domain.grade.entities.Grade;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.service.common.dictionary.DictionaryLookupService;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.grade.services.entity.GradeEntityService;
import com.aidigital.aionboarding.service.group.support.GroupAccessPolicy;
import com.aidigital.aionboarding.service.mappers.user.UserRecordMapper;
import com.aidigital.aionboarding.service.permission.models.PermissionSnapshotRecord;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import com.aidigital.aionboarding.service.storage.StorageService;
import com.aidigital.aionboarding.service.team.services.TeamService;
import com.aidigital.aionboarding.service.user.models.AdminUserStatsRecord;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import com.aidigital.aionboarding.service.user.services.UserGradeAssignmentSyncService;
import com.aidigital.aionboarding.service.user.services.entity.UserEntityService;
import org.instancio.Instancio;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.instancio.Select.field;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

	@Mock
	private UserEntityService userEntityService;
	@Mock
	private DictionaryLookupService dictionaryLookupService;
	@Mock
	private GradeEntityService gradeEntityService;
	@Mock
	private GroupAccessPolicy groupAccessPolicy;
	@Mock
	private UserGradeAssignmentSyncService userGradeAssignmentSyncService;
	@Mock
	private UserRecordMapper userMapper;
	@Mock
	private TeamService teamService;
	@Mock
	private PermissionService permissionService;
	@Mock
	private StorageService storageService;

	@Spy
	private CurrentTime currentTime = new CurrentTime();

	@InjectMocks
	private UserServiceImpl service;

	@Nested
	class ResolveOrCreateFromClerk {

		@Test
		void shouldReturnExistingUserWithoutSavingWhenClerkFieldsAreUnchangedTest() {
			// Given: an authenticated request whose Clerk-linked fields already
			// match the stored user must not write a no-op UPDATE on every call.
			User existingUser = Instancio.of(User.class)
					.set(field(User::getClerkUserId), "clerk-1")
					.set(field(User::getEmail), "existing@test.com")
					.set(field(User::getName), "Existing User")
					.create();
			AppUser expectedAppUser = new AppUser(1L, "clerk-1", "existing@test.com", "Existing User", "member",
					"Existing User", null, null, null);
			when(userEntityService.findByClerkUserId("clerk-1")).thenReturn(Optional.of(existingUser));
			when(userMapper.toAppUser(existingUser)).thenReturn(expectedAppUser);

			// When:
			AppUser result = service.resolveOrCreateFromClerk("clerk-1", "existing@test.com", "Existing User");

			// Then:
			assertThat(result).isEqualTo(expectedAppUser);
			verify(userEntityService, never()).findByEmail("existing@test.com");
			verify(userEntityService, never()).save(org.mockito.ArgumentMatchers.any());
		}

		@Test
		void shouldSaveExistingUserWhenEmailHasChangedTest() {
			// Given:
			User existingUser = Instancio.of(User.class)
					.set(field(User::getClerkUserId), "clerk-1")
					.set(field(User::getEmail), "old@test.com")
					.set(field(User::getName), "Existing User")
					.create();
			AppUser expectedAppUser = new AppUser(1L, "clerk-1", "new@test.com", "Existing User", "member", "Existing " +
					"User", null, null, null);
			when(userEntityService.findByClerkUserId("clerk-1")).thenReturn(Optional.of(existingUser));
			when(userEntityService.save(existingUser)).thenReturn(existingUser);
			when(userMapper.toAppUser(existingUser)).thenReturn(expectedAppUser);

			// When:
			AppUser result = service.resolveOrCreateFromClerk("clerk-1", "new@test.com", "Existing User");

			// Then:
			assertThat(result).isEqualTo(expectedAppUser);
			assertThat(existingUser.getEmail()).isEqualTo("new@test.com");
			assertThat(existingUser.getUpdatedAt()).isNotNull();
			verify(userEntityService).save(existingUser);
		}

		@Test
		void shouldReturnEmailMatchedUserAndBackfillClerkIdTest() {
			// Given:
			User existingUser = Instancio.of(User.class)
					.set(field(User::getClerkUserId), null)
					.set(field(User::getEmail), "existing@test.com")
					.set(field(User::getName), "Existing User")
					.create();
			UserRecord existingRecord = new UserRecord(1L, "clerk-2", "Existing User", "existing@test.com", "member",
					null, null, null, null, null, null);
			AppUser expectedAppUser = new AppUser(1L, "clerk-2", "existing@test.com", "Existing User", "member",
					"Existing User", null, null, null);
			when(userEntityService.findByClerkUserId("clerk-2")).thenReturn(Optional.empty());
			when(userEntityService.findByEmail("existing@test.com")).thenReturn(Optional.of(existingUser));
			when(userEntityService.save(existingUser)).thenReturn(existingUser);
			when(userMapper.toAppUser(existingUser)).thenReturn(expectedAppUser);

			// When:
			AppUser result = service.resolveOrCreateFromClerk("clerk-2", "existing@test.com", "Existing User");

			// Then:
			assertThat(result).isEqualTo(expectedAppUser);
			assertThat(existingUser.getClerkUserId()).isEqualTo("clerk-2");
			verify(dictionaryLookupService, never()).getUserRoleReference(org.mockito.ArgumentMatchers.anyString());
		}

		@Test
		void shouldBackfillBlankClerkIdOnEmailMatchedUserTest() {
			// Given:
			User existingUser = Instancio.of(User.class)
					.set(field(User::getClerkUserId), "   ")
					.set(field(User::getEmail), "existing@test.com")
					.set(field(User::getName), "Existing User")
					.create();
			AppUser expectedAppUser = new AppUser(1L, "clerk-3", "existing@test.com", "Existing User", "member",
					"Existing User", null, null, null);
			when(userEntityService.findByClerkUserId("clerk-3")).thenReturn(Optional.empty());
			when(userEntityService.findByEmail("existing@test.com")).thenReturn(Optional.of(existingUser));
			when(userEntityService.save(existingUser)).thenReturn(existingUser);
			when(userMapper.toAppUser(existingUser)).thenReturn(expectedAppUser);

			// When:
			AppUser result = service.resolveOrCreateFromClerk("clerk-3", "existing@test.com", "Existing User");

			// Then:
			assertThat(result).isEqualTo(expectedAppUser);
			assertThat(existingUser.getClerkUserId()).isEqualTo("clerk-3");
		}

		@Test
		void shouldCreateNewUserWithMemberRoleWhenNoMatchFoundTest() {
			// Given:
			UserRole memberRole = Instancio.create(UserRole.class);
			AppUser expectedAppUser = new AppUser(2L, "clerk-4", "new@test.com", "New User", "member", "New User",
					null, null, null);
			when(userEntityService.findByClerkUserId("clerk-4")).thenReturn(Optional.empty());
			when(userEntityService.findByEmail("new@test.com")).thenReturn(Optional.empty());
			when(dictionaryLookupService.getUserRoleReference(UserRoleCode.MEMBER)).thenReturn(memberRole);
			ArgumentCaptor<User> savedUserCaptor = ArgumentCaptor.forClass(User.class);
			when(userEntityService.save(savedUserCaptor.capture())).thenAnswer(invocation -> savedUserCaptor.getValue());
			when(userMapper.toAppUser(org.mockito.ArgumentMatchers.any(User.class))).thenReturn(expectedAppUser);

			// When:
			AppUser result = service.resolveOrCreateFromClerk("clerk-4", "new@test.com", "New User");

			// Then: exactly one save (the creation insert) — no redundant follow-up update
			// for a brand-new user whose fields already match what was just written.
			assertThat(savedUserCaptor.getAllValues()).hasSize(1);
			User created = savedUserCaptor.getAllValues().get(0);
			assertThat(result).isEqualTo(expectedAppUser);
			assertThat(created.getClerkUserId()).isEqualTo("clerk-4");
			assertThat(created.getEmail()).isEqualTo("new@test.com");
			assertThat(created.getName()).isEqualTo("New User");
			assertThat(created.getRole()).isEqualTo(memberRole);
		}
	}

	@Nested
	class UpdateProfile {

		@Test
		void shouldLoadByInternalIdApplyNonNullFieldsAndSaveTest() {
			// Given:
			AppUser viewer = new AppUser(1L, "clerk-1", "viewer@test.com", "Viewer", "member", "Viewer", "old-position"
					, "old-key", "old-color");
			User existingUser = Instancio.of(User.class)
					.set(field(User::getName), "Old Name")
					.set(field(User::getPosition), "old-position")
					.set(field(User::getAvatarStorageKey), "old-key")
					.set(field(User::getAvatarColor), "old-color")
					.create();
			UserRecord updatedRecord = new UserRecord(1L, "clerk-1", "New Name", "viewer@test.com", "member", "new" +
					"-position", "new-key", "new-color", null, null, null);
			when(userEntityService.findById(1L)).thenReturn(Optional.of(existingUser));
			when(userEntityService.save(existingUser)).thenReturn(existingUser);
			when(userMapper.toRecord(existingUser)).thenReturn(updatedRecord);

			// When:
			UserRecord result = service.updateProfile(viewer, "New Name", "new-position", "new-key", "new-color");

			// Then:
			assertThat(result).isEqualTo(updatedRecord);
			assertThat(existingUser.getName()).isEqualTo("New Name");
			assertThat(existingUser.getPosition()).isEqualTo("new-position");
			assertThat(existingUser.getAvatarStorageKey()).isEqualTo("new-key");
			assertThat(existingUser.getAvatarColor()).isEqualTo("new-color");
			assertThat(existingUser.getUpdatedAt()).isNotNull();
			verify(storageService).requireOwnership(viewer, "new-key");
		}

		@Test
		void shouldNotCheckOwnershipWhenAvatarStorageKeyIsUnchangedTest() {
			// Given: the frontend commonly resubmits the current avatarStorageKey unchanged
			// alongside an unrelated field edit (e.g. name); this must not require re-proving
			// ownership of an object the user already owns.
			AppUser viewer = new AppUser(1L, "clerk-1", "viewer@test.com", "Viewer", "member", "Viewer", "old-position"
					, "same-key", "old-color");
			User existingUser = Instancio.of(User.class)
					.set(field(User::getAvatarStorageKey), "same-key")
					.create();
			when(userEntityService.findById(1L)).thenReturn(Optional.of(existingUser));
			when(userEntityService.save(existingUser)).thenReturn(existingUser);
			when(userMapper.toRecord(existingUser)).thenReturn(org.mockito.Mockito.mock(UserRecord.class));

			// When:
			service.updateProfile(viewer, "New Name", "old-position", "same-key", "old-color");

			// Then:
			verify(storageService, never()).requireOwnership(org.mockito.ArgumentMatchers.any(),
					org.mockito.ArgumentMatchers.any());
		}

		@Test
		void shouldRejectWhenAvatarStorageKeyIsNotOwnedByTheCallerTest() {
			// Given:
			AppUser viewer = new AppUser(1L, "clerk-1", "viewer@test.com", "Viewer", "member", "Viewer", "old-position"
					, "old-key", "old-color");
			User existingUser = Instancio.of(User.class)
					.set(field(User::getAvatarStorageKey), "old-key")
					.create();
			when(userEntityService.findById(1L)).thenReturn(Optional.of(existingUser));
			org.mockito.Mockito.doThrow(new AppException(com.aidigital.aionboarding.service.common.error.ErrorReason.C002, "Invalid or expired upload reference."))
					.when(storageService).requireOwnership(viewer, "someone-elses-key");

			// When-Then:
			assertThatThrownBy(() -> service.updateProfile(viewer, null, null, "someone-elses-key", null))
					.isInstanceOf(AppException.class);
			verify(userEntityService, never()).save(org.mockito.ArgumentMatchers.any());
		}
	}

	@Nested
	class UpdateGrade {

		@Test
		void shouldRejectWhenViewerCannotEditMemberGradeTest() {
			// Given:
			AppUser lead = new AppUser(2L, "clerk-lead", "lead@test.com", "Lead", "teamlead", "Lead", null, null,
					null);
			when(groupAccessPolicy.canEditMemberGrade(lead, 5L)).thenReturn(false);

			// When-Then:
			assertThatThrownBy(() -> service.updateGrade(lead, 5L, 10L)).isInstanceOf(AppException.class);
			verify(userEntityService, never()).save(org.mockito.ArgumentMatchers.any());
		}

		@Test
		void shouldSetGradeAndTriggerSyncWhenGradeChangesTest() {
			// Given:
			AppUser admin = new AppUser(1L, "clerk-admin", "admin@test.com", "Admin", "admin", "Admin", null, null,
					null);
			User user = Instancio.of(User.class).set(field(User::getGrade), null).create();
			Grade grade = Instancio.of(Grade.class).set(field(Grade::getId), 10L).create();
			when(groupAccessPolicy.canEditMemberGrade(admin, 5L)).thenReturn(true);
			when(userEntityService.findById(5L)).thenReturn(Optional.of(user));
			when(gradeEntityService.findById(10L)).thenReturn(Optional.of(grade));
			when(userEntityService.save(user)).thenReturn(user);
			UserRecord updatedRecord = new UserRecord(5L, "clerk-5", "Name", "user@test.com", "member", null, null,
					null, 10L, "junior", "Junior");
			when(userMapper.toRecord(user)).thenReturn(updatedRecord);

			// When:
			UserRecord result = service.updateGrade(admin, 5L, 10L);

			// Then:
			assertThat(result.gradeId()).isEqualTo(10L);
			assertThat(user.getGrade()).isEqualTo(grade);
			verify(userGradeAssignmentSyncService).onGradeChanged(5L, 10L);
		}

		@Test
		void shouldClearGradeWhenGradeIdIsNullTest() {
			// Given:
			AppUser admin = new AppUser(1L, "clerk-admin", "admin@test.com", "Admin", "admin", "Admin", null, null,
					null);
			Grade existingGrade = Instancio.of(Grade.class).set(field(Grade::getId), 10L).create();
			User user = Instancio.of(User.class).set(field(User::getGrade), existingGrade).create();
			when(groupAccessPolicy.canEditMemberGrade(admin, 5L)).thenReturn(true);
			when(userEntityService.findById(5L)).thenReturn(Optional.of(user));
			when(userEntityService.save(user)).thenReturn(user);
			UserRecord updatedRecord = new UserRecord(5L, "clerk-5", "Name", "user@test.com", "member", null, null,
					null, null, null, null);
			when(userMapper.toRecord(user)).thenReturn(updatedRecord);

			// When:
			UserRecord result = service.updateGrade(admin, 5L, null);

			// Then:
			assertThat(result.gradeId()).isNull();
			assertThat(user.getGrade()).isNull();
			verify(userGradeAssignmentSyncService).onGradeChanged(5L, null);
		}

		@Test
		void shouldNotTriggerSyncWhenGradeIsUnchangedTest() {
			// Given:
			AppUser admin = new AppUser(1L, "clerk-admin", "admin@test.com", "Admin", "admin", "Admin", null, null,
					null);
			Grade existingGrade = Instancio.of(Grade.class).set(field(Grade::getId), 10L).create();
			User user = Instancio.of(User.class).set(field(User::getGrade), existingGrade).create();
			when(groupAccessPolicy.canEditMemberGrade(admin, 5L)).thenReturn(true);
			when(userEntityService.findById(5L)).thenReturn(Optional.of(user));
			when(gradeEntityService.findById(10L)).thenReturn(Optional.of(existingGrade));
			when(userEntityService.save(user)).thenReturn(user);
			UserRecord updatedRecord = new UserRecord(5L, "clerk-5", "Name", "user@test.com", "member", null, null,
					null, 10L, "junior", "Junior");
			when(userMapper.toRecord(user)).thenReturn(updatedRecord);

			// When:
			service.updateGrade(admin, 5L, 10L);

			// Then:
			verify(userGradeAssignmentSyncService, never()).onGradeChanged(org.mockito.ArgumentMatchers.anyLong(),
					org.mockito.ArgumentMatchers.any());
		}
	}

	@Nested
	class AssignRole {

		@Test
		void shouldRejectWhenViewerIsNotAdminTest() {
			// Given:
			AppUser teamLead = new AppUser(2L, "clerk-lead", "lead@test.com", "Lead", "teamlead", "Lead", null, null,
					null);

			// When-Then:
			assertThatThrownBy(() -> service.assignRole(teamLead, 5L, UserRoleCode.ADMIN)).isInstanceOf(AppException.class);
			verify(userEntityService, never()).save(org.mockito.ArgumentMatchers.any());
		}

		@Test
		void shouldSetNewRoleAndSaveWhenRoleChangesTest() {
			// Given:
			AppUser admin = new AppUser(1L, "clerk-admin", "admin@test.com", "Admin", "admin", "Admin", null, null,
					null);
			UserRole memberRole =
					Instancio.of(UserRole.class).set(field(UserRole::getCode), UserRoleCode.MEMBER).create();
			UserRole teamLeadRole =
					Instancio.of(UserRole.class).set(field(UserRole::getCode), UserRoleCode.TEAMLEAD).create();
			User user = Instancio.of(User.class).set(field(User::getRole), memberRole).create();
			when(userEntityService.findById(5L)).thenReturn(Optional.of(user));
			when(dictionaryLookupService.getUserRoleReference(UserRoleCode.TEAMLEAD)).thenReturn(teamLeadRole);
			when(userEntityService.save(user)).thenReturn(user);
			UserRecord updatedRecord = new UserRecord(5L, "clerk-5", "Name", "user@test.com", "teamlead", null, null,
					null, null, null, null);
			when(userMapper.toRecord(user)).thenReturn(updatedRecord);

			// When:
			UserRecord result = service.assignRole(admin, 5L, UserRoleCode.TEAMLEAD);

			// Then:
			assertThat(result.roleCode()).isEqualTo("teamlead");
			assertThat(user.getRole()).isEqualTo(teamLeadRole);
			assertThat(user.getUpdatedAt()).isNotNull();
			verify(permissionService).resetOverrides(5L);
		}

		@Test
		void shouldSkipSaveWhenRoleIsUnchangedTest() {
			// Given:
			AppUser admin = new AppUser(1L, "clerk-admin", "admin@test.com", "Admin", "admin", "Admin", null, null,
					null);
			UserRole memberRole =
					Instancio.of(UserRole.class).set(field(UserRole::getCode), UserRoleCode.MEMBER).create();
			User user = Instancio.of(User.class).set(field(User::getRole), memberRole).create();
			when(userEntityService.findById(5L)).thenReturn(Optional.of(user));
			UserRecord unchangedRecord = new UserRecord(5L, "clerk-5", "Name", "user@test.com", "member", null, null,
					null, null, null, null);
			when(userMapper.toRecord(user)).thenReturn(unchangedRecord);

			// When:
			UserRecord result = service.assignRole(admin, 5L, UserRoleCode.MEMBER);

			// Then:
			assertThat(result.roleCode()).isEqualTo("member");
			verify(userEntityService, never()).save(org.mockito.ArgumentMatchers.any());
			verify(dictionaryLookupService, never()).getUserRoleReference(org.mockito.ArgumentMatchers.anyString());
			verify(permissionService, never()).resetOverrides(org.mockito.ArgumentMatchers.anyLong());
		}
	}

	@Nested
	class ListUsers {

		@Test
		void shouldMapUserPageFromEntityServiceTest() {
			// Given:
			User user = Instancio.of(User.class).create();
			UserRecord record = Instancio.of(UserRecord.class).create();
			Page<User> userPage = new PageImpl<>(List.of(user), PageRequest.of(0, 20), 1);
			when(userEntityService.search(UserRoleCode.TEAMLEAD, "ann", PageRequest.of(0, 20))).thenReturn(userPage);
			when(userMapper.toRecord(user)).thenReturn(record);

			// When:
			Page<UserRecord> result = service.listUsers(UserRoleCode.TEAMLEAD, "ann", 0, 20);

			// Then:
			assertThat(result.getContent()).containsExactly(record);
			assertThat(result.getTotalElements()).isEqualTo(1);
		}
	}

	@Nested
	class GetAdminUserStats {

		@Test
		void shouldAggregateCountsAndSumEnabledTeamLeadPermissionsTest() {
			// Given:
			UserRole teamLeadRole =
					Instancio.of(UserRole.class).set(field(UserRole::getCode), UserRoleCode.TEAMLEAD).create();
			User teamLead = Instancio.of(User.class).set(field(User::getRole), teamLeadRole).create();
			UserRecord teamLeadRecord = new UserRecord(9L, "clerk-9", "Lead", "lead@test.com", "teamlead", null, null,
					null, null, null, null);
			PermissionSnapshotRecord snapshot = new PermissionSnapshotRecord(
					"teamlead",
					java.util.Map.of("materials.create", true, "materials.delete", false, "lessons.create", true),
					java.util.Map.of()
			);
			when(userEntityService.findByRoleCodeIn(List.of(UserRoleCode.TEAMLEAD))).thenReturn(List.of(teamLead));
			when(userMapper.toRecord(teamLead)).thenReturn(teamLeadRecord);
			when(permissionService.snapshotForUsers(List.of(teamLeadRecord)))
					.thenReturn(java.util.Map.of(9L, snapshot));
			when(userEntityService.count()).thenReturn(6L);
			when(userEntityService.countByRoleCode(UserRoleCode.ADMIN)).thenReturn(2L);

			// When:
			AdminUserStatsRecord result = service.getAdminUserStats();

			// Then:
			assertThat(result.totalUsers()).isEqualTo(6);
			assertThat(result.totalAdmins()).isEqualTo(2);
			assertThat(result.totalTeamLeads()).isEqualTo(1);
			assertThat(result.totalPermissionsEnabled()).isEqualTo(2);
		}
	}
}
