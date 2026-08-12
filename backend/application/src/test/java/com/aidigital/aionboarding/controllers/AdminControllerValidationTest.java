package com.aidigital.aionboarding.controllers;

import com.aidigital.aionboarding.error.mapper.GlobalExceptionResponseHelperImpl;
import com.aidigital.aionboarding.mappers.common.UserRoleCodeApiMapper;
import com.aidigital.aionboarding.mappers.team.TeamApiMapper;
import com.aidigital.aionboarding.mappers.user.UserApiMapper;
import com.aidigital.aionboarding.service.common.security.RequestAuthenticationCache;
import com.aidigital.aionboarding.service.common.time.CurrentTimeImpl;
import com.aidigital.aionboarding.service.team.services.TeamService;
import com.aidigital.aionboarding.service.user.services.UserService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
		GlobalExceptionResponseHelperImpl.class,
		CurrentTimeImpl.class,
		AdminControllerValidationTest.MeterRegistryTestConfig.class
})
class AdminControllerValidationTest {

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private CurrentUserSupport currentUser;
	@MockitoBean
	private TeamService teamService;
	@MockitoBean
	private UserService userService;
	@MockitoBean
	private TeamApiMapper teamApiMapper;
	@MockitoBean
	private UserApiMapper userApiMapper;
	@MockitoBean
	private PaginationSupport paginationSupport;
	@MockitoBean
	private UserRoleCodeApiMapper userRoleCodeApiMapper;
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
	void shouldRejectListAdminUsersWithOversizedPageTest() throws Exception {
		// Given/When: size violates the generated @Max(100) on the query parameter
		var result = mvc.perform(get("/api/v1/admin/users").param("size", "101"));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectAssignUserRoleWithMissingRoleCodeTest() throws Exception {
		// Given: roleCode is required (@NotNull) and omitted
		String body = "{}";

		// When:
		var result = mvc.perform(patch("/api/v1/admin/users/1/role").contentType(APPLICATION_JSON).content(body));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectAssignUserRoleWithNonPositiveIdTest() throws Exception {
		// Given/When: id violates the generated @Min(1) on the path variable
		String body = "{\"roleCode\":\"member\"}";
		var result = mvc.perform(patch("/api/v1/admin/users/0/role").contentType(APPLICATION_JSON).content(body));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectPromoteTeamLeadWithMissingEmailTest() throws Exception {
		// Given: email is required (@NotNull) and omitted
		String body = "{}";

		// When:
		var result = mvc.perform(post("/api/v1/admin/team-leads").contentType(APPLICATION_JSON).content(body));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectDemoteTeamLeadWithMissingEmailTest() throws Exception {
		// Given: email is required (@NotNull) and omitted
		String body = "{}";

		// When:
		var result = mvc.perform(delete("/api/v1/admin/team-leads").contentType(APPLICATION_JSON).content(body));

		// Then:
		result.andExpect(status().isBadRequest());
	}
}
