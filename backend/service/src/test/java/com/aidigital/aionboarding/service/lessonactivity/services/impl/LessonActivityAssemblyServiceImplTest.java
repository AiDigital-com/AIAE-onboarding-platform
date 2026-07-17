package com.aidigital.aionboarding.service.lessonactivity.services.impl;

import com.aidigital.aionboarding.domain.common.dictionary.ActivityTypeCode;
import com.aidigital.aionboarding.domain.lessonactivity.repositories.LessonActivityTypeCountProjection;
import com.aidigital.aionboarding.service.lessonactivity.models.LessonActivityCountsRecord;
import com.aidigital.aionboarding.service.lessonactivity.support.LessonActivityProgressPersistence;
import com.aidigital.aionboarding.service.lessonactivity.support.LessonActivityRecordAssembler;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonActivityAssemblyServiceImplTest {

	@Mock
	private LessonActivityPersistenceHelper lessonActivityPersistenceHelper;
	@Mock
	private LessonActivityProgressPersistence progressPersistence;
	@Mock
	private LessonActivityRecordAssembler lessonActivityMapper;

	@InjectMocks
	private LessonActivityAssemblyServiceImpl service;

	@Nested
	class CountActivitiesByLessonIds {

		@Test
		void shouldReturnEmptyMapWhenLessonIdsEmptyTest() {
			// When:
			Map<Long, LessonActivityCountsRecord> result = service.countActivitiesByLessonIds(List.of());

			// Then:
			assertThat(result).isEmpty();
		}

		@Test
		void shouldGroupCountsByLessonAndDefaultMissingTypeToZeroTest() {
			// Given: lesson 1 has 2 flashcard sets and 1 quiz; lesson 2 has 1 quiz only
			List<Long> lessonIds = List.of(1L, 2L);
			when(lessonActivityPersistenceHelper.countByLessonIdsGroupedByType(lessonIds)).thenReturn(List.of(
					countRow(1L, ActivityTypeCode.FLASHCARDS, 2L),
					countRow(1L, ActivityTypeCode.QUIZ, 1L),
					countRow(2L, ActivityTypeCode.QUIZ, 1L)
			));

			// When:
			Map<Long, LessonActivityCountsRecord> result = service.countActivitiesByLessonIds(lessonIds);

			// Then:
			assertThat(result).containsEntry(1L, new LessonActivityCountsRecord(2, 1));
			assertThat(result).containsEntry(2L, new LessonActivityCountsRecord(0, 1));
		}

		private LessonActivityTypeCountProjection countRow(Long lessonId, String typeCode, long count) {
			return new LessonActivityTypeCountProjection() {
				@Override
				public Long getLessonId() {
					return lessonId;
				}

				@Override
				public String getTypeCode() {
					return typeCode;
				}

				@Override
				public long getActivityCount() {
					return count;
				}
			};
		}
	}
}
