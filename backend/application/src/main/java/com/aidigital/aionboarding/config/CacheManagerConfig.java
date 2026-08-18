package com.aidigital.aionboarding.config;

import java.net.URI;
import java.net.URISyntaxException;
import javax.cache.Caching;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.cache.CacheManager;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes Hibernate's existing JCache manager through Spring's cache abstraction so the
 * {@code cache-management} module's generic {@code CacheService}/{@code CacheUpdaterService}
 * (which resolve regions via {@link org.springframework.cache.CacheManager}, not
 * {@code javax.cache}) can reach the Hibernate L2/query regions declared in {@code ehcache.xml}.
 *
 * <p><b>Deliberately does not add {@code @EnableCaching}.</b> See
 * {@code docs/migration-guardrails.md}: {@code spring.cache.type: jcache} is already declared in
 * {@code application.yml}, so {@code @EnableCaching} would activate Spring Boot's
 * {@code JCacheCacheConfiguration}, which — being {@code @ConditionalOnMissingBean(CacheManager
 * .class)} — would back off the moment it saw the bean below; but nothing here needs the
 * annotation-driven {@code @Cacheable}/{@code @CacheEvict} aspect it exists to enable, and adding
 * an unused import of that risk is not worth it. This class wires the manager directly instead.
 *
 * <p><b>There is still only one {@code javax.cache.CacheManager}, not two.</b> Rather than have
 * Spring build a second manager from the same {@code ehcache.xml} URI and rely on the JSR-107
 * provider de-duplicating same-URI managers, {@link #sharedJCacheManagerCustomizer} forces
 * Hibernate to reuse the exact instance this class creates: {@code JCacheRegionFactory
 * .resolveCacheManager} (hibernate-jcache) reads {@code hibernate.javax.cache.cache_manager} from
 * the Hibernate properties map first, and if that value is already a {@code javax.cache
 * .CacheManager} instance, returns it verbatim instead of resolving one itself. Verified by
 * {@code CacheInvalidationOutboxIntegrationTest}, which asserts object identity between this
 * bean and the manager Hibernate ends up using.
 */
@Configuration
public class CacheManagerConfig {

	/**
	 * Creates the single JSR-107 cache manager shared by Hibernate and Spring, resolved from the
	 * same {@code ehcache.xml} classpath resource Hibernate is configured with
	 * ({@code hibernate.javax.cache.uri} in {@code application.yml}).
	 *
	 * @return the ehcache.xml-configured manager
	 */
	@Bean(destroyMethod = "close")
	javax.cache.CacheManager jCacheManager() {
		var resource = getClass().getClassLoader().getResource("ehcache.xml");
		if (resource == null) {
			throw new IllegalStateException("ehcache.xml is missing from the application classpath");
		}
		try {
			URI configUri = resource.toURI();
			return Caching.getCachingProvider().getCacheManager(configUri, getClass().getClassLoader());
		} catch (URISyntaxException exception) {
			throw new IllegalStateException("Cannot resolve ehcache.xml", exception);
		}
	}

	/**
	 * Exposes the shared JCache manager through Spring's cache abstraction, for
	 * {@code cache-management}'s {@code CacheService} to resolve regions by name.
	 *
	 * @param jCacheManager shared JSR-107 manager
	 * @return Spring cache manager backed by the same Ehcache regions
	 */
	@Bean
	CacheManager cacheManager(javax.cache.CacheManager jCacheManager) {
		return new JCacheCacheManager(jCacheManager);
	}

	/**
	 * Forces Hibernate L2/query caching to reuse the same JCache manager exposed to Spring, so a
	 * region cleared through Spring's {@code CacheManager} and a region read through Hibernate are
	 * the same underlying Ehcache region.
	 *
	 * @param jCacheManager shared JSR-107 manager
	 * @return Hibernate property customizer
	 */
	@Bean
	HibernatePropertiesCustomizer sharedJCacheManagerCustomizer(javax.cache.CacheManager jCacheManager) {
		return properties -> properties.put("hibernate.javax.cache.cache_manager", jCacheManager);
	}
}
