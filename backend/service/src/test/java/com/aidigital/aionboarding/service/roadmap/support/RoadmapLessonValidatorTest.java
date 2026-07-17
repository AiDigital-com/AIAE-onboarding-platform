package com.aidigital.aionboarding.service.roadmap.support;

import com.aidigital.aionboarding.domain.common.dictionary.LessonPublicationStatusCode;
import com.aidigital.aionboarding.domain.common.dictionary.LessonStatusCode;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonPublicationStatus;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonStatus;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonEntityService;
import com.aidigital.aionboarding.service.lesson.util.LessonTagUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoadmapLessonValidatorTest {

	@Mock
	private LessonEntityService lessonEntityService;
	@Mock
	private LessonTagUtil lessonTagUtil;

	@InjectMocks
	private RoadmapLessonValidator validator;

	@Test
	void shouldNormalizeNullLessonIdsToEmptyListTest() {
		// When:
		List<Long> result = validator.normalizeLessonIds(null);

		// Then:
		assertThat(result).isEmpty();
	}

	@Test
	void shouldNormalizeLessonIdsDroppingNullsAndDuplicatesTest() {
		// Given:
		List<Long> lessonIds = java.util.Arrays.asList(1L, null, 2L, 1L);

		// When:
		List<Long> result = validator.normalizeLessonIds(lessonIds);

		// Then:
		assertThat(result).containsExactly(1L, 2L);
	}

	@Test
	void shouldValidateReadyPublishedLessonsInRequestedOrderTest() {
		// Given:
		Lesson lessonOne = readyPublishedLesson(1L);
		Lesson lessonTwo = readyPublishedLesson(2L);
		when(lessonEntityService.findAllById(eq(List.of(1L, 2L)))).thenReturn(List.of(lessonOne, lessonTwo));

		// When:
		List<Lesson> result = validator.validateReadyPublishedLessons(List.of(1L, 2L));

		// Then:
		assertThat(result).extracting(Lesson::getId).containsExactly(1L, 2L);
	}

	@Test
	void shouldRejectEmptyLessonListTest() {
		// When-Then:
		assertThatThrownBy(() -> validator.validateReadyPublishedLessons(List.of()))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("Select at least one lesson");
	}

	@Test
	void shouldRejectWhenLessonCountDoesNotMatchIdsTest() {
		// Given:
		when(lessonEntityService.findAllById(eq(List.of(1L, 2L)))).thenReturn(List.of(readyPublishedLesson(1L)));

		// When-Then:
		assertThatThrownBy(() -> validator.validateReadyPublishedLessons(List.of(1L, 2L)))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("Roadmaps can include only existing published ready lessons");
	}

	@Test
	void shouldRejectWhenLessonIsNotReadyTest() {
		// Given:
		Lesson lesson = lessonWithStatus(1L, LessonStatusCode.DRAFT, LessonPublicationStatusCode.PUBLISHED);
		when(lessonEntityService.findAllById(eq(List.of(1L)))).thenReturn(List.of(lesson));

		// When-Then:
		assertThatThrownBy(() -> validator.validateReadyPublishedLessons(List.of(1L)))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("Roadmaps can include only existing published ready lessons");
	}

	@Test
	void shouldRejectWhenLessonIsNotPublishedTest() {
		// Given:
		Lesson lesson = lessonWithStatus(1L, LessonStatusCode.READY, LessonPublicationStatusCode.PRIVATE);
		when(lessonEntityService.findAllById(eq(List.of(1L)))).thenReturn(List.of(lesson));

		// When-Then:
		assertThatThrownBy(() -> validator.validateReadyPublishedLessons(List.of(1L)))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("Roadmaps can include only existing published ready lessons");
	}

	@Test
	void shouldMergeInputTagsWithLessonTagsTest() {
		// Given:
		Lesson lesson = readyPublishedLesson(1L);
		lesson.setTags(List.of("lesson-tag"));
		when(lessonTagUtil.normalizeLessonTagInput(eq(List.of("input-tag", "lesson-tag"))))
				.thenReturn(List.of("input-tag", "lesson-tag"));

		// When:
		List<String> result = validator.mergeTags(List.of("input-tag"), List.of(lesson));

		// Then:
		assertThat(result).containsExactly("input-tag", "lesson-tag");
	}

	private Lesson readyPublishedLesson(Long id) {
		return lessonWithStatus(id, LessonStatusCode.READY, LessonPublicationStatusCode.PUBLISHED);
	}

	private Lesson lessonWithStatus(Long id, String statusCode, String publicationStatusCode) {
		Lesson lesson = new Lesson();
		lesson.setId(id);
		LessonStatus status = new LessonStatus();
		status.setCode(statusCode);
		lesson.setStatus(status);
		LessonPublicationStatus publicationStatus = new LessonPublicationStatus();
		publicationStatus.setCode(publicationStatusCode);
		lesson.setPublicationStatus(publicationStatus);
		return lesson;
	}
}
