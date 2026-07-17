package com.aidigital.aionboarding.controllers;

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
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.team.models.TeamRecord;
import com.aidigital.aionboarding.service.team.services.TeamService;
import com.aidigital.aionboarding.service.user.models.AdminUserStatsRecord;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import com.aidigital.aionboarding.service.user.services.UserService;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.instancio.Select.field;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

	@Mock
	private CurrentUserSupport currentUser;
	@Mock
	private TeamService teamService;
	@Mock
	private UserService userService;
	@Mock
	private TeamApiMapper teamApiMapper;
	@Mock
	private UserApiMapper userApiMapper;

	@InjectMocks
	private AdminController controller;

	@Test
	void shouldGetTeamLeadAdminViewTest() {
		// Given:
		AppUser viewer = viewer();
		TeamRecord team = Instancio.create(TeamRecord.class);
		UserRecord candidate = Instancio.create(UserRecord.class);
		TeamLeadAdminViewV1 expectedBody = Instancio.create(TeamLeadAdminViewV1.class);
		when(currentUser.requireUser()).thenReturn(viewer);
		when(teamService.getTeams()).thenReturn(List.of(team));
		when(teamService.getTeamCandidateUsers(viewer)).thenReturn(List.of(candidate));
		when(teamApiMapper.toTeamLeadAdminViewV1(List.of(team), List.of(candidate))).thenReturn(expectedBody);

		// When:
		ResponseEntity<TeamLeadAdminViewV1> response = controller.getTeamLeadAdminView();

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
	}

	@Test
	void shouldPromoteTeamLeadByEmailTest() {
		// Given:
		TeamLeadEmailRequestV1 request = Instancio.of(TeamLeadEmailRequestV1.class)
				.set(field("email"), "lead@example.com")
				.create();
		UserRecord user = Instancio.create(UserRecord.class);
		UserProfileV1 expectedBody = Instancio.create(UserProfileV1.class);
		when(teamService.promoteTeamLeadByEmail("lead@example.com")).thenReturn(Optional.of(user));
		when(userApiMapper.toUserProfileV1(user)).thenReturn(expectedBody);

		// When:
		ResponseEntity<UserProfileV1> response = controller.promoteTeamLead(request);

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
	}

	@Test
	void shouldThrowWhenPromotingUnknownEmailTest() {
		// Given:
		TeamLeadEmailRequestV1 request = Instancio.of(TeamLeadEmailRequestV1.class)
				.set(field("email"), "missing@example.com")
				.create();
		when(teamService.promoteTeamLeadByEmail("missing@example.com")).thenReturn(Optional.empty());

		// When / Then:
		assertThatThrownBy(() -> controller.promoteTeamLead(request))
				.isInstanceOf(AppException.class)
				.satisfies(ex -> assertThat(((AppException) ex).getCode()).isEqualTo("C001"));
	}

	@Test
	void shouldDemoteTeamLeadByEmailTest() {
		// Given:
		TeamLeadEmailRequestV1 request = Instancio.of(TeamLeadEmailRequestV1.class)
				.set(field("email"), "lead@example.com")
				.create();
		UserRecord user = Instancio.create(UserRecord.class);
		UserProfileV1 expectedBody = Instancio.create(UserProfileV1.class);
		when(teamService.demoteTeamLeadByEmail("lead@example.com")).thenReturn(Optional.of(user));
		when(userApiMapper.toUserProfileV1(user)).thenReturn(expectedBody);

		// When:
		ResponseEntity<UserProfileV1> response = controller.demoteTeamLead(request);

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
	}

	@Test
	void shouldThrowWhenDemotingUnknownEmailTest() {
		// Given:
		TeamLeadEmailRequestV1 request = Instancio.of(TeamLeadEmailRequestV1.class)
				.set(field("email"), "missing@example.com")
				.create();
		when(teamService.demoteTeamLeadByEmail("missing@example.com")).thenReturn(Optional.empty());

		// When / Then:
		assertThatThrownBy(() -> controller.demoteTeamLead(request))
				.isInstanceOf(AppException.class)
				.satisfies(ex -> assertThat(((AppException) ex).getCode()).isEqualTo("C001"));
	}

	@Test
	void shouldListAdminUsersWithDefaultPaginationAndNoRoleFilterTest() {
		// Given:
		Page<UserRecord> page = new PageImpl<>(List.of());
		AdminUsersListResponseV1 expectedBody = Instancio.create(AdminUsersListResponseV1.class);
		when(userService.listUsers(null, "search", 0, 20)).thenReturn(page);
		when(userApiMapper.toAdminUsersListResponseV1(page)).thenReturn(expectedBody);

		// When:
		ResponseEntity<AdminUsersListResponseV1> response = controller.listAdminUsers("search", null, null, null);

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
	}

	@Test
	void shouldListAdminUsersWithRoleFilterTest() {
		// Given:
		UserRoleCodeV1 role = Instancio.create(UserRoleCodeV1.class);
		Page<UserRecord> page = new PageImpl<>(List.of());
		AdminUsersListResponseV1 expectedBody = Instancio.create(AdminUsersListResponseV1.class);
		when(userService.listUsers(role.getValue(), "search", 1, 50)).thenReturn(page);
		when(userApiMapper.toAdminUsersListResponseV1(page)).thenReturn(expectedBody);

		// When:
		ResponseEntity<AdminUsersListResponseV1> response = controller.listAdminUsers("search", role, 1, 50);

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
	}

	@Test
	void shouldGetAdminUserStatsTest() {
		// Given:
		AdminUserStatsRecord stats = Instancio.create(AdminUserStatsRecord.class);
		AdminUserStatsResponseV1 expectedBody = Instancio.create(AdminUserStatsResponseV1.class);
		when(userService.getAdminUserStats()).thenReturn(stats);
		when(userApiMapper.toAdminUserStatsResponseV1(stats)).thenReturn(expectedBody);

		// When:
		ResponseEntity<AdminUserStatsResponseV1> response = controller.getAdminUserStats();

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
	}

	@Test
	void shouldAssignUserRoleTest() {
		// Given:
		AppUser viewer = viewer();
		AssignUserRoleRequestV1 request = Instancio.of(AssignUserRoleRequestV1.class)
				.set(field("roleCode"), UserRoleCodeV1.TEAMLEAD)
				.create();
		UserRecord updated = Instancio.create(UserRecord.class);
		UserProfileV1 expectedBody = Instancio.create(UserProfileV1.class);
		when(currentUser.requireUser()).thenReturn(viewer);
		when(userService.assignRole(viewer, 7L, "teamlead")).thenReturn(updated);
		when(userApiMapper.toUserProfileV1(updated)).thenReturn(expectedBody);

		// When:
		ResponseEntity<UserProfileV1> response = controller.assignUserRole(7L, request);

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
	}

	AppUser viewer() {
		return Instancio.of(AppUser.class)
				.set(field("internalId"), 1L)
				.set(field("clerkUserId"), "clerk-1")
				.set(field("email"), "v@t.com")
				.set(field("fullName"), "V")
				.set(field("roleCode"), "admin")
				.set(field("name"), "V")
				.set(field("position"), null)
				.set(field("avatarStorageKey"), null)
				.set(field("avatarColor"), null)
				.create();
	}
}
