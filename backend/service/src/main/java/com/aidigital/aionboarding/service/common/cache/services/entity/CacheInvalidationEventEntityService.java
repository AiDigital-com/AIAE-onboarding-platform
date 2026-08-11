package com.aidigital.aionboarding.service.common.cache.services.entity;

import com.aidigital.aionboarding.domain.cache.entities.CacheInvalidationEventEntity;
import com.aidigital.aionboarding.domain.cache.repositories.CacheInvalidationEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Short-transaction CRUD helpers for the {@link CacheInvalidationEventEntity} entity.
 * <p>
 * This is the only service that may inject {@link CacheInvalidationEventRepository} directly. All
 * other services that need cache-invalidation outbox rows must depend on this service.
 */
@Service
@RequiredArgsConstructor
public class CacheInvalidationEventEntityService {

	private final CacheInvalidationEventRepository repository;

	/**
	 * Reads one ordered page of outbox rows after the caller's last processed sequence.
	 *
	 * @param sequence exclusive lower event-ID bound
	 * @param limit maximum number of events to return
	 * @return matching events ordered by increasing ID
	 */
	@Transactional(readOnly = true)
	public List<CacheInvalidationEventEntity> findAfter(long sequence, int limit) {
		return repository.findByIdGreaterThanOrderByIdAsc(sequence, PageRequest.of(0, limit));
	}

	/**
	 * Persists a new outbox row. {@link Propagation#MANDATORY} is enforced here too — not only on
	 * the calling {@code JpaCacheInvalidationEventService.publishUpdateEvent} — so a future direct
	 * caller of this entity service cannot silently open its own transaction and defeat the
	 * mutation/invalidation atomicity guarantee.
	 *
	 * @param event the outbox row to persist
	 * @return the saved {@link CacheInvalidationEventEntity}
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public CacheInvalidationEventEntity save(CacheInvalidationEventEntity event) {
		return repository.save(event);
	}

	/**
	 * Deletes outbox rows older than the retention cutoff.
	 *
	 * @param cutoff exclusive creation-time upper bound
	 * @return number of deleted rows
	 */
	@Transactional
	public int deleteCreatedBefore(Instant cutoff) {
		return repository.deleteCreatedBefore(cutoff);
	}
}
