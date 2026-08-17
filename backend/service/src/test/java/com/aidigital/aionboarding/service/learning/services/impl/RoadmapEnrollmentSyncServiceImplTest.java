package com.aidigital.aionboarding.service.learning.services.impl;

import com.aidigital.aionboarding.service.learning.models.CompletedRoadmapRecord;
import com.aidigital.aionboarding.service.learning.services.entity.LearningEnrollmentEntityService;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoadmapEnrollmentSyncServiceImplTest {

	@Mock
	private LearningEnrollmentEntityService learningEnrollmentEntityService;

	@InjectMocks
	private RoadmapEnrollmentSyncServiceImpl service;

	@Test
	void shouldReturnCompletedRoadmapsFromEntityServiceTest() {
		// Given:
		CompletedRoadmapRecord first = Instancio.create(CompletedRoadmapRecord.class);
		CompletedRoadmapRecord second = Instancio.create(CompletedRoadmapRecord.class);
		when(learningEnrollmentEntityService.findCompletedRoadmapsForUserLesson(10L, 100L))
				.thenReturn(List.of(first, second));

		// When:
		List<CompletedRoadmapRecord> result = service.getCompletedRoadmapsForUserLesson(10L, 100L);

		// Then:
		assertThat(result).containsExactly(first, second);
		verify(learningEnrollmentEntityService).findCompletedRoadmapsForUserLesson(eq(10L), eq(100L));
	}

	@Test
	void shouldReturnEmptyListWhenNoRoadmapsCompletedForUserLessonTest() {
		// Given:
		when(learningEnrollmentEntityService.findCompletedRoadmapsForUserLesson(11L, 101L))
				.thenReturn(List.of());

		// When:
		List<CompletedRoadmapRecord> result = service.getCompletedRoadmapsForUserLesson(11L, 101L);

		// Then:
		assertThat(result).isEmpty();
		verify(learningEnrollmentEntityService).findCompletedRoadmapsForUserLesson(eq(11L), eq(101L));
	}

	@Test
	void shouldDelegateDistinctUserAndLessonIdsWithoutMixingThemUpTest() {
		// Given:
		CompletedRoadmapRecord recordForOtherPair = Instancio.create(CompletedRoadmapRecord.class);
		when(learningEnrollmentEntityService.findCompletedRoadmapsForUserLesson(20L, 200L))
				.thenReturn(List.of(recordForOtherPair));
		when(learningEnrollmentEntityService.findCompletedRoadmapsForUserLesson(21L, 200L))
				.thenReturn(List.of());

		// When:
		List<CompletedRoadmapRecord> resultForUser20 = service.getCompletedRoadmapsForUserLesson(20L, 200L);
		List<CompletedRoadmapRecord> resultForUser21 = service.getCompletedRoadmapsForUserLesson(21L, 200L);

		// Then:
		assertThat(resultForUser20).containsExactly(recordForOtherPair);
		assertThat(resultForUser21).isEmpty();
		verify(learningEnrollmentEntityService).findCompletedRoadmapsForUserLesson(eq(20L), eq(200L));
		verify(learningEnrollmentEntityService).findCompletedRoadmapsForUserLesson(eq(21L), eq(200L));
	}
}
