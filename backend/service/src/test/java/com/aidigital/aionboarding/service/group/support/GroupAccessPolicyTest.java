package com.aidigital.aionboarding.service.group.support;

import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.group.services.entity.GroupLeadEntityService;
import com.aidigital.aionboarding.service.group.services.entity.GroupMemberEntityService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupAccessPolicyTest {

	@Mock
	private GroupLeadEntityService groupLeadEntityService;
	@Mock
	private GroupMemberEntityService groupMemberEntityService;

	@InjectMocks
	private GroupAccessPolicy policy;

	@Nested
	class CanManageGroup {

		@Test
		void canManageGroupShouldReturnTrueForAdminRegardlessOfLeadershipTest() {
			// Given:
			AppUser admin = new AppUser(1L, "clerk-admin", "admin@test.com", "Admin", "admin", "Admin", null, null,
					null);

			// When:
			boolean result = policy.canManageGroup(admin, 42L);

			// Then:
			assertThat(result).isTrue();
		}

		@Test
		void canManageGroupShouldReturnTrueForTeamLeadWhoLeadsTheGroupTest() {
			// Given:
			AppUser lead = new AppUser(2L, "clerk-lead", "lead@test.com", "Lead", "teamlead", "Lead", null, null,
					null);
			when(groupLeadEntityService.existsByGroupIdAndLeadUserId(42L, 2L)).thenReturn(true);

			// When:
			boolean result = policy.canManageGroup(lead, 42L);

			// Then:
			assertThat(result).isTrue();
		}

		@Test
		void canManageGroupShouldReturnFalseForTeamLeadWhoDoesNotLeadTheGroupTest() {
			// Given:
			AppUser lead = new AppUser(2L, "clerk-lead", "lead@test.com", "Lead", "teamlead", "Lead", null, null,
					null);
			when(groupLeadEntityService.existsByGroupIdAndLeadUserId(42L, 2L)).thenReturn(false);

			// When:
			boolean result = policy.canManageGroup(lead, 42L);

			// Then:
			assertThat(result).isFalse();
		}

		@Test
		void canManageGroupShouldReturnFalseForPlainMemberTest() {
			// Given:
			AppUser member = new AppUser(3L, "clerk-member", "member@test.com", "Member", "member", "Member", null,
					null, null);

			// When:
			boolean result = policy.canManageGroup(member, 42L);

			// Then:
			assertThat(result).isFalse();
		}

		@Test
		void canManageGroupShouldReturnFalseWhenViewerIsNullTest() {
			// When:
			boolean result = policy.canManageGroup(null, 42L);

			// Then:
			assertThat(result).isFalse();
		}
	}

	@Nested
	class CanEditMemberGrade {

		@Test
		void canEditMemberGradeShouldReturnTrueForAdminTest() {
			// Given:
			AppUser admin = new AppUser(1L, "clerk-admin", "admin@test.com", "Admin", "admin", "Admin", null, null,
					null);

			// When:
			boolean result = policy.canEditMemberGrade(admin, 99L);

			// Then:
			assertThat(result).isTrue();
		}

		@Test
		void canEditMemberGradeShouldReturnTrueWhenLeaderAndTargetShareAGroupTest() {
			// Given:
			AppUser lead = new AppUser(2L, "clerk-lead", "lead@test.com", "Lead", "teamlead", "Lead", null, null,
					null);
			when(groupLeadEntityService.findGroupIdsByLeadUserId(2L)).thenReturn(Set.of(10L, 20L));
			when(groupMemberEntityService.findGroupIdsByMemberUserId(99L)).thenReturn(Set.of(20L, 30L));

			// When:
			boolean result = policy.canEditMemberGrade(lead, 99L);

			// Then:
			assertThat(result).isTrue();
		}

		@Test
		void canEditMemberGradeShouldReturnFalseWhenLeaderAndTargetShareNoGroupTest() {
			// Given:
			AppUser lead = new AppUser(2L, "clerk-lead", "lead@test.com", "Lead", "teamlead", "Lead", null, null,
					null);
			when(groupLeadEntityService.findGroupIdsByLeadUserId(2L)).thenReturn(Set.of(10L));
			when(groupMemberEntityService.findGroupIdsByMemberUserId(99L)).thenReturn(Set.of(30L));

			// When:
			boolean result = policy.canEditMemberGrade(lead, 99L);

			// Then:
			assertThat(result).isFalse();
		}

		@Test
		void canEditMemberGradeShouldReturnFalseWhenLeaderLeadsNoGroupsTest() {
			// Given:
			AppUser lead = new AppUser(2L, "clerk-lead", "lead@test.com", "Lead", "teamlead", "Lead", null, null,
					null);
			when(groupLeadEntityService.findGroupIdsByLeadUserId(2L)).thenReturn(Set.of());

			// When:
			boolean result = policy.canEditMemberGrade(lead, 99L);

			// Then:
			assertThat(result).isFalse();
		}

		@Test
		void canEditMemberGradeShouldReturnFalseForPlainMemberTest() {
			// Given:
			AppUser member = new AppUser(3L, "clerk-member", "member@test.com", "Member", "member", "Member", null,
					null, null);

			// When:
			boolean result = policy.canEditMemberGrade(member, 99L);

			// Then:
			assertThat(result).isFalse();
		}
	}
}
