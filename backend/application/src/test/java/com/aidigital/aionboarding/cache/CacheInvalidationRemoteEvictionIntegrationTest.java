package com.aidigital.aionboarding.cache;

import com.aidigital.aionboarding.cachemanagement.updater.CacheUpdaterService;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.UserRoleRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the mechanism {@code .claude/agent_docs/distributed_cache.md} requires:
 * {@code CacheUpdaterService.clearCache(...)} — the same call {@code ScheduledCacheUpdater} makes
 * for a "remote" invalidation event — causes the next dictionary read to miss the Hibernate query
 * cache and hit the database, exactly as if another node had mutated the row.
 *
 * <p>Runs the real Liquibase migrations (default profile, real Postgres via Testcontainers,
 * unlike {@link CacheInvalidationOutboxIntegrationTest}'s H2 slice) so the dictionary rows
 * {@code findByCode} resolves are the actual Liquibase-seeded data, not test fixtures — the same
 * setup {@code DictionaryCacheIntegrationTest} uses to prove the cache is populated in the first
 * place. Full {@code @SpringBootTest} (not {@code @DataJpaTest}) because
 * {@code CacheUpdaterService} is a regular {@code @Service} bean the JPA test slice excludes.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class CacheInvalidationRemoteEvictionIntegrationTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

	@DynamicPropertySource
	static void enableHibernateStatistics(DynamicPropertyRegistry registry) {
		registry.add("spring.jpa.properties.hibernate.generate_statistics", () -> "true");
	}

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Autowired
	private UserRoleRepository userRoleRepository;

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private CacheUpdaterService cacheUpdaterService;

	@Test
	void remoteEvictionCausesTheNextReadToHitTheDatabaseTest() {
		// Given: warm the query cache exactly like DictionaryCacheIntegrationTest
		Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
		statistics.clear();
		userRoleRepository.findByCode("admin");
		long putsAfterWarmUp = statistics.getQueryCachePutCount();
		userRoleRepository.findByCode("admin");
		assertThat(statistics.getQueryCacheHitCount())
				.as("second lookup before any invalidation should be served from cache")
				.isGreaterThan(0);
		assertThat(statistics.getQueryCachePutCount())
				.as("second lookup before any invalidation should not re-populate the cache")
				.isEqualTo(putsAfterWarmUp);

		// When: clear the region by name — the exact call ScheduledCacheUpdater makes for an
		// event whose trackedClass resolves (via ApplicationCacheNamesByClassRegistry, once a
		// mutable source is registered) to this region name.
		cacheUpdaterService.clearCache("hibernate-cache.findUserRoleByCode");

		// Then: the next read is a real miss — the region was actually cleared, not left stale
		userRoleRepository.findByCode("admin");
		assertThat(statistics.getQueryCachePutCount())
				.as("read after remote eviction must repopulate the cache from the database")
				.isGreaterThan(putsAfterWarmUp);
	}
}
