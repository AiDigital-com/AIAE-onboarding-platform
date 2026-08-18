package com.aidigital.aionboarding.cache;

import com.aidigital.aionboarding.cachemanagement.event.CacheInvalidationEventService;
import com.aidigital.aionboarding.cachemanagement.registry.CacheNamesByClassRegistry;
import com.aidigital.aionboarding.cachemanagement.registry.CacheNamesByClassServiceImpl;
import com.aidigital.aionboarding.cachemanagement.cache.CacheServiceImpl;
import com.aidigital.aionboarding.cachemanagement.updater.CacheRegistryVerifier;
import com.aidigital.aionboarding.domain.cache.repositories.CacheInvalidationEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins the transactional outbox and shared-cache-manager correctness contract described in
 * {@code .claude/agent_docs/distributed_cache.md}. Runs against H2 ({@code application-test.yml},
 * {@code ddl-auto: create-drop}) — no Testcontainers/Docker needed for these assertions, unlike
 * the Postgres-backed L2-eviction proof in {@code DictionaryCacheIntegrationTest}.
 */
@SpringBootTest
@ActiveProfiles("test")
class CacheInvalidationOutboxIntegrationTest {

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Autowired
	private CacheInvalidationEventService eventService;

	@Autowired
	private CacheInvalidationEventRepository repository;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private javax.cache.CacheManager jCacheManager;

	@Autowired
	private org.springframework.cache.CacheManager springCacheManager;

	@Autowired
	@Qualifier("sharedJCacheManagerCustomizer")
	private HibernatePropertiesCustomizer hibernateCustomizer;

	@Autowired
	private CacheNamesByClassRegistry cacheNamesByClassRegistry;

	@Autowired
	private CacheServiceImpl cacheService;

	@BeforeEach
	void clearEvents() {
		repository.deleteAll();
	}

	@Test
	void shouldCommitMutationAndInvalidationAtomicallyTest() {
		// Given:
		TransactionTemplate transaction = new TransactionTemplate(transactionManager);

		// When:
		transaction.executeWithoutResult(status -> eventService.publishUpdateEvent("example.Team"));

		// Then:
		assertThat(repository.count()).isEqualTo(1L);
	}

	@Test
	void shouldRollbackInvalidationWithMutationTransactionTest() {
		// Given:
		TransactionTemplate transaction = new TransactionTemplate(transactionManager);

		// When:
		transaction.executeWithoutResult(status -> {
			eventService.publishUpdateEvent("example.Team");
			status.setRollbackOnly();
		});

		// Then:
		assertThat(repository.count()).isZero();
	}

	@Test
	void shouldRejectPublicationOutsideMutationTransactionTest() {
		// When-Then:
		assertThatThrownBy(() -> eventService.publishUpdateEvent("example.Team"))
				.isInstanceOf(org.springframework.transaction.IllegalTransactionStateException.class);
	}

	@Test
	void shouldShareTheExactJCacheManagerWithSpringAndHibernateTest() {
		// Then:
		assertThat(springCacheManager).isInstanceOf(JCacheCacheManager.class);
		assertThat(((JCacheCacheManager) springCacheManager).getCacheManager()).isSameAs(jCacheManager);

		Map<String, Object> hibernateProperties = new HashMap<>();
		hibernateCustomizer.customize(hibernateProperties);
		assertThat(hibernateProperties.get("hibernate.javax.cache.cache_manager")).isSameAs(jCacheManager);
	}

	@Test
	void shouldExposeHibernateRegionsThroughTheSpringCacheAbstractionTest() {
		// The dictionary entity region is declared in ehcache.xml and is reachable by name
		// through the same manager Hibernate reads/writes, proving CacheServiceImpl can resolve
		// (and, via CacheUpdaterServiceImpl, clear) it once a mutable source registers it.
		assertThat(cacheService.getCachesByName(
				"hibernate-cache.com.aidigital.aionboarding.domain.common.dictionary.entities.UserRole"))
				.isNotEmpty();
	}

	@Test
	void shouldStartWithAnEmptyApplicationRegistryTest() {
		// Given: M2 — no mutable cached source exists yet, see ApplicationCacheNamesByClassRegistry
		assertThat(cacheNamesByClassRegistry.cacheNamesByClassMap()).isEmpty();
	}

	@Test
	void shouldPassRegistryVerificationAgainstTheRealEmptyRegistryTest() {
		// Given: verify-registry is opt-in (see CacheRegistryVerifier); flip it for this
		// assertion against the real, wired CacheNamesByClassServiceImpl and CacheService beans.
		CacheNamesByClassServiceImpl realService = new CacheNamesByClassServiceImpl(cacheNamesByClassRegistry);
		CacheRegistryVerifier verifierAgainstRealBeans =
				new CacheRegistryVerifier(cacheService, realService, verifyRegistryEnabled());

		// When-Then: an empty registry has nothing to resolve, so verification is a no-op
		assertThatCode(verifierAgainstRealBeans::verify).doesNotThrowAnyException();
	}

	private com.aidigital.aionboarding.cachemanagement.config.CacheManagementProperties verifyRegistryEnabled() {
		com.aidigital.aionboarding.cachemanagement.config.CacheManagementProperties properties =
				new com.aidigital.aionboarding.cachemanagement.config.CacheManagementProperties();
		properties.setVerifyRegistry(true);
		return properties;
	}
}
