package com.aidigital.aionboarding.controllers;

import com.aidigital.aionboarding.error.mapper.GlobalExceptionResponseHelperImpl;
import com.aidigital.aionboarding.mappers.common.PermissionDefinitionApiMapper;
import com.aidigital.aionboarding.mappers.common.PermissionDefinitionRegistry;
import com.aidigital.aionboarding.mappers.permission.PermissionApiMapper;
import com.aidigital.aionboarding.service.common.security.RequestAuthenticationCache;
import com.aidigital.aionboarding.service.common.time.CurrentTimeImpl;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import com.aidigital.aionboarding.service.team.services.TeamService;
import com.aidigital.aionboarding.service.user.services.UserService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PermissionsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
		GlobalExceptionResponseHelperImpl.class,
		CurrentTimeImpl.class,
		PermissionsControllerValidationTest.MeterRegistryTestConfig.class
})
class PermissionsControllerValidationTest {

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private CurrentUserSupport currentUser;
	@MockitoBean
	private PermissionService permissionService;
	@MockitoBean
	private UserService userService;
	@MockitoBean
	private TeamService teamService;
	@MockitoBean
	private PermissionApiMapper permissionApiMapper;
	@MockitoBean
	private PermissionDefinitionApiMapper permissionDefinitionApiMapper;
	@MockitoBean
	private PermissionDefinitionRegistry permissionDefinitionRegistry;
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
	void shouldRejectSetPermissionOverridesWithMissingUserIdTest() throws Exception {
		// Given: userId is required (@NotNull) and omitted
		String body = "{\"overrides\":{\"materials.create\":true}}";

		// When:
		var result = mvc.perform(put("/api/v1/permissions").contentType(APPLICATION_JSON).content(body));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectSetPermissionOverridesWithMissingOverridesTest() throws Exception {
		// Given: overrides is required (@NotNull) and omitted
		String body = "{\"userId\":1}";

		// When:
		var result = mvc.perform(put("/api/v1/permissions").contentType(APPLICATION_JSON).content(body));

		// Then:
		result.andExpect(status().isBadRequest());
	}
}
