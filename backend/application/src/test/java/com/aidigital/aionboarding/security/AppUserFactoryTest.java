package com.aidigital.aionboarding.security;

import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.security.RequestAuthenticationCache;
import com.aidigital.aionboarding.service.user.services.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppUserFactoryTest {

	@Mock
	private UserService userService;
	@Mock
	private RequestAuthenticationCache requestAuthenticationCache;

	@InjectMocks
	private AppUserFactory factory;

	private JwtAuthenticationToken jwtAuth(String userId, String email, String fullName) {
		Jwt.Builder builder = Jwt.withTokenValue("token")
				.header("alg", "none")
				.subject(userId)
				.claim("user_id", userId)
				.claim("email", email);
		if (fullName != null) {
			builder.claim("full_name", fullName);
		}
		return new JwtAuthenticationToken(builder.build());
	}

	@Test
	void shouldReturnCachedUserWithoutResolvingWhenAlreadyCachedTest() {
		// Given: a protected request commonly resolves via @PreAuthorize SpEL
		// then again via CurrentUserSupport.requireUser(); the second call must not re-hit
		// the database.
		AppUser cached = new AppUser(1L, "clerk-1", "user@aidigital.com", "User", "member", "User", null, null, null);
		when(requestAuthenticationCache.getUser()).thenReturn(Optional.of(cached));
		Authentication auth = jwtAuth("clerk-1", "user@aidigital.com", "User");

		// When:
		AppUser result = factory.from(auth);

		// Then:
		assertThat(result).isSameAs(cached);
		verify(userService, never()).resolveOrCreateFromClerk(
				org.mockito.ArgumentMatchers.anyString(),
				org.mockito.ArgumentMatchers.anyString(),
				org.mockito.ArgumentMatchers.anyString());
	}

	@Test
	void shouldResolveAndCacheUserOnFirstCallTest() {
		// Given:
		AppUser resolved = new AppUser(1L, "clerk-1", "user@aidigital.com", "User", "member", "User", null, null,
				null);
		when(requestAuthenticationCache.getUser()).thenReturn(Optional.empty());
		when(userService.resolveOrCreateFromClerk("clerk-1", "user@aidigital.com", "User")).thenReturn(resolved);
		Authentication auth = jwtAuth("clerk-1", "user@aidigital.com", "User");

		// When:
		AppUser result = factory.from(auth);

		// Then:
		assertThat(result).isSameAs(resolved);
		verify(requestAuthenticationCache).putUser(resolved);
	}

	@Test
	void shouldLowercaseAndTrimEmailClaimTest() {
		// Given:
		AppUser resolved = new AppUser(1L, "clerk-1", "user@aidigital.com", "User", "member", "User", null, null,
				null);
		when(requestAuthenticationCache.getUser()).thenReturn(Optional.empty());
		when(userService.resolveOrCreateFromClerk("clerk-1", "user@aidigital.com", "User")).thenReturn(resolved);
		Authentication auth = jwtAuth("clerk-1", "  User@Aidigital.com  ", "User");

		// When:
		factory.from(auth);

		// Then:
		verify(userService).resolveOrCreateFromClerk("clerk-1", "user@aidigital.com", "User");
	}

	@Test
	void shouldFallBackToEmailWhenFullNameClaimIsMissingTest() {
		// Given:
		AppUser resolved = new AppUser(1L, "clerk-1", "user@aidigital.com", "user@aidigital.com", "member", "user" +
				"@aidigital.com", null, null, null);
		when(requestAuthenticationCache.getUser()).thenReturn(Optional.empty());
		when(userService.resolveOrCreateFromClerk("clerk-1", "user@aidigital.com", "user@aidigital.com")).thenReturn(resolved);
		Authentication auth = jwtAuth("clerk-1", "user@aidigital.com", null);

		// When:
		factory.from(auth);

		// Then:
		verify(userService).resolveOrCreateFromClerk("clerk-1", "user@aidigital.com", "user@aidigital.com");
	}

	@Test
	void shouldRejectNonJwtAuthenticationWhenNotCachedTest() {
		// Given:
		when(requestAuthenticationCache.getUser()).thenReturn(Optional.empty());
		Authentication auth = org.mockito.Mockito.mock(Authentication.class);

		// When / Then:
		assertThatThrownBy(() -> factory.from(auth))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Clerk JWT");
	}
}
