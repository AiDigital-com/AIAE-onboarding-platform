package com.aidigital.aionboarding.service.common.cache.services.entity;

import com.aidigital.aionboarding.domain.cache.entities.CacheInvalidationEventEntity;
import com.aidigital.aionboarding.domain.cache.repositories.CacheInvalidationEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CacheInvalidationEventEntityService}.
 */
@ExtendWith(MockitoExtension.class)
class CacheInvalidationEventEntityServiceTest {

	@Mock
	private CacheInvalidationEventRepository repository;

	@InjectMocks
	private CacheInvalidationEventEntityService service;

	@Test
	void shouldReadABoundedPageAfterTheGivenSequenceTest() {
		// Given:
		CacheInvalidationEventEntity entity =
				new CacheInvalidationEventEntity("example.Team", Instant.parse("2026-07-27T10:00:00Z"));
		when(repository.findByIdGreaterThanOrderByIdAsc(10L, PageRequest.of(0, 50)))
				.thenReturn(List.of(entity));

		// When:
		List<CacheInvalidationEventEntity> result = service.findAfter(10L, 50);

		// Then:
		assertThat(result).containsExactly(entity);
	}

	@Test
	void shouldSaveTheGivenOutboxRowTest() {
		// Given:
		CacheInvalidationEventEntity entity = new CacheInvalidationEventEntity("example.Team", Instant.now());
		when(repository.save(entity)).thenReturn(entity);

		// When:
		CacheInvalidationEventEntity saved = service.save(entity);

		// Then:
		assertThat(saved).isSameAs(entity);
	}

	@Test
	void shouldDeleteRowsOlderThanTheCutoffTest() {
		// Given:
		Instant cutoff = Instant.parse("2026-07-01T00:00:00Z");
		when(repository.deleteCreatedBefore(cutoff)).thenReturn(3);

		// When:
		int deleted = service.deleteCreatedBefore(cutoff);

		// Then:
		assertThat(deleted).isEqualTo(3);
		verify(repository).deleteCreatedBefore(cutoff);
	}
}
