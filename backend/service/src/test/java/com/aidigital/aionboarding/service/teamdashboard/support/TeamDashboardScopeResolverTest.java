package com.aidigital.aionboarding.service.teamdashboard.support;

import com.aidigital.aionboarding.domain.common.dictionary.UserRoleCode;
import com.aidigital.aionboarding.domain.common.dictionary.entities.UserRole;
import com.aidigital.aionboarding.domain.group.entities.GroupMember;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.group.services.entity.GroupLeadEntityService;
import com.aidigital.aionboarding.service.group.services.entity.GroupMemberEntityService;
import com.aidigital.aionboarding.service.mappers.user.UserRecordMapper;
import com.aidigital.aionboarding.service.teamdashboard.models.TeamDashboardScope;
import com.aidigital.aionboarding.service.teamdashboard.util.TeamDashboardSupport;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import com.aidigital.aionboarding.service.user.services.entity.UserEntityService;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.Select.field;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamDashboardScopeResolverTest {

	@Mock
	private GroupLeadEntityService groupLeadEntityService;
	@Mock
	private GroupMemberEntityService groupMemberEntityService;
	@Mock
	private UserEntityService userEntityService;
	@Mock
	private UserRecordMapper userMapper;
	@Mock
	private TeamDashboardSupport teamDashboardSupport;

	@InjectMocks
	private TeamDashboardScopeResolver resolver;

	@Test
	void resolveVisibleTeamScopeShouldReturnUnionOfEveryLedGroupTest() {
		// Given:
		AppUser viewer = new AppUser(1L, "clerk-lead", "lead@test.com", "Lead", UserRoleCode.TEAMLEAD, "Lead", null,
				null, null);
		User leadUser = Instancio.of(User.class).set(field(User::getId), 1L).create();
		UserRecord leadRecord = new UserRecord(1L, "clerk-lead", "Lead", "lead@test.com", UserRoleCode.TEAMLEAD, null,
				null, null, null, null, null);
		UserRecord memberRecord = new UserRecord(2L, "clerk-member", "Member", "member@test.com", UserRoleCode.MEMBER,
				null, null, null, null, null, null);
		User memberUser = Instancio.of(User.class).set(field(User::getId), 2L).create();
		GroupMember groupMember =
				Instancio.of(GroupMember.class).set(field(GroupMember::getMemberUser), memberUser).create();

		when(groupLeadEntityService.findGroupIdsByLeadUserId(1L)).thenReturn(Set.of(10L, 20L));
		when(userEntityService.findById(1L)).thenReturn(Optional.of(leadUser));
		when(userMapper.toRecord(leadUser)).thenReturn(leadRecord);
		when(groupMemberEntityService.findByGroupIdIn(Set.of(10L, 20L))).thenReturn(List.of(groupMember));
		when(userMapper.toRecord(memberUser)).thenReturn(memberRecord);
		when(teamDashboardSupport.uniqById(eq(List.of(leadRecord, memberRecord)), any())).thenReturn(List.of(leadRecord, memberRecord));

		// When:
		TeamDashboardScope scope = resolver.resolveVisibleTeamScope(viewer);

		// Then:
		assertThat(scope.label()).isEqualTo("My team");
		assertThat(scope.leadId()).isEqualTo(1L);
		assertThat(scope.members()).containsExactly(leadRecord, memberRecord);
	}

	@Test
	void resolveVisibleTeamScopeShouldReturnAdminFallbackExcludingSelfAndAdminRoleUsersWhenAdminLeadsNoGroupTest() {
		// Given:
		AppUser viewer = new AppUser(9L, "clerk-admin", "admin@test.com", "Admin", UserRoleCode.ADMIN, "Admin", null,
				null, null);
		when(groupMemberEntityService.findAllWithMembers()).thenReturn(List.of());

		UserRole memberRole = Instancio.of(UserRole.class).set(field(UserRole::getCode), UserRoleCode.MEMBER).create();
		UserRole adminRole = Instancio.of(UserRole.class).set(field(UserRole::getCode), UserRoleCode.ADMIN).create();

		User self = Instancio.of(User.class).set(field(User::getId), 9L).set(field(User::getRole), adminRole).create();
		User otherAdmin =
				Instancio.of(User.class).set(field(User::getId), 10L).set(field(User::getRole), adminRole).create();
		User regularMember = Instancio.of(User.class).set(field(User::getId), 11L).set(field(User::getRole),
				memberRole).create();

		when(userEntityService.findAll()).thenReturn(List.of(self, otherAdmin, regularMember));
		UserRecord regularMemberRecord = new UserRecord(11L, "clerk-11", "Member", "member@test.com",
				UserRoleCode.MEMBER, null, null, null, null, null, null);
		when(userMapper.toRecord(regularMember)).thenReturn(regularMemberRecord);

		// When:
		TeamDashboardScope scope = resolver.resolveVisibleTeamScope(viewer);

		// Then:
		assertThat(scope.label()).isEqualTo("Organization");
		assertThat(scope.leadId()).isNull();
		assertThat(scope.members()).containsExactly(regularMemberRecord);
	}

	@Test
	void resolveVisibleTeamScopeShouldReturnOrganizationScopeFromAllGroupsWhenAdminLeadsNoGroupButGroupsExistTest() {
		// Given:
		AppUser viewer = new AppUser(9L, "clerk-admin", "admin@test.com", "Admin", UserRoleCode.ADMIN, "Admin", null,
				null, null);
		User memberUser = Instancio.of(User.class).set(field(User::getId), 11L).create();
		GroupMember groupMember =
				Instancio.of(GroupMember.class).set(field(GroupMember::getMemberUser), memberUser).create();
		UserRecord memberRecord = new UserRecord(11L, "clerk-11", "Member", "member@test.com", UserRoleCode.MEMBER,
				null, null, null, null, null, null);
		when(groupMemberEntityService.findAllWithMembers()).thenReturn(List.of(groupMember));
		when(userMapper.toRecord(memberUser)).thenReturn(memberRecord);
		when(teamDashboardSupport.uniqById(eq(List.of(memberRecord)), any())).thenReturn(List.of(memberRecord));

		// When:
		TeamDashboardScope scope = resolver.resolveVisibleTeamScope(viewer);

		// Then:
		assertThat(scope.label()).isEqualTo("Organization");
		assertThat(scope.leadId()).isNull();
		assertThat(scope.members()).containsExactly(memberRecord);
	}

	@Test
	void resolveVisibleTeamScopeShouldPreferOrganizationScopeForAdminEvenWhenAdminCouldLeadGroupsTest() {
		// Given:
		AppUser viewer = new AppUser(9L, "clerk-admin", "admin@test.com", "Admin", UserRoleCode.ADMIN, "Admin", null,
				null, null);
		User memberUser = Instancio.of(User.class).set(field(User::getId), 11L).create();
		GroupMember groupMember =
				Instancio.of(GroupMember.class).set(field(GroupMember::getMemberUser), memberUser).create();
		UserRecord memberRecord = new UserRecord(11L, "clerk-11", "Member", "member@test.com", UserRoleCode.MEMBER,
				null, null, null, null, null, null);
		when(groupMemberEntityService.findAllWithMembers()).thenReturn(List.of(groupMember));
		when(userMapper.toRecord(memberUser)).thenReturn(memberRecord);
		when(teamDashboardSupport.uniqById(eq(List.of(memberRecord)), any())).thenReturn(List.of(memberRecord));

		// When:
		TeamDashboardScope scope = resolver.resolveVisibleTeamScope(viewer);

		// Then:
		assertThat(scope.label()).isEqualTo("Organization");
		assertThat(scope.leadId()).isNull();
		assertThat(scope.members()).containsExactly(memberRecord);
		verifyNoInteractions(groupLeadEntityService);
	}

	@Test
	void resolveVisibleTeamScopeShouldReturnEmptyMyTeamScopeWhenNonAdminLeadsNoGroupTest() {
		// Given:
		AppUser viewer = new AppUser(5L, "clerk-member", "member@test.com", "Member", UserRoleCode.MEMBER, "Member",
				null, null, null);
		when(groupLeadEntityService.findGroupIdsByLeadUserId(5L)).thenReturn(Set.of());

		// When:
		TeamDashboardScope scope = resolver.resolveVisibleTeamScope(viewer);

		// Then:
		assertThat(scope.label()).isEqualTo("My team");
		assertThat(scope.leadId()).isNull();
		assertThat(scope.members()).isEmpty();
	}
}
