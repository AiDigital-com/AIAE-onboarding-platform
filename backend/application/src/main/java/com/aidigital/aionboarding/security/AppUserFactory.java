package com.aidigital.aionboarding.security;

import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.security.RequestAuthenticationCache;
import com.aidigital.aionboarding.service.user.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;

/**
 * Resolves the authenticated {@link AppUser} from a Clerk JWT, reusing one resolution per
 * HTTP request via {@link RequestAuthenticationCache} — a protected controller method is
 * commonly resolved twice per request (once by {@code @PreAuthorize} SpEL, once via
 * {@code CurrentUserSupport.requireUser()}), and without this cache each call independently
 * re-queries and unconditionally re-writes the {@code users} row.
 */
@Component
@RequiredArgsConstructor
public class AppUserFactory {

	private static final String CLAIM_USER_ID = "user_id";
	private static final String CLAIM_EMAIL = "email";
	private static final String CLAIM_FULLNAME = "full_name";

	private final UserService userService;
	private final RequestAuthenticationCache requestAuthenticationCache;

	/**
	 * Returns the {@link AppUser} for the given authentication, reusing the request-scoped
	 * cache when this request has already resolved one.
	 *
	 * @param auth the current request's authentication (must be a Clerk JWT)
	 * @return the resolved (or cached) app user
	 */
	public AppUser from(Authentication auth) {
		Optional<AppUser> cached = requestAuthenticationCache.getUser();
		if (cached.isPresent()) {
			return cached.get();
		}

		if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
			throw new IllegalStateException(
					"Unsupported authentication type; expected a Clerk JWT: "
							+ (auth == null ? "null" : auth.getClass().getName()));
		}
		Jwt jwt = jwtAuth.getToken();

		String userId = jwt.getClaimAsString(CLAIM_USER_ID);
		if (userId == null || userId.isBlank()) {
			throw new IllegalStateException("Clerk JWT is missing required claim: user_id");
		}

		String rawEmail = jwt.getClaimAsString(CLAIM_EMAIL);
		if (rawEmail == null || rawEmail.isBlank()) {
			throw new IllegalStateException("Clerk JWT is missing required claim: email");
		}
		String email = rawEmail.trim().toLowerCase(Locale.ROOT);

		String fullName = Optional.ofNullable(jwt.getClaimAsString(CLAIM_FULLNAME))
				.filter(v -> !v.isBlank())
				.orElse(email);

		AppUser resolved = userService.resolveOrCreateFromClerk(userId, email, fullName);
		requestAuthenticationCache.putUser(resolved);
		return resolved;
	}
}
