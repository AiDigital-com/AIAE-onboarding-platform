package com.aidigital.aionboarding.domain.cache.entities;

import com.aidigital.aionboarding.domain.common.entities.IdAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Durable outbox row announcing that data feeding one or more node-local cache regions changed on
 * some node. The generated {@code id} is the poller's monotonic ordering cursor — see
 * {@code .claude/agent_docs/distributed_cache.md}.
 */
@Entity
@Table(name = "cache_invalidation_event")
@Getter
@Setter
@NoArgsConstructor
public class CacheInvalidationEventEntity extends IdAwareEntity {

	@Column(name = "tracked_class", nullable = false)
	private String trackedClass;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	/**
	 * Creates a new outbox row for the given tracked class.
	 *
	 * @param trackedClass fully qualified name of the class that changed
	 * @param createdAt when the event was assembled
	 */
	public CacheInvalidationEventEntity(String trackedClass, Instant createdAt) {
		this.trackedClass = trackedClass;
		this.createdAt = createdAt;
	}
}
