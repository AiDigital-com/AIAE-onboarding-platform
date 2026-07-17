package com.aidigital.aionboarding.auth.controllers;

import com.aidigital.aionboarding.controllers.AuthController;
import com.aidigital.aionboarding.mappers.auth.UserMapperImpl;
import com.aidigital.aionboarding.security.AppUserFactory;
import com.aidigital.aionboarding.security.AuthProperties;
import com.aidigital.aionboarding.security.ClerkJwtClaimsValidator;
import com.aidigital.aionboarding.security.ClerkPublishableKeyDecoder;
import com.aidigital.aionboarding.security.CompanyEmailDomainAuthorizationManager;
import com.aidigital.aionboarding.security.SecurityConfig;
import com.aidigital.aionboarding.security.SecurityProperties;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.security.RequestAuthenticationCache;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.user.services.UserService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@EnableConfigurationProperties({AuthProperties.class, SecurityProperties.class})
@Import({
		SecurityConfig.class,
		AppUserFactory.class,
		UserMapperImpl.class,
		ClerkJwtClaimsValidator.class,
		ClerkPublishableKeyDecoder.class,
		CompanyEmailDomainAuthorizationManager.class,
		CurrentTime.class,
		RequestAuthenticationCache.class,
		AuthControllerTest.MeterRegistryTestConfig.class
})
class AuthControllerTest {

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@MockitoBean
	private UserService userService;

	@Autowired
	private RequestAuthenticationCache requestAuthenticationCache;

	/**
	 * This slice's context has no request-boundary filter to clear
	 * {@link RequestAuthenticationCache} (that lives in the {@code observability} package,
	 * outside this test's {@code @Import} list), and the cache bean is a singleton reused
	 * across every {@code @Test} method here — clear it explicitly so one test's resolved
	 * user can't leak into the next.
	 */
	@AfterEach
	void clearRequestAuthenticationCache() {
		requestAuthenticationCache.clear();
	}

	/**
	 * {@code @WebMvcTest} auto-registers {@code Filter} beans (including
	 * {@code PerformanceMetricsFilter}), which needs a {@link MeterRegistry} the
	 * slice's minimal auto-configuration doesn't otherwise provide.
	 */
	@TestConfiguration
	static class MeterRegistryTestConfig {

		/**
		 * Provides a lightweight in-memory registry for the slice context.
		 *
		 * @return a simple meter registry
		 */
		@Bean
		MeterRegistry meterRegistry() {
			return new SimpleMeterRegistry();
		}
	}

	@Test
	void shouldRejectUnauthenticatedRequestToAuthMeTest() throws Exception {
		ResultActions response = mvc.perform(get("/api/v1/auth/me"));
		response.andExpect(status().isUnauthorized());
	}

	@Test
	void shouldReturnUserPayloadFromJwtClaimsTest() throws Exception {
		when(userService.resolveOrCreateFromClerk(eq("user_123"), eq("alice@aidigital.com"), eq("Alice Example")))
				.thenReturn(new AppUser(1L, "user_123", "alice@aidigital.com", "Alice Example", "member", null, null,
						null, null));

		ResultActions response = mvc.perform(get("/api/v1/auth/me")
				.with(jwt().jwt(j -> j
						.subject("user_123")
						.claim("user_id", "user_123")
						.claim("email", "alice@aidigital.com")
						.claim("full_name", "Alice Example"))));

		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.userId").value("user_123"))
				.andExpect(jsonPath("$.email").value("alice@aidigital.com"))
				.andExpect(jsonPath("$.fullName").value("Alice Example"));
	}

	@Test
	void shouldRejectInvalidOrExpiredTokenTest() throws Exception {
		when(jwtDecoder.decode(anyString())).thenThrow(new BadJwtException("invalid token"));
		ResultActions response = mvc.perform(get("/api/v1/auth/me")
				.header("Authorization", "Bearer not-a-real-token"));
		response.andExpect(status().isUnauthorized());
	}

	@Test
	void shouldForbidValidJwtWithForeignEmailDomainTest() throws Exception {
		ResultActions response = mvc.perform(get("/api/v1/auth/me")
				.with(jwt().jwt(j -> j
						.subject("user_999")
						.claim("user_id", "user_999")
						.claim("email", "mallory@attacker.example"))));

		response.andExpect(status().isForbidden());
	}

	@Test
	void shouldForbidValidJwtWithoutEmailTest() throws Exception {
		ResultActions response = mvc.perform(get("/api/v1/auth/me")
				.with(jwt().jwt(j -> j
						.subject("user_999")
						.claim("user_id", "user_999"))));

		response.andExpect(status().isForbidden());
	}

	@Test
	void shouldForbidValidJwtWithSubdomainEmailTest() throws Exception {
		ResultActions response = mvc.perform(get("/api/v1/auth/me")
				.with(jwt().jwt(j -> j
						.subject("user_999")
						.claim("user_id", "user_999")
						.claim("email", "bob@team.aidigital.com"))));

		response.andExpect(status().isForbidden());
	}
}
