package com.aidigital.aionboarding.controllers;

import com.aidigital.aionboarding.api.v1.model.AddTeamMemberRequestV1;
import com.aidigital.aionboarding.api.v1.model.AddTeamMemberResponseV1;
import com.aidigital.aionboarding.api.v1.model.OkResponseV1;
import com.aidigital.aionboarding.api.v1.model.RemoveTeamMemberRequestV1;
import com.aidigital.aionboarding.api.v1.model.TeamsResponseV1;
import com.aidigital.aionboarding.mappers.team.TeamApiMapper;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.learning.services.LearningService;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import com.aidigital.aionboarding.service.team.models.TeamRecord;
import com.aidigital.aionboarding.service.team.services.TeamService;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import com.aidigital.aionboarding.support.ApiResponses;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.instancio.Select.field;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamsControllerTest {

	@Mock
	private CurrentUserSupport currentUser;
	@Mock
	private TeamService teamService;
	@Mock
	private LearningService learningService;
	@Mock
	private PermissionService permissionService;
	@Mock
	private TeamApiMapper teamApiMapper;
	@Mock
	private ApiResponses apiResponses;

	@InjectMocks
	private TeamsController controller;

	@Test
	void shouldListTeamsWithDefaultPaginationTest() {
		// Given:
		AppUser viewer = viewer();
		Page<TeamRecord> teams = mock(Page.class);
		Page<UserRecord> candidates = mock(Page.class);
		TeamsResponseV1 expectedBody = Instancio.create(TeamsResponseV1.class);
		when(currentUser.requireUser()).thenReturn(viewer);
		when(teamService.getTeams(eq(viewer), eq("t-query"), PageRequestMatchers.pageable(0, 20))).thenReturn(teams);
		when(teamService.getTeamCandidateUsers(eq(viewer), eq("u-query"), PageRequestMatchers.pageable(0, 20))).thenReturn(candidates);
		when(permissionService.getUserPermissionMap(eq(viewer))).thenReturn(Map.of("teams.manage_members", true));
		when(teamApiMapper.toTeamsResponseV1(eq(teams), eq(candidates), eq(Map.of("teams.manage_members", true)))).thenReturn(expectedBody);

		// When:
		ResponseEntity<TeamsResponseV1> response = controller.listTeams(null, null, "t-query", null, null, "u-query");

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
	}

	@Test
	void shouldAddTeamMemberByMemberIdTest() {
		// Given:
		AppUser viewer = viewer();
		AddTeamMemberRequestV1 request = Instancio.of(AddTeamMemberRequestV1.class)
				.set(field("memberId"), 9L)
				.set(field("member"), (String) null)
				.set(field("email"), (String) null)
				.create();
		UserRecord user = Instancio.create(UserRecord.class);
		AddTeamMemberResponseV1 expectedBody = Instancio.create(AddTeamMemberResponseV1.class);
		when(currentUser.requireUser()).thenReturn(viewer);
		when(permissionService.canManageTeam(viewer, 5L)).thenReturn(true);
		when(teamService.getUserById(5L)).thenReturn(Optional.of(user));
		when(teamService.addTeamMember(5L, 9L, null)).thenReturn(user);
		when(teamApiMapper.toAddTeamMemberResponseV1(user)).thenReturn(expectedBody);

		// When:
		ResponseEntity<AddTeamMemberResponseV1> response = controller.addTeamMember(5L, request);

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
	}

	@Test
	void shouldAddTeamMemberByMemberReferenceWhenMemberIdIsNullTest() {
		// Given:
		AppUser viewer = viewer();
		AddTeamMemberRequestV1 request = Instancio.of(AddTeamMemberRequestV1.class)
				.set(field("memberId"), (Long) null)
				.set(field("member"), "member-ref")
				.set(field("email"), "member@example.com")
				.create();
		UserRecord user = Instancio.create(UserRecord.class);
		AddTeamMemberResponseV1 expectedBody = Instancio.create(AddTeamMemberResponseV1.class);
		when(currentUser.requireUser()).thenReturn(viewer);
		when(permissionService.canManageTeam(viewer, 5L)).thenReturn(true);
		when(teamService.getUserById(5L)).thenReturn(Optional.of(user));
		when(teamService.addTeamMember(5L, null, "member-ref")).thenReturn(user);
		when(teamApiMapper.toAddTeamMemberResponseV1(user)).thenReturn(expectedBody);

		// When:
		ResponseEntity<AddTeamMemberResponseV1> response = controller.addTeamMember(5L, request);

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
	}

	@Test
	void shouldThrowWhenAddingTeamMemberWithoutManagePermissionTest() {
		// Given:
		AppUser viewer = viewer();
		AddTeamMemberRequestV1 request = Instancio.create(AddTeamMemberRequestV1.class);
		when(currentUser.requireUser()).thenReturn(viewer);
		when(permissionService.canManageTeam(viewer, 5L)).thenReturn(false);

		// When / Then:
		assertThatThrownBy(() -> controller.addTeamMember(5L, request))
				.isInstanceOf(AppException.class)
				.satisfies(ex -> assertThat(((AppException) ex).getCode()).isEqualTo("C004"));
	}

	@Test
	void shouldThrowWhenAddingTeamMemberForUnknownLeadTest() {
		// Given:
		AppUser viewer = viewer();
		AddTeamMemberRequestV1 request = Instancio.create(AddTeamMemberRequestV1.class);
		when(currentUser.requireUser()).thenReturn(viewer);
		when(permissionService.canManageTeam(viewer, 5L)).thenReturn(true);
		when(teamService.getUserById(5L)).thenReturn(Optional.empty());

		// When / Then:
		assertThatThrownBy(() -> controller.addTeamMember(5L, request))
				.isInstanceOf(AppException.class)
				.satisfies(ex -> assertThat(((AppException) ex).getCode()).isEqualTo("C001"));
	}

	@Test
	void shouldRemoveTeamMemberTest() {
		// Given:
		AppUser viewer = viewer();
		RemoveTeamMemberRequestV1 request = Instancio.of(RemoveTeamMemberRequestV1.class)
				.set(field("memberId"), 9L)
				.create();
		OkResponseV1 expectedBody = Instancio.create(OkResponseV1.class);
		when(currentUser.requireUser()).thenReturn(viewer);
		when(permissionService.canManageTeam(viewer, 5L)).thenReturn(true);
		when(apiResponses.ok()).thenReturn(expectedBody);

		// When:
		ResponseEntity<OkResponseV1> response = controller.removeTeamMember(5L, request);

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
	}

	@Test
	void shouldRemoveTeamMemberByIdTest() {
		// Given:
		AppUser viewer = viewer();
		OkResponseV1 expectedBody = Instancio.create(OkResponseV1.class);
		when(currentUser.requireUser()).thenReturn(viewer);
		when(permissionService.canManageTeam(viewer, 5L)).thenReturn(true);
		when(apiResponses.ok()).thenReturn(expectedBody);

		// When:
		ResponseEntity<OkResponseV1> response = controller.removeTeamMemberById(5L, 9L);

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
	}

	@Test
	void shouldNormalizePageableBoundsTest() {
		// When / Then:
		Pageable first = controller.pageable(null, null);
		assertThat(first.getPageNumber()).isEqualTo(0);
		assertThat(first.getPageSize()).isEqualTo(20);

		Pageable clamped = controller.pageable(-1, 500);
		assertThat(clamped.getPageNumber()).isEqualTo(0);
		assertThat(clamped.getPageSize()).isEqualTo(100);

		Pageable minSize = controller.pageable(0, 0);
		assertThat(minSize.getPageSize()).isEqualTo(1);
	}

	AppUser viewer() {
		return Instancio.of(AppUser.class)
				.set(field("internalId"), 1L)
				.set(field("clerkUserId"), "clerk-1")
				.set(field("email"), "v@t.com")
				.set(field("fullName"), "V")
				.set(field("roleCode"), "member")
				.set(field("name"), "V")
				.set(field("position"), null)
				.set(field("avatarStorageKey"), null)
				.set(field("avatarColor"), null)
				.create();
	}

	static class PageRequestMatchers {

		static Pageable pageable(int page, int size) {
			return org.mockito.ArgumentMatchers.argThat(p ->
					p.getPageNumber() == page && p.getPageSize() == size
							&& p.getSort().getOrderFor("name") != null
							&& p.getSort().getOrderFor("email") != null);
		}
	}
}
