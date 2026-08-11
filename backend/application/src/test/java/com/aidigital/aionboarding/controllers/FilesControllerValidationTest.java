package com.aidigital.aionboarding.controllers;

import com.aidigital.aionboarding.error.mapper.GlobalExceptionResponseHelperImpl;
import com.aidigital.aionboarding.service.common.security.RequestAuthenticationCache;
import com.aidigital.aionboarding.service.common.time.CurrentTimeImpl;
import com.aidigital.aionboarding.service.storage.StorageKeyAuthorizationService;
import com.aidigital.aionboarding.service.storage.StorageService;
import com.aidigital.aionboarding.support.ApiResponses;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FilesController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
		GlobalExceptionResponseHelperImpl.class,
		CurrentTimeImpl.class,
		FilesControllerValidationTest.MeterRegistryTestConfig.class
})
class FilesControllerValidationTest {

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private CurrentUserSupport currentUser;
	@MockitoBean
	private StorageService storageService;
	@MockitoBean
	private StorageKeyAuthorizationService authService;
	@MockitoBean
	private ApiResponses apiResponses;
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
	void shouldRejectGetFilePreviewsWithMissingStorageKeysTest() throws Exception {
		// Given: storageKeys is required (@NotNull) and omitted
		String body = "{}";

		// When:
		var result = mvc.perform(post("/api/v1/files/previews").contentType(APPLICATION_JSON).content(body));

		// Then:
		result.andExpect(status().isBadRequest());
	}
}
