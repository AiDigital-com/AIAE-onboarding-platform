package com.aidigital.aionboarding.service.common.cache;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test pinning the M2 design decision: the registry starts empty because every currently
 * cached source is {@code READ_ONLY} and Liquibase-seeded (see the class JavaDoc and
 * {@code .claude/agent_docs/distributed_cache.md}).
 */
class ApplicationCacheNamesByClassRegistryTest {

	@Test
	void shouldStartWithNoRegisteredMutationSourcesTest() {
		// Given:
		ApplicationCacheNamesByClassRegistry registry = new ApplicationCacheNamesByClassRegistry();

		// When-Then:
		assertThat(registry.cacheNamesByClassMap()).isEmpty();
	}
}
