package com.aidigital.aionboarding.cache;

import com.aidigital.aionboarding.domain.common.dictionary.repositories.UserRoleRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link UserRoleRepository#findByCode(String)} — annotated with
 * {@code @QueryHints(HINT_CACHEABLE)} — is actually served from Hibernate's L2/query cache
 * (backed by Ehcache via JCache) on repeated lookups, rather than only proving the cache regions
 * boot without a {@code missing_cache_strategy=fail} error.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class DictionaryCacheIntegrationTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

	@DynamicPropertySource
	static void enableHibernateStatistics(DynamicPropertyRegistry registry) {
		registry.add("spring.jpa.properties.hibernate.generate_statistics", () -> "true");
	}

	@Autowired
	private UserRoleRepository userRoleRepository;

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Test
	void findByCodeShouldBeServedFromCacheOnRepeatedLookupsTest() {
		// Given:
		Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
		statistics.clear();

		// When:
		userRoleRepository.findByCode("admin");
		userRoleRepository.findByCode("admin");
		userRoleRepository.findByCode("admin");

		// Then:
		assertThat(statistics.getQueryCacheHitCount())
				.as("repeated findByCode lookups should hit the Hibernate query cache")
				.isGreaterThan(0);
	}
}
