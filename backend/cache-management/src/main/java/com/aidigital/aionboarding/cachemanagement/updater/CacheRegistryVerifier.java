package com.aidigital.aionboarding.cachemanagement.updater;

import com.aidigital.aionboarding.cachemanagement.cache.CacheService;
import com.aidigital.aionboarding.cachemanagement.config.CacheManagementProperties;
import com.aidigital.aionboarding.cachemanagement.registry.CacheNamesByClassService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Optional startup check that every cache region named in the
 * {@link com.aidigital.aionboarding.cachemanagement.registry.CacheNamesByClassRegistry} actually
 * exists in some cache manager — catching registry typos and stale region names. Disabled by
 * default (regions may be created lazily); enable via
 * {@code app.cache-management.verify-registry=true} in non-production environments.
 */
@Component
@RequiredArgsConstructor
public class CacheRegistryVerifier {

	private final CacheService cacheService;
	private final CacheNamesByClassService cacheNamesByClassService;
	private final CacheManagementProperties properties;

	/**
	 * Verifies that each registered cache name resolves to at least one cache region.
	 *
	 * @throws IllegalStateException when a registered region is missing from every cache manager
	 */
	@PostConstruct
	public void verify() {
		if (!properties.isVerifyRegistry()) {
			return;
		}
		for (String cacheName : cacheNamesByClassService.getAllCacheNames()) {
			if (cacheService.getCachesByName(cacheName).isEmpty()) {
				throw new IllegalStateException("Cache region '" + cacheName
						+ "' is registered in CacheNamesByClassRegistry but not found in any cache manager");
			}
		}
	}
}
