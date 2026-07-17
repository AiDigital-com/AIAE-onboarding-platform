package com.aidigital.aionboarding.service.common.security;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Holds the resolved {@link AppUser} and effective permission map for the currently
 * processing HTTP request, so authorization checks and business logic within one request
 * reuse a single user resolution and a single permission-map load instead of repeating
 * both on every {@code @PreAuthorize} evaluation and every {@code requireUser()}/permission
 * check.
 * <p>
 * Backed by {@link ThreadLocal}s since one HTTP request is served by one thread in this
 * application's servlet model. A request-boundary filter (in the {@code application}
 * module, alongside {@code CorrelationIdFilter}) MUST call {@link #clear()} in a
 * {@code finally} block so nothing leaks into the next request served by the same pooled
 * thread. Never shared/global across requests or principals.
 */
@Component
public class RequestAuthenticationCache {

	private final ThreadLocal<AppUser> userHolder = new ThreadLocal<>();
	private final ThreadLocal<Long> permissionMapUserIdHolder = new ThreadLocal<>();
	private final ThreadLocal<Map<String, Boolean>> permissionMapHolder = new ThreadLocal<>();

	/**
	 * Returns the {@link AppUser} resolved earlier in this request, if any.
	 *
	 * @return the cached user, or empty if not yet resolved this request
	 */
	public Optional<AppUser> getUser() {
		return Optional.ofNullable(userHolder.get());
	}

	/**
	 * Stores the resolved {@link AppUser} for reuse for the remainder of this request.
	 *
	 * @param user the resolved user
	 */
	public void putUser(AppUser user) {
		userHolder.set(user);
	}

	/**
	 * Returns the effective permission map computed earlier in this request for the given
	 * user, if any. Guarded by user id so a permission check for a different user (should
	 * one ever occur) is a cache miss rather than stale data for the wrong principal.
	 *
	 * @param userId internal id of the user the caller wants a permission map for
	 * @return the cached permission map, or empty if not cached (or cached for a different user)
	 */
	public Optional<Map<String, Boolean>> getPermissionMap(Long userId) {
		if (userId == null || !userId.equals(permissionMapUserIdHolder.get())) {
			return Optional.empty();
		}
		return Optional.ofNullable(permissionMapHolder.get());
	}

	/**
	 * Stores the effective permission map for the given user for reuse for the remainder
	 * of this request.
	 *
	 * @param userId        internal id of the user the map belongs to
	 * @param permissionMap the computed permission map
	 */
	public void putPermissionMap(Long userId, Map<String, Boolean> permissionMap) {
		permissionMapUserIdHolder.set(userId);
		permissionMapHolder.set(permissionMap);
	}

	/**
	 * Clears both caches. Must be called at the end of every request to prevent leaking
	 * into the next request handled by the same pooled thread.
	 */
	public void clear() {
		userHolder.remove();
		permissionMapUserIdHolder.remove();
		permissionMapHolder.remove();
	}
}
