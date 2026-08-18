package com.aidigital.aionboarding.service.common.cache;

import com.aidigital.aionboarding.cachemanagement.config.CacheManagementProperties;
import com.aidigital.aionboarding.cachemanagement.event.CacheInvalidationEvent;
import com.aidigital.aionboarding.domain.cache.entities.CacheInvalidationEventEntity;
import com.aidigital.aionboarding.service.common.cache.services.entity.CacheInvalidationEventEntityService;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the JPA cache-invalidation outbox adapter.
 */
@ExtendWith(MockitoExtension.class)
class JpaCacheInvalidationEventServiceTest {

	@Mock
	private CacheInvalidationEventEntityService cacheInvalidationEventEntityService;

	@Mock
	private CurrentTime currentTime;

	@Test
	void shouldReadASequenceOrderedBoundedPageTest() {
		// Given:
		Instant createdAt = Instant.parse("2026-07-27T10:00:00Z");
		CacheInvalidationEventEntity entity = new CacheInvalidationEventEntity("example.Team", createdAt);
		entity.setId(12L);
		when(cacheInvalidationEventEntityService.findAfter(10L, 100)).thenReturn(List.of(entity));
		JpaCacheInvalidationEventService service = service();

		// When:
		List<CacheInvalidationEvent> events = service.updatesAfter(10L, 100);

		// Then:
		assertThat(events).containsExactly(new CacheInvalidationEvent(12L, "example.Team", createdAt));
	}

	@Test
	void shouldPersistFullyQualifiedInvalidationSourceTest() {
		// Given:
		Instant now = Instant.parse("2026-07-27T10:00:00Z");
		when(currentTime.instant()).thenReturn(now);
		JpaCacheInvalidationEventService service = service();

		// When:
		service.publishUpdateEvent("example.Team");

		// Then:
		ArgumentCaptor<CacheInvalidationEventEntity> captor = ArgumentCaptor.forClass(CacheInvalidationEventEntity.class);
		verify(cacheInvalidationEventEntityService).save(captor.capture());
		assertThat(captor.getValue().getTrackedClass()).isEqualTo("example.Team");
		assertThat(captor.getValue().getCreatedAt()).isEqualTo(now);
	}

	@Test
	void shouldRejectInvalidArgumentsTest() {
		// Given:
		JpaCacheInvalidationEventService service = service();

		// When-Then:
		assertThatThrownBy(() -> service.updatesAfter(0L, 0))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> service.publishUpdateEvent(" "))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void shouldNotPruneEventsByDefaultTest() {
		// Given:
		JpaCacheInvalidationEventService service = service();

		// When:
		service.cleanupOldEvents();

		// Then:
		verifyNoInteractions(cacheInvalidationEventEntityService, currentTime);
	}

	@Test
	void shouldPruneEventsOnlyWhenExplicitlyEnabledTest() {
		// Given:
		Instant now = Instant.parse("2026-07-27T10:00:00Z");
		when(currentTime.instant()).thenReturn(now);
		CacheManagementProperties properties = new CacheManagementProperties();
		properties.setCleanupEnabled(true);
		JpaCacheInvalidationEventService service =
				new JpaCacheInvalidationEventService(cacheInvalidationEventEntityService, properties, currentTime);

		// When:
		service.cleanupOldEvents();

		// Then:
		verify(cacheInvalidationEventEntityService).deleteCreatedBefore(now.minus(properties.getRetention()));
	}

	private JpaCacheInvalidationEventService service() {
		return new JpaCacheInvalidationEventService(
				cacheInvalidationEventEntityService, new CacheManagementProperties(), currentTime);
	}
}
