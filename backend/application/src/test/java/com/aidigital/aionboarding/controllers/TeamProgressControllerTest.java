package com.aidigital.aionboarding.controllers;

import com.aidigital.aionboarding.api.v1.model.DashboardPeriodV1;
import com.aidigital.aionboarding.api.v1.model.TeamDashboardV1;
import com.aidigital.aionboarding.mappers.teamdashboard.TeamDashboardApiMapper;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.teamdashboard.models.TeamDashboardPeriod;
import com.aidigital.aionboarding.service.teamdashboard.models.TeamDashboardRecord;
import com.aidigital.aionboarding.service.teamdashboard.services.TeamDashboardService;
import com.aidigital.aionboarding.service.teamdashboard.util.TeamDashboardSupport;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamProgressControllerTest {

	@Mock
	private CurrentUserSupport currentUser;
	@Mock
	private TeamDashboardService teamDashboardService;
	@Mock
	private TeamDashboardSupport teamDashboardSupport;
	@Mock
	private TeamDashboardApiMapper teamDashboardApiMapper;

	@InjectMocks
	private TeamProgressController controller;

	@Test
	void shouldGetTeamDashboardForAdminTest() {
		// Given:
		AppUser viewer = adminViewer();
		TeamDashboardRecord record = Instancio.create(TeamDashboardRecord.class);
		TeamDashboardV1 expectedBody = Instancio.create(TeamDashboardV1.class);
		when(currentUser.requireUser()).thenReturn(viewer);
		when(teamDashboardSupport.resolvePeriod("month")).thenReturn(TeamDashboardPeriod.MONTH);
		when(teamDashboardService.getTeamDashboardData(viewer, TeamDashboardPeriod.MONTH)).thenReturn(record);
		when(teamDashboardApiMapper.toTeamDashboardV1(record)).thenReturn(expectedBody);

		// When:
		ResponseEntity<TeamDashboardV1> response = controller.getTeamDashboardData(DashboardPeriodV1.MONTH);

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
	}

	@Test
	void shouldGetTeamDashboardForTeamLeadTest() {
		// Given:
		AppUser viewer = teamLeadViewer();
		TeamDashboardRecord record = Instancio.create(TeamDashboardRecord.class);
		TeamDashboardV1 expectedBody = Instancio.create(TeamDashboardV1.class);
		when(currentUser.requireUser()).thenReturn(viewer);
		when(teamDashboardSupport.resolvePeriod(null)).thenReturn(TeamDashboardPeriod.WEEK);
		when(teamDashboardService.getTeamDashboardData(viewer, TeamDashboardPeriod.WEEK)).thenReturn(record);
		when(teamDashboardApiMapper.toTeamDashboardV1(record)).thenReturn(expectedBody);

		// When:
		ResponseEntity<TeamDashboardV1> response = controller.getTeamDashboardData(null);

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
	}

	@Test
	void shouldRejectNonAdminNonTeamLeadTest() {
		// Given:
		AppUser viewer = memberViewer();
		when(currentUser.requireUser()).thenReturn(viewer);

		// When / Then:
		assertThatThrownBy(() -> controller.getTeamDashboardData(DashboardPeriodV1.MONTH))
				.isInstanceOf(AppException.class)
				.satisfies(ex -> assertThat(((AppException) ex).getCode()).isEqualTo("C004"));
	}

	AppUser adminViewer() {
		return new AppUser(1L, "clerk-1", "admin@test.com", "Admin", "admin", "Admin", null, null, null);
	}

	AppUser teamLeadViewer() {
		return new AppUser(1L, "clerk-1", "lead@test.com", "Lead", "teamlead", "Lead", null, null, null);
	}

	AppUser memberViewer() {
		return new AppUser(1L, "clerk-1", "member@test.com", "Member", "member", "Member", null, null, null);
	}
}
