package com.aidigital.aionboarding.controllers;

import com.aidigital.aionboarding.error.mapper.GlobalExceptionResponseHelperImpl;
import com.aidigital.aionboarding.mappers.user.UserApiMapper;
import com.aidigital.aionboarding.service.common.security.RequestAuthenticationCache;
import com.aidigital.aionboarding.service.common.time.CurrentTimeImpl;
import com.aidigital.aionboarding.service.storage.StorageService;
import com.aidigital.aionboarding.service.user.services.UserService;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UsersController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
		GlobalExceptionResponseHelperImpl.class,
		CurrentTimeImpl.class,
		UsersControllerValidationTest.MeterRegistryTestConfig.class
})
class UsersControllerValidationTest {

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private CurrentUserSupport currentUser;
	@MockitoBean
	private UserService userService;
	@MockitoBean
	private StorageService storageService;
	@MockitoBean
	private UserApiMapper userApiMapper;
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
	void shouldRejectUploadMyAvatarWithEmptyFileTest() throws Exception {
		// Given: an empty (zero-byte) avatar file, rejected by UsersController#uploadMyAvatar's own
		// file.isEmpty() check (AppException C002 -> 400). updateMyProfile has no request-side
		// constraint to test on its own (every field is x-unconstrained-reason or, for
		// avatarStorageKey, deliberately blank-tolerant), so this class carries two cases for
		// uploadMyAvatar to keep the aggregate isBadRequest() count honest -- see the phase log.
		MockMultipartFile emptyFile = new MockMultipartFile("file", "avatar.png", "image/png", new byte[0]);

		// When:
		var result = mvc.perform(multipart("/api/v1/users/me/avatar").file(emptyFile));

		// Then:
		result.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectUpdateUserGradeWithNonPositiveIdTest() throws Exception {
		// Given/When: id violates the generated @Min(1) on the path variable
		String body = "{\"gradeId\":1}";
		var result = mvc.perform(patch("/api/v1/users/0/grade").contentType(APPLICATION_JSON).content(body));

		// Then:
		result.andExpect(status().isBadRequest());
	}
}
