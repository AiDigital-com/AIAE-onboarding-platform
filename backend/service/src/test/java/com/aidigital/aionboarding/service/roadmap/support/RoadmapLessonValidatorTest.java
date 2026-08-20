package com.aidigital.aionboarding.service.roadmap.support;

import com.aidigital.aionboarding.domain.common.dictionary.LessonPublicationStatusCode;
import com.aidigital.aionboarding.domain.common.dictionary.LessonStatusCode;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonPublicationStatus;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonStatus;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.learning.services.LearningEnrollmentService;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonEntityService;
import com.aidigital.aionboarding.service.lesson.support.LessonVisibilityPolicy;
import com.aidigital.aionboarding.service.lesson.util.LessonTagUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoadmapLessonValidatorTest {

	@Mock
	private LessonEntityService lessonEntityService;
	@Mock
	private LessonTagUtil lessonTagUtil;
	@Mock
	private LearningEnrollmentService learningEnrollmentService;
	@Mock
	private LessonVisibilityPolicy lessonVisibilityPolicy;

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
	void shouldValidateLearnableLessonsInRequestedOrderTest() {
		// Given:
		AppUser actor = actor();
		Lesson lessonOne = readyPublishedLesson(1L);
		Lesson lessonTwo = readyPublishedLesson(2L);
		when(lessonEntityService.findAllById(eq(List.of(1L, 2L)))).thenReturn(List.of(lessonOne, lessonTwo));
		when(learningEnrollmentService.isLearnable(lessonOne)).thenReturn(true);
		when(learningEnrollmentService.isLearnable(lessonTwo)).thenReturn(true);
		when(lessonVisibilityPolicy.visibleLessonIds(actor, List.of(lessonOne, lessonTwo)))
				.thenReturn(Set.of(1L, 2L));

		// When:
		List<Lesson> result = validator.validateReadyPublishedLessons(actor, List.of(1L, 2L));

		// Then:
		assertThat(result).extracting(Lesson::getId).containsExactly(1L, 2L);
	}

	@Test
	void shouldAcceptAPrivateLessonVisibleToTheActorTest() {
		// Given: a private (assigned-only) lesson must remain includable, when visible to the
		// actor, so the roadmap's own fan-out (which already enrolls members into private
		// lessons via isLearnable) is reachable through the API
		AppUser actor = actor();
		Lesson lesson = lessonWithStatus(1L, LessonStatusCode.READY, LessonPublicationStatusCode.PRIVATE);
		when(lessonEntityService.findAllById(eq(List.of(1L)))).thenReturn(List.of(lesson));
		when(learningEnrollmentService.isLearnable(lesson)).thenReturn(true);
		when(lessonVisibilityPolicy.visibleLessonIds(actor, List.of(lesson))).thenReturn(Set.of(1L));

		// When:
		List<Lesson> result = validator.validateReadyPublishedLessons(actor, List.of(1L));

		// Then:
		assertThat(result).extracting(Lesson::getId).containsExactly(1L);
	}

	@Test
	void shouldRejectEmptyLessonListTest() {
		// Given:
		AppUser actor = actor();

		// When-Then:
		assertThatThrownBy(() -> validator.validateReadyPublishedLessons(actor, List.of()))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("Select at least one lesson");
	}

	@Test
	void shouldRejectWhenLessonCountDoesNotMatchIdsTest() {
		// Given:
		AppUser actor = actor();
		when(lessonEntityService.findAllById(eq(List.of(1L, 2L)))).thenReturn(List.of(readyPublishedLesson(1L)));

		// When-Then:
		assertThatThrownBy(() -> validator.validateReadyPublishedLessons(actor, List.of(1L, 2L)))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("Roadmaps can include only existing, learnable lessons");
	}

	@Test
	void shouldRejectWhenLessonIsNotLearnableTest() {
		// Given: not ready yet (still generating), so not learnable, even though visible
		AppUser actor = actor();
		Lesson lesson = lessonWithStatus(1L, LessonStatusCode.DRAFT, LessonPublicationStatusCode.PUBLISHED);
		when(lessonEntityService.findAllById(eq(List.of(1L)))).thenReturn(List.of(lesson));
		when(learningEnrollmentService.isLearnable(lesson)).thenReturn(false);
		when(lessonVisibilityPolicy.visibleLessonIds(actor, List.of(lesson))).thenReturn(Set.of(1L));

		// When-Then:
		assertThatThrownBy(() -> validator.validateReadyPublishedLessons(actor, List.of(1L)))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("Roadmaps can include only existing, learnable lessons");
	}

	@Test
	void shouldRejectWhenLessonIsArchivedTest() {
		// Given: archived lessons are never learnable, even though they exist and are visible
		AppUser actor = actor();
		Lesson lesson = lessonWithStatus(1L, LessonStatusCode.READY, LessonPublicationStatusCode.ARCHIVED);
		when(lessonEntityService.findAllById(eq(List.of(1L)))).thenReturn(List.of(lesson));
		when(learningEnrollmentService.isLearnable(lesson)).thenReturn(false);
		when(lessonVisibilityPolicy.visibleLessonIds(actor, List.of(lesson))).thenReturn(Set.of(1L));

		// When-Then:
		assertThatThrownBy(() -> validator.validateReadyPublishedLessons(actor, List.of(1L)))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("Roadmaps can include only existing, learnable lessons");
	}

	@Test
	void shouldRejectALearnablePrivateLessonTheActorCannotSeeTest() {
		// Given: the regression case for the actual leak this closes — a team lead adds
		// another author's private lesson by ID. The lesson is learnable (private is a valid
		// learnable state) but the visibility policy does not include it, so it must still be
		// rejected with the SAME message a non-existent ID gets — never a distinct message that
		// would confirm the lesson exists.
		AppUser teamLead = new AppUser(9L, "clerk-9", "lead@test.com", "Lead", "teamlead", "Lead", null, null, null);
		Lesson othersPrivateLesson = lessonWithStatus(7L, LessonStatusCode.READY, LessonPublicationStatusCode.PRIVATE);
		when(lessonEntityService.findAllById(eq(List.of(7L)))).thenReturn(List.of(othersPrivateLesson));
		when(learningEnrollmentService.isLearnable(othersPrivateLesson)).thenReturn(true);
		when(lessonVisibilityPolicy.visibleLessonIds(teamLead, List.of(othersPrivateLesson))).thenReturn(Set.of());

		// When-Then:
		assertThatThrownBy(() -> validator.validateReadyPublishedLessons(teamLead, List.of(7L)))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("Roadmaps can include only existing, learnable lessons.");
	}

	@Test
	void shouldResolveVisibilityInExactlyOneBatchCallRegardlessOfLessonCountTest() {
		// Given: a roadmap with several lessons — the validator must not loop calling the
		// visibility policy once per lesson; it must resolve the whole batch in one call
		AppUser actor = actor();
		Lesson lessonOne = readyPublishedLesson(1L);
		Lesson lessonTwo = readyPublishedLesson(2L);
		Lesson lessonThree = readyPublishedLesson(3L);
		List<Lesson> lessons = List.of(lessonOne, lessonTwo, lessonThree);
		when(lessonEntityService.findAllById(eq(List.of(1L, 2L, 3L)))).thenReturn(lessons);
		when(learningEnrollmentService.isLearnable(lessonOne)).thenReturn(true);
		when(learningEnrollmentService.isLearnable(lessonTwo)).thenReturn(true);
		when(learningEnrollmentService.isLearnable(lessonThree)).thenReturn(true);
		when(lessonVisibilityPolicy.visibleLessonIds(actor, lessons)).thenReturn(Set.of(1L, 2L, 3L));

		// When:
		validator.validateReadyPublishedLessons(actor, List.of(1L, 2L, 3L));

		// Then:
		verify(lessonVisibilityPolicy, times(1)).visibleLessonIds(actor, lessons);
		verifyNoMoreInteractions(lessonVisibilityPolicy);
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

	private AppUser actor() {
		return new AppUser(1L, "clerk-1", "actor@test.com", "Actor", "teamlead", "Actor", null, null, null);
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
