package com.aidigital.aionboarding.service.permission.services.impl;

import com.aidigital.aionboarding.domain.common.dictionary.UserRoleCode;
import com.aidigital.aionboarding.domain.common.dictionary.entities.UserRole;
import com.aidigital.aionboarding.domain.permission.entities.UserPermissionOverride;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.security.RequestAuthenticationCache;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.group.services.entity.GroupLeadEntityService;
import com.aidigital.aionboarding.service.group.services.entity.GroupMemberEntityService;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import com.aidigital.aionboarding.service.permission.models.PermissionSnapshotRecord;
import com.aidigital.aionboarding.service.permission.services.entity.PermissionEntityService;
import com.aidigital.aionboarding.service.permission.support.PermissionDefaultsProvider;
import com.aidigital.aionboarding.service.team.services.entity.TeamEntityService;
import com.aidigital.aionboarding.service.user.models.UserRecord;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.instancio.Select.field;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionServiceImplTest {

	@Mock
	private PermissionEntityService permissionEntityService;
	@Mock
	private TeamEntityService teamEntityService;
	@Mock
	private GroupLeadEntityService groupLeadEntityService;
	@Mock
	private GroupMemberEntityService groupMemberEntityService;
	@Mock
	private UserEntityService userEntityService;
	@Mock
	private PermissionDefaultsProvider permissionDefaultsProvider;
	@Mock
	private RequestAuthenticationCache requestAuthenticationCache;

	@Spy
	private CurrentTime currentTime = new CurrentTime();

	@InjectMocks
	private PermissionServiceImpl service;

	private AppUser adminActor() {
		return new AppUser(1L, "clerk-admin", "admin@test.com", "Admin", "admin", "Admin", null, null, null);
	}

	private AppUser teamLeadActor() {
		return new AppUser(2L, "clerk-lead", "lead@test.com", "Lead", "teamlead", "Lead", null, null, null);
	}

	private AppUser memberActor() {
		return new AppUser(3L, "clerk-member", "member@test.com", "Member", "member", "Member", null, null, null);
	}

	private User userWithRole(Long id, String roleCode) {
		User user = Instancio.of(User.class)
				.set(field(User::getId), id)
				.create();
		UserRole role = Instancio.of(UserRole.class)
				.set(field(UserRole::getCode), roleCode)
				.create();
		user.setRole(role);
		return user;
	}

	private Map<String, Boolean> allFalseDefaults() {
		Map<String, Boolean> map = new LinkedHashMap<>();
		PermissionKeys.ALL.forEach(k -> map.put(k, false));
		return map;
	}

	@Nested
	class SetOverrides {

		@Test
		void setOverridesShouldThrowWhenTargetRoleIsAdminTest() {
			// Given:
			AppUser actor = adminActor();
			Long targetUserId = 10L;
			User target = userWithRole(targetUserId, UserRoleCode.ADMIN);
			when(userEntityService.findByIdForUpdate(targetUserId)).thenReturn(Optional.of(target));

			// When-Then:
			assertThatThrownBy(() -> service.setOverrides(actor, targetUserId, Map.of()))
					.isInstanceOf(AppException.class);
			verify(permissionEntityService, never()).deleteByIdUserId(targetUserId);
		}

		@Test
		void setOverridesShouldThrowWhenActorIsNeitherAdminNorTeamLeadTest() {
			// Given:
			AppUser actor = memberActor();
			Long targetUserId = 11L;
			User target = userWithRole(targetUserId, UserRoleCode.MEMBER);
			when(userEntityService.findByIdForUpdate(targetUserId)).thenReturn(Optional.of(target));

			// When-Then:
			assertThatThrownBy(() -> service.setOverrides(actor, targetUserId, Map.of()))
					.isInstanceOf(AppException.class);
			verify(permissionEntityService, never()).deleteByIdUserId(targetUserId);
		}

		@Test
		void setOverridesShouldAllowTeamLeadTargetingMemberTheyLeadTest() {
			// Given:
			AppUser actor = teamLeadActor();
			Long targetUserId = 12L;
			User target = userWithRole(targetUserId, UserRoleCode.MEMBER);
			when(userEntityService.findByIdForUpdate(targetUserId)).thenReturn(Optional.of(target));
			when(teamEntityService.existsByIdLeadUserIdAndIdMemberUserId(actor.internalId(), targetUserId))
					.thenReturn(true);
			when(permissionDefaultsProvider.baseDefaults(UserRoleCode.MEMBER)).thenReturn(allFalseDefaults());
			when(userEntityService.getReference(actor.internalId())).thenReturn(userWithRole(actor.internalId(),
					UserRoleCode.TEAMLEAD));

			Map<String, Boolean> overrides = Map.of(PermissionKeys.MATERIALS_CREATE, true);

			// When:
			service.setOverrides(actor, targetUserId, overrides);

			// Then:
			verify(permissionEntityService).deleteByIdUserId(targetUserId);
			ArgumentCaptor<UserPermissionOverride> captor = ArgumentCaptor.forClass(UserPermissionOverride.class);
			verify(permissionEntityService).save(captor.capture());
			UserPermissionOverride saved = captor.getValue();
			assertThat(saved.getId().getPermissionKey()).isEqualTo(PermissionKeys.MATERIALS_CREATE);
			assertThat(saved.getAllowed()).isTrue();
		}

		@Test
		void setOverridesShouldThrowWhenTeamLeadTargetsMemberTheyDoNotLeadTest() {
			// Given:
			AppUser actor = teamLeadActor();
			Long targetUserId = 13L;
			User target = userWithRole(targetUserId, UserRoleCode.MEMBER);
			when(userEntityService.findByIdForUpdate(targetUserId)).thenReturn(Optional.of(target));
			when(teamEntityService.existsByIdLeadUserIdAndIdMemberUserId(actor.internalId(), targetUserId))
					.thenReturn(false);
			when(groupLeadEntityService.findGroupIdsByLeadUserId(actor.internalId())).thenReturn(Set.of());

			// When-Then:
			assertThatThrownBy(() -> service.setOverrides(actor, targetUserId, Map.of()))
					.isInstanceOf(AppException.class);
			verify(permissionEntityService, never()).deleteByIdUserId(targetUserId);
		}

		@Test
		void setOverridesShouldStripAdminToTeamleadDenylistKeysTest() {
			// Given:
			AppUser actor = adminActor();
			Long targetUserId = 14L;
			User target = userWithRole(targetUserId, UserRoleCode.TEAMLEAD);
			when(userEntityService.findByIdForUpdate(targetUserId)).thenReturn(Optional.of(target));
			when(permissionDefaultsProvider.baseDefaults(UserRoleCode.TEAMLEAD)).thenReturn(allFalseDefaults());
			when(userEntityService.getReference(actor.internalId())).thenReturn(userWithRole(actor.internalId(),
					UserRoleCode.ADMIN));

			Map<String, Boolean> overrides = new LinkedHashMap<>();
			overrides.put(PermissionKeys.ADMIN_MANAGE_ROLES, true);
			overrides.put(PermissionKeys.PERMISSIONS_MANAGE_TEAMLEADS, true);
			overrides.put(PermissionKeys.MATERIALS_CREATE, true);

			// When:
			service.setOverrides(actor, targetUserId, overrides);

			// Then:
			verify(permissionEntityService).deleteByIdUserId(targetUserId);
			ArgumentCaptor<UserPermissionOverride> captor = ArgumentCaptor.forClass(UserPermissionOverride.class);
			verify(permissionEntityService, times(1)).save(captor.capture());
			List<UserPermissionOverride> saved = captor.getAllValues();
			assertThat(saved).hasSize(1);
			assertThat(saved.get(0).getId().getPermissionKey()).isEqualTo(PermissionKeys.MATERIALS_CREATE);
		}

		@Test
		void setOverridesShouldStripTeamleadToMemberDenylistKeysTest() {
			// Given:
			AppUser actor = teamLeadActor();
			Long targetUserId = 15L;
			User target = userWithRole(targetUserId, UserRoleCode.MEMBER);
			when(userEntityService.findByIdForUpdate(targetUserId)).thenReturn(Optional.of(target));
			when(teamEntityService.existsByIdLeadUserIdAndIdMemberUserId(actor.internalId(), targetUserId))
					.thenReturn(true);
			when(permissionDefaultsProvider.baseDefaults(UserRoleCode.MEMBER)).thenReturn(allFalseDefaults());
			when(userEntityService.getReference(actor.internalId())).thenReturn(userWithRole(actor.internalId(),
					UserRoleCode.TEAMLEAD));

			Map<String, Boolean> overrides = new LinkedHashMap<>();
			overrides.put(PermissionKeys.TEAMS_MANAGE_MEMBERS, true);
			overrides.put(PermissionKeys.LEARNING_ASSIGN, true);
			overrides.put(PermissionKeys.MATERIALS_EDIT, true);

			// When:
			service.setOverrides(actor, targetUserId, overrides);

			// Then:
			verify(permissionEntityService).deleteByIdUserId(targetUserId);
			ArgumentCaptor<UserPermissionOverride> captor = ArgumentCaptor.forClass(UserPermissionOverride.class);
			verify(permissionEntityService, times(1)).save(captor.capture());
			List<UserPermissionOverride> saved = captor.getAllValues();
			assertThat(saved).hasSize(1);
			assertThat(saved.get(0).getId().getPermissionKey()).isEqualTo(PermissionKeys.MATERIALS_EDIT);
		}

		@Test
		void setOverridesShouldDeleteExistingBeforeWritingOnlyNonDefaultKeysTest() {
			// Given:
			AppUser actor = adminActor();
			Long targetUserId = 16L;
			User target = userWithRole(targetUserId, UserRoleCode.MEMBER);
			when(userEntityService.findByIdForUpdate(targetUserId)).thenReturn(Optional.of(target));
			Map<String, Boolean> defaults = allFalseDefaults();
			defaults.put(PermissionKeys.LEARNING_ENROLL, true);
			when(permissionDefaultsProvider.baseDefaults(UserRoleCode.MEMBER)).thenReturn(defaults);
			when(userEntityService.getReference(actor.internalId())).thenReturn(userWithRole(actor.internalId(),
					UserRoleCode.ADMIN));

			Map<String, Boolean> overrides = new LinkedHashMap<>();
			overrides.put(PermissionKeys.LEARNING_ENROLL, true);
			overrides.put(PermissionKeys.MATERIALS_CREATE, true);

			// When:
			service.setOverrides(actor, targetUserId, overrides);

			// Then:
			verify(permissionEntityService).deleteByIdUserId(targetUserId);
			ArgumentCaptor<UserPermissionOverride> captor = ArgumentCaptor.forClass(UserPermissionOverride.class);
			verify(permissionEntityService, times(1)).save(captor.capture());
			List<UserPermissionOverride> saved = captor.getAllValues();
			assertThat(saved).hasSize(1);
			assertThat(saved.get(0).getId().getPermissionKey()).isEqualTo(PermissionKeys.MATERIALS_CREATE);
		}

		@Test
		void setOverridesShouldLockTheTargetUserRowRatherThanUseTheNonLockingLookupTest() {
			// Given: two admins editing the same target user's overrides concurrently must be
			// serialized, not silently interleaved — this only happens if the row is locked.
			AppUser actor = adminActor();
			Long targetUserId = 17L;
			User target = userWithRole(targetUserId, UserRoleCode.MEMBER);
			when(userEntityService.findByIdForUpdate(targetUserId)).thenReturn(Optional.of(target));
			when(permissionDefaultsProvider.baseDefaults(UserRoleCode.MEMBER)).thenReturn(allFalseDefaults());

			// When:
			service.setOverrides(actor, targetUserId, Map.of());

			// Then:
			verify(userEntityService).findByIdForUpdate(targetUserId);
			verify(userEntityService, never()).findById(targetUserId);
		}
	}

	@Nested
	class ResetOverrides {

		@Test
		void resetOverridesShouldDeleteAllStoredOverridesForTheUserTest() {
			// Given:
			Long userId = 17L;

			// When:
			service.resetOverrides(userId);

			// Then:
			verify(permissionEntityService).deleteByIdUserId(userId);
		}
	}

	@Nested
	class IsTeamLeadForMember {

		@Test
		void isTeamLeadForMemberShouldReturnTrueViaLegacyTeamMembershipTest() {
			// Given:
			Long leadUserId = 40L;
			Long memberUserId = 41L;
			when(teamEntityService.existsByIdLeadUserIdAndIdMemberUserId(leadUserId, memberUserId)).thenReturn(true);

			// When:
			boolean result = service.isTeamLeadForMember(leadUserId, memberUserId);

			// Then:
			assertThat(result).isTrue();
		}

		@Test
		void isTeamLeadForMemberShouldReturnTrueWhenMemberBelongsToALedGroupTest() {
			// Given:
			Long leadUserId = 42L;
			Long memberUserId = 43L;
			when(teamEntityService.existsByIdLeadUserIdAndIdMemberUserId(leadUserId, memberUserId)).thenReturn(false);
			when(groupLeadEntityService.findGroupIdsByLeadUserId(leadUserId)).thenReturn(Set.of(100L, 200L));
			when(groupMemberEntityService.findGroupIdsByMemberUserId(memberUserId)).thenReturn(Set.of(200L, 300L));

			// When:
			boolean result = service.isTeamLeadForMember(leadUserId, memberUserId);

			// Then:
			assertThat(result).isTrue();
		}

		@Test
		void isTeamLeadForMemberShouldReturnFalseWhenLeadHasNoLedGroupsTest() {
			// Given:
			Long leadUserId = 44L;
			Long memberUserId = 45L;
			when(teamEntityService.existsByIdLeadUserIdAndIdMemberUserId(leadUserId, memberUserId)).thenReturn(false);
			when(groupLeadEntityService.findGroupIdsByLeadUserId(leadUserId)).thenReturn(Set.of());

			// When:
			boolean result = service.isTeamLeadForMember(leadUserId, memberUserId);

			// Then:
			assertThat(result).isFalse();
			verify(groupMemberEntityService, never()).findGroupIdsByMemberUserId(memberUserId);
		}

		@Test
		void isTeamLeadForMemberShouldReturnFalseWhenMemberIsNotInAnyLedGroupTest() {
			// Given:
			Long leadUserId = 46L;
			Long memberUserId = 47L;
			when(teamEntityService.existsByIdLeadUserIdAndIdMemberUserId(leadUserId, memberUserId)).thenReturn(false);
			when(groupLeadEntityService.findGroupIdsByLeadUserId(leadUserId)).thenReturn(Set.of(100L));
			when(groupMemberEntityService.findGroupIdsByMemberUserId(memberUserId)).thenReturn(Set.of(200L));

			// When:
			boolean result = service.isTeamLeadForMember(leadUserId, memberUserId);

			// Then:
			assertThat(result).isFalse();
		}
	}

	@Nested
	class GetUserPermissionMap {

		@Test
		void getUserPermissionMapShouldMergeRoleDefaultsWithOverridesTest() {
			// Given:
			AppUser user = memberActor();
			Map<String, Boolean> defaults = allFalseDefaults();
			when(permissionDefaultsProvider.baseDefaults(UserRoleCode.MEMBER)).thenReturn(defaults);

			UserPermissionOverride.UserPermissionOverrideId id =
					Instancio.of(UserPermissionOverride.UserPermissionOverrideId.class)
					.set(field(UserPermissionOverride.UserPermissionOverrideId::getUserId), user.internalId())
					.set(field(UserPermissionOverride.UserPermissionOverrideId::getPermissionKey),
							PermissionKeys.MATERIALS_CREATE)
					.create();
			UserPermissionOverride row = Instancio.of(UserPermissionOverride.class)
					.set(field(UserPermissionOverride::getId), id)
					.set(field(UserPermissionOverride::getAllowed), true)
					.create();
			when(permissionEntityService.findByIdUserId(user.internalId())).thenReturn(List.of(row));

			// When:
			Map<String, Boolean> result = service.getUserPermissionMap(user);

			// Then:
			assertThat(result.get(PermissionKeys.MATERIALS_CREATE)).isTrue();
			assertThat(result.get(PermissionKeys.MATERIALS_EDIT)).isFalse();
		}

		@Test
		void getUserPermissionMapShouldIgnoreOverrideKeysNotInPermissionKeysAllTest() {
			// Given:
			AppUser user = memberActor();
			Map<String, Boolean> defaults = allFalseDefaults();
			when(permissionDefaultsProvider.baseDefaults(UserRoleCode.MEMBER)).thenReturn(defaults);

			UserPermissionOverride.UserPermissionOverrideId id =
					Instancio.of(UserPermissionOverride.UserPermissionOverrideId.class)
					.set(field(UserPermissionOverride.UserPermissionOverrideId::getUserId), user.internalId())
					.set(field(UserPermissionOverride.UserPermissionOverrideId::getPermissionKey), "unknown.key")
					.create();
			UserPermissionOverride row = Instancio.of(UserPermissionOverride.class)
					.set(field(UserPermissionOverride::getId), id)
					.set(field(UserPermissionOverride::getAllowed), true)
					.create();
			when(permissionEntityService.findByIdUserId(user.internalId())).thenReturn(List.of(row));

			// When:
			Map<String, Boolean> result = service.getUserPermissionMap(user);

			// Then:
			assertThat(result).doesNotContainKey("unknown.key");
		}

		@Test
		void getUserPermissionMapShouldReturnRequestCachedMapWithoutQueryingTest() {
			// Given:
			AppUser user = memberActor();
			Map<String, Boolean> cachedMap = allFalseDefaults();
			when(requestAuthenticationCache.getPermissionMap(user.internalId())).thenReturn(Optional.of(cachedMap));

			// When:
			Map<String, Boolean> result = service.getUserPermissionMap(user);

			// Then:
			assertThat(result).isSameAs(cachedMap);
			verify(permissionEntityService, never()).findByIdUserId(org.mockito.ArgumentMatchers.anyLong());
		}

		@Test
		void getUserPermissionMapShouldPopulateRequestCacheOnFirstLoadTest() {
			// Given:
			AppUser user = memberActor();
			Map<String, Boolean> defaults = allFalseDefaults();
			when(permissionDefaultsProvider.baseDefaults(UserRoleCode.MEMBER)).thenReturn(defaults);
			when(permissionEntityService.findByIdUserId(user.internalId())).thenReturn(List.of());

			// When:
			Map<String, Boolean> result = service.getUserPermissionMap(user);

			// Then:
			verify(requestAuthenticationCache).putPermissionMap(eq(user.internalId()),
					org.mockito.ArgumentMatchers.same(result));
		}
	}

	@Nested
	class SnapshotForUsers {

		@Test
		void snapshotForUsersShouldGroupOverridesByUserIdTest() {
			// Given:
			UserRecord userOne = new UserRecord(21L, "clerk-1", "One", "one@test.com", UserRoleCode.MEMBER, null, null
					, null, null, null, null);
			UserRecord userTwo = new UserRecord(22L, "clerk-2", "Two", "two@test.com", UserRoleCode.MEMBER, null, null
					, null, null, null, null);
			when(permissionDefaultsProvider.baseDefaults(UserRoleCode.MEMBER)).thenReturn(allFalseDefaults());

			UserPermissionOverride.UserPermissionOverrideId idOne =
					Instancio.of(UserPermissionOverride.UserPermissionOverrideId.class)
					.set(field(UserPermissionOverride.UserPermissionOverrideId::getUserId), 21L)
					.set(field(UserPermissionOverride.UserPermissionOverrideId::getPermissionKey),
							PermissionKeys.MATERIALS_CREATE)
					.create();
			UserPermissionOverride rowOne = Instancio.of(UserPermissionOverride.class)
					.set(field(UserPermissionOverride::getId), idOne)
					.set(field(UserPermissionOverride::getAllowed), true)
					.create();

			when(permissionEntityService.findByIdUserIdIn(List.of(21L, 22L))).thenReturn(List.of(rowOne));

			// When:
			Map<Long, PermissionSnapshotRecord> result = service.snapshotForUsers(List.of(userOne, userTwo));

			// Then:
			assertThat(result).hasSize(2);
			PermissionSnapshotRecord snapshotOne = result.get(21L);
			assertThat(snapshotOne.overrides()).containsEntry(PermissionKeys.MATERIALS_CREATE, true);
			assertThat(snapshotOne.effective()).containsEntry(PermissionKeys.MATERIALS_CREATE, true);
			PermissionSnapshotRecord snapshotTwo = result.get(22L);
			assertThat(snapshotTwo.overrides()).isEmpty();
		}

		@Test
		void snapshotForUsersShouldReturnEmptyMapForEmptyUserListTest() {
			// Given:
			List<UserRecord> users = List.of();

			// When:
			Map<Long, PermissionSnapshotRecord> result = service.snapshotForUsers(users);

			// Then:
			assertThat(result).isEmpty();
		}
	}
}
