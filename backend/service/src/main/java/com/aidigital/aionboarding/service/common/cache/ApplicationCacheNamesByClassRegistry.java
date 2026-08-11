package com.aidigital.aionboarding.service.common.cache;

import com.aidigital.aionboarding.cachemanagement.registry.CacheNamesByClassRegistry;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Application-owned mapping from mutation sources to every affected Hibernate L2, query-cache,
 * and Spring cache region.
 *
 * <p>Empty by design (see {@code .claude/agent_docs/distributed_cache.md} and the migration plan's
 * P6 record): all 8 cached sources today — {@code UserRole}, {@code LessonStatus},
 * {@code LessonPublicationStatus}, {@code LessonContentFormat}, {@code LessonAssetKind},
 * {@code MaterialFileKind}, {@code ActivityType}, {@code ActivityProgressStatus} — are
 * {@code READ_ONLY} {@code @Immutable} dictionary entities changed only by Liquibase at deploy
 * time, so no application transaction ever mutates them and registering them would add publish
 * calls no code path makes. This registry becomes load-bearing the moment a mutable source is
 * cached: add its class here, mapped to every affected {@code ehcache.xml} region name, in the
 * same change that adds {@code CacheInvalidationEventService.publishUpdateEvent(Source.class)} to
 * its mutation.
 */
@Component
public class ApplicationCacheNamesByClassRegistry implements CacheNamesByClassRegistry {

	@Override
	public Map<Class<?>, List<String>> cacheNamesByClassMap() {
		return Map.of();
	}
}
