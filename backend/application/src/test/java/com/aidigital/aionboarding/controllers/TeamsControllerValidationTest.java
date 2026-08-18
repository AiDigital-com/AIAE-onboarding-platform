package com.aidigital.aionboarding.controllers;

import com.aidigital.aionboarding.error.mapper.GlobalExceptionResponseHelperImpl;
import com.aidigital.aionboarding.mappers.team.TeamApiMapper;
import com.aidigital.aionboarding.service.common.security.RequestAuthenticationCache;
import com.aidigital.aionboarding.service.common.time.CurrentTimeImpl;
import com.aidigital.aionboarding.service.learning.services.RoadmapAssignmentService;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import com.aidigital.aionboarding.service.team.services.TeamService;
import com.aidigital.aionboarding.support.ApiResponses;
import com.aidigital.aionboarding.support.PaginationSupport;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TeamsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
		GlobalExceptionResponseHelperImpl.class,
		CurrentTimeImpl.class,
		TeamsControllerValidationTest.MeterRegistryTestConfig.class
})
class TeamsControllerValidationTest {

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private CurrentUserSupport currentUser;
	@MockitoBean
	private TeamService teamService;
	@MockitoBean
	private RoadmapAssignmentService roadmapAssignmentService;
	@MockitoBean
	private PermissionService permissionService;
	@MockitoBean
	private TeamApiMapper teamApiMapper;
	@MockitoBean
	private ApiResponses apiResponses;
	@MockitoBean
	private PaginationSupport paginationSupport;
	@MockitoBean
	private RequestAuthenticationCache requestAuthenticationCache;

	@TestConfiguration
	static class MeterRegistryTestConfig {

		@Bean
		MeterRegistry meterRegistry() {
			return new SimpleMeterRegistry();
		}
	}

	@Test
	void shouldRejectListTeamsWithOversizedTeamsSizeTest() throws Exception {
		// Given/When: teamsSize violates the generated @Max(100) on the query parameter
		var result = mvc.perform(get("/api/v1/teams").param("teamsSize", "101"));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectAddTeamMemberWithNonPositiveLeadIdTest() throws Exception {
		// Given/When: leadId violates the generated @Min(1) on the path variable
		String body = "{\"email\":\"member@aidigital.com\"}";
		var result = mvc.perform(post("/api/v1/teams/0/members").contentType(APPLICATION_JSON).content(body));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectRemoveTeamMemberWithMissingMemberIdTest() throws Exception {
		// Given: memberId is required (@NotNull) and omitted
		String body = "{}";

		// When:
		var result = mvc.perform(delete("/api/v1/teams/1/members").contentType(APPLICATION_JSON).content(body));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectRemoveTeamMemberByIdWithNonPositiveMemberIdTest() throws Exception {
		// Given/When: memberId violates the generated @Min(1) on the path variable
		var result = mvc.perform(delete("/api/v1/teams/1/members/0"));

		// Then:
		result.andExpect(status().isBadRequest());
	}
}
