package com.aidigital.aionboarding.domain.cache.entities;

import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CacheInvalidationEventEntityTest {

	@Test
	void constructorShouldSetTrackedClassAndCreatedAtTest() {
		// Given
		Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");

		// When
		CacheInvalidationEventEntity event = new CacheInvalidationEventEntity("com.example.Tracked", createdAt);

		// Then
		assertThat(event.getTrackedClass()).isEqualTo("com.example.Tracked");
		assertThat(event.getCreatedAt()).isEqualTo(createdAt);
	}
}
