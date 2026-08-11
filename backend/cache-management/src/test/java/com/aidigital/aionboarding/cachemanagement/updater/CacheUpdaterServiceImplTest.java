package com.aidigital.aionboarding.cachemanagement.updater;

import com.aidigital.aionboarding.cachemanagement.cache.CacheService;
import com.aidigital.aionboarding.cachemanagement.registry.CacheNamesByClassService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CacheUpdaterServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class CacheUpdaterServiceImplTest {

	@Mock
	private CacheService cacheService;

	@Mock
	private CacheNamesByClassService cacheNamesByClassService;

	@InjectMocks
	private CacheUpdaterServiceImpl service;

	@Test
	void shouldClearEveryRegisteredRegionForClassTest() {
		// Given:
		Cache cacheOne = Mockito.mock(Cache.class);
		Cache cacheTwo = Mockito.mock(Cache.class);
		when(cacheNamesByClassService.getCacheNamesByClassName("example.HubTeam")).thenReturn(List.of("c1", "c2"));
		when(cacheService.getCachesByName("c1")).thenReturn(List.of(cacheOne));
		when(cacheService.getCachesByName("c2")).thenReturn(List.of(cacheTwo));

		// When:
		service.clearCachesForClass("example.HubTeam");

		// Then:
		verify(cacheOne).clear();
		verify(cacheTwo).clear();
	}

	@Test
	void shouldFailWhenClassHasNoRegisteredRegionsTest() {
		// Given:
		when(cacheNamesByClassService.getCacheNamesByClassName("example.Unregistered")).thenReturn(List.of());

		// When-Then:
		assertThatThrownBy(() -> service.clearCachesForClass("example.Unregistered"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("No cache regions are registered");
	}

	@Test
	void shouldFailWhenRegionIsNotFoundTest() {
		// Given: the region resolves to no cache in any manager
		when(cacheService.getCachesByName("missing")).thenReturn(List.of());

		// When-Then: the event must remain retryable instead of being silently acknowledged
		assertThatThrownBy(() -> service.clearCache("missing"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("not found in any cache manager");
	}
}
