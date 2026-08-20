package com.aidigital.aionboarding.service.learning.services.impl;

import com.aidigital.aionboarding.domain.learning.entities.UserLesson;
import com.aidigital.aionboarding.domain.learning.entities.UserRoadmap;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.roadmap.entities.Roadmap;
import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapLesson;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.learning.services.LearningEnrollmentService;
import com.aidigital.aionboarding.service.learning.services.entity.LearningEnrollmentEntityService;
import com.aidigital.aionboarding.service.learning.support.LearningEnrollmentSupport;
import com.aidigital.aionboarding.service.roadmap.services.entity.RoadmapEntityService;
import com.aidigital.aionboarding.service.user.services.entity.UserEntityService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoadmapEnrollmentServiceImplTest {

	@Mock
	private RoadmapEntityService roadmapEntityService;
	@Mock
	private LearningEnrollmentEntityService learningEnrollmentEntityService;
	@Mock
	private UserEntityService userEntityService;
	@Mock
	private LearningEnrollmentSupport enrollmentSupport;
	@Mock
	private CurrentTime currentTime;
	@Mock
	private LearningEnrollmentService learningEnrollmentService;

	@InjectMocks
	private RoadmapEnrollmentServiceImpl service;

	@Nested
	class EnrollUsersInRoadmap {

		@Test
		void shouldReturnEmptyListWhenUserIdsEmptyTest() {
			// Given:
			Long roadmapId = 1L;

			// When:
			List<UserRoadmap> result = service.enrollUsersInRoadmap(List.of(), roadmapId);

			// Then:
			assertThat(result).isEmpty();
			verifyNoInteractions(roadmapEntityService);
		}

		@Test
		void shouldSkipExistingRoadmapEnrollmentsAndCreateNewOnesTest() {
			// Given:
			RoadmapEnrollmentServiceImpl spy = spy(service);
			Long roadmapId = 1L;
			Long existingUserId = 10L;
			Long newUserId = 20L;
			Roadmap roadmap = new Roadmap();
			roadmap.setId(roadmapId);
			UserRoadmap existing = userRoadmap(existingUserId, roadmapId);
			existing.setRoadmap(roadmap);
			UserRoadmap.UserRoadmapId newId = new UserRoadmap.UserRoadmapId();
			newId.setUserId(newUserId);
			newId.setRoadmapId(roadmapId);
			User newUser = userWithId(newUserId);
			LocalDateTime enrolledAt = LocalDateTime.of(2026, 1, 1, 0, 0);
			when(roadmapEntityService.getReference(roadmapId)).thenReturn(roadmap);
			when(learningEnrollmentEntityService.findUserRoadmapsByUserIdsAndRoadmapId(List.of(existingUserId, newUserId),
					roadmapId)).thenReturn(List.of(existing));
			when(currentTime.utcDateTime()).thenReturn(enrolledAt);
			when(enrollmentSupport.userRoadmapId(newUserId, roadmapId)).thenReturn(newId);
			when(userEntityService.getReference(newUserId)).thenReturn(newUser);
			doNothing().when(spy).fanOutRoadmapLessons(List.of(existingUserId, newUserId), roadmapId, true);

			// When:
			List<UserRoadmap> result = spy.enrollUsersInRoadmap(List.of(existingUserId, newUserId), roadmapId);

			// Then:
			assertThat(result).hasSize(2);
			assertThat(result.get(0)).isSameAs(existing);
			assertThat(result.get(1).getId()).isEqualTo(newId);
			assertThat(result.get(1).getEnrolledAt()).isEqualTo(enrolledAt);
			ArgumentCaptor<List<UserRoadmap>> captor = ArgumentCaptor.forClass(List.class);
			verify(learningEnrollmentEntityService).saveAllUserRoadmaps(captor.capture());
			assertThat(captor.getValue()).hasSize(1);
			assertThat(captor.getValue().get(0).getId()).isEqualTo(newId);
		}
	}

	@Nested
	class FanOutRoadmapLessons {

		@Test
		void shouldReturnEarlyWhenUserIdsEmptyTest() {
			// Given:
			Long roadmapId = 1L;

			// When:
			service.fanOutRoadmapLessons(List.of(), roadmapId, true);

			// Then:
			verifyNoInteractions(roadmapEntityService);
		}

		@Test
		void shouldReturnEarlyWhenNoRoadmapLessonsAreEnrollableTest() {
			// Given:
			Long roadmapId = 1L;
			Long userId = 10L;
			Lesson draftLesson = new Lesson();
			RoadmapLesson roadmapLesson = roadmapLesson(roadmapId, draftLesson, 1);
			when(roadmapEntityService.findByIdRoadmapIdOrderBySortOrderAsc(roadmapId))
					.thenReturn(List.of(roadmapLesson));
			when(learningEnrollmentService.isLearnable(draftLesson)).thenReturn(false);

			// When:
			service.fanOutRoadmapLessons(List.of(userId), roadmapId, true);

			// Then:
			verify(roadmapEntityService).findByIdRoadmapIdOrderBySortOrderAsc(roadmapId);
			verifyNoInteractions(learningEnrollmentEntityService);
		}

		@Test
		void shouldCreateNewLessonEnrollmentsForEnrollableRoadmapLessonsTest() {
			// Given:
			Long roadmapId = 1L;
			Long userId = 10L;
			Long lessonId = 100L;
			LocalDateTime base = LocalDateTime.of(2026, 1, 1, 12, 0, 0);
			Lesson lesson = new Lesson();
			lesson.setId(lessonId);
			RoadmapLesson roadmapLesson = roadmapLesson(roadmapId, lesson, 5);
			User user = userWithId(userId);
			UserLesson.UserLessonId id = new UserLesson.UserLessonId();
			id.setUserId(userId);
			id.setLessonId(lessonId);
			when(roadmapEntityService.findByIdRoadmapIdOrderBySortOrderAsc(roadmapId))
					.thenReturn(List.of(roadmapLesson));
			when(learningEnrollmentService.isLearnable(lesson)).thenReturn(true);
			when(learningEnrollmentEntityService.findUserLessonsByUserIdsAndLessonIds(List.of(userId), List.of(lessonId)))
					.thenReturn(List.of());
			when(currentTime.utcDateTime()).thenReturn(base);
			when(userEntityService.getReference(userId)).thenReturn(user);
			when(enrollmentSupport.userLessonId(userId, lessonId)).thenReturn(id);

			// When:
			service.fanOutRoadmapLessons(List.of(userId), roadmapId, true);

			// Then:
			ArgumentCaptor<List<UserLesson>> captor = ArgumentCaptor.forClass(List.class);
			verify(learningEnrollmentEntityService).saveAllUserLessons(captor.capture());
			assertThat(captor.getValue()).hasSize(1);
			assertThat(captor.getValue().get(0).getId()).isEqualTo(id);
			assertThat(captor.getValue().get(0).getEnrolledAt()).isEqualTo(base.minusNanos(5_000_000L));
		}

		@Test
		void shouldUpdateExistingEnrollmentWhenFlagSetTrueTest() {
			// Given:
			Long roadmapId = 1L;
			Long userId = 10L;
			Long lessonId = 100L;
			LocalDateTime base = LocalDateTime.of(2026, 1, 1, 12, 0, 0);
			Lesson lesson = new Lesson();
			lesson.setId(lessonId);
			RoadmapLesson roadmapLesson = roadmapLesson(roadmapId, lesson, 3);
			UserLesson existing = userLesson(userId, lessonId);
			existing.setLesson(lesson);
			existing.setEnrolledAt(LocalDateTime.of(2020, 1, 1, 0, 0));
			when(roadmapEntityService.findByIdRoadmapIdOrderBySortOrderAsc(roadmapId))
					.thenReturn(List.of(roadmapLesson));
			when(learningEnrollmentService.isLearnable(lesson)).thenReturn(true);
			when(learningEnrollmentEntityService.findUserLessonsByUserIdsAndLessonIds(List.of(userId), List.of(lessonId)))
					.thenReturn(List.of(existing));
			when(currentTime.utcDateTime()).thenReturn(base);
			when(learningEnrollmentEntityService.saveAllUserLessons(List.of(existing))).thenReturn(List.of(existing));

			// When:
			service.fanOutRoadmapLessons(List.of(userId), roadmapId, true);

			// Then:
			assertThat(existing.getEnrolledAt()).isEqualTo(base.minusNanos(3_000_000L));
			verify(learningEnrollmentEntityService).saveAllUserLessons(List.of(existing));
		}

		@Test
		void shouldSkipExistingEnrollmentWhenFlagSetFalseTest() {
			// Given:
			Long roadmapId = 1L;
			Long userId = 10L;
			Long lessonId = 100L;
			LocalDateTime base = LocalDateTime.of(2026, 1, 1, 12, 0, 0);
			Lesson lesson = new Lesson();
			lesson.setId(lessonId);
			RoadmapLesson roadmapLesson = roadmapLesson(roadmapId, lesson, 3);
			UserLesson existing = userLesson(userId, lessonId);
			existing.setLesson(lesson);
			when(roadmapEntityService.findByIdRoadmapIdOrderBySortOrderAsc(roadmapId))
					.thenReturn(List.of(roadmapLesson));
			when(learningEnrollmentService.isLearnable(lesson)).thenReturn(true);
			when(learningEnrollmentEntityService.findUserLessonsByUserIdsAndLessonIds(List.of(userId), List.of(lessonId)))
					.thenReturn(List.of(existing));
			when(currentTime.utcDateTime()).thenReturn(base);

			// When:
			service.fanOutRoadmapLessons(List.of(userId), roadmapId, false);

			// Then:
			verify(learningEnrollmentEntityService).saveAllUserLessons(List.of());
		}
	}

	@Nested
	class UnenrollUserFromRoadmap {

		@Test
		void shouldBulkDeleteRoadmapAndLessonEnrollmentsForSingleUserTest() {
			// Given:
			Long userId = 1L;
			Long roadmapId = 10L;

			// When:
			service.unenrollUserFromRoadmap(userId, roadmapId);

			// Then:
			verify(learningEnrollmentEntityService).deleteUserRoadmapsByUserIdsAndRoadmapId(List.of(userId),
					roadmapId);
			verify(learningEnrollmentEntityService).deleteRoadmapDerivedLessonEnrollments(List.of(userId), roadmapId);
		}
	}

	@Nested
	class UnenrollUsersFromRoadmap {

		@Test
		void shouldBulkDeleteRoadmapAndLessonEnrollmentsForMultipleUsersInOneStatementEachTest() {
			// Given:
			List<Long> userIds = List.of(1L, 2L, 3L);
			Long roadmapId = 10L;

			// When:
			service.unenrollUsersFromRoadmap(userIds, roadmapId);

			// Then:
			verify(learningEnrollmentEntityService).deleteUserRoadmapsByUserIdsAndRoadmapId(userIds, roadmapId);
			verify(learningEnrollmentEntityService).deleteRoadmapDerivedLessonEnrollments(userIds, roadmapId);
		}

		@Test
		void shouldDeduplicateAndDropNullUserIdsBeforeDeletingTest() {
			// Given:
			Long userId = 1L;
			Long roadmapId = 10L;
			List<Long> userIdsWithDuplicateAndNull = java.util.Arrays.asList(userId, null, userId);

			// When:
			service.unenrollUsersFromRoadmap(userIdsWithDuplicateAndNull, roadmapId);

			// Then:
			verify(learningEnrollmentEntityService).deleteUserRoadmapsByUserIdsAndRoadmapId(List.of(userId),
					roadmapId);
			verify(learningEnrollmentEntityService).deleteRoadmapDerivedLessonEnrollments(List.of(userId), roadmapId);
		}

		@Test
		void shouldNotDeleteWhenUserIdsIsEmptyTest() {
			// Given:
			Long roadmapId = 10L;

			// When:
			service.unenrollUsersFromRoadmap(List.of(), roadmapId);

			// Then:
			verify(learningEnrollmentEntityService, never()).deleteUserRoadmapsByUserIdsAndRoadmapId(List.of(),
					roadmapId);
			verify(learningEnrollmentEntityService, never()).deleteRoadmapDerivedLessonEnrollments(List.of(), roadmapId);
		}
	}

	// -------------------------------------------------------------------------
	// Setup helpers
	// -------------------------------------------------------------------------

	private User userWithId(Long userId) {
		User user = new User();
		user.setId(userId);
		return user;
	}

	private UserLesson userLesson(Long userId, Long lessonId) {
		UserLesson enrollment = new UserLesson();
		UserLesson.UserLessonId id = new UserLesson.UserLessonId();
		id.setUserId(userId);
		id.setLessonId(lessonId);
		enrollment.setId(id);
		enrollment.setEnrolledAt(LocalDateTime.of(2025, 1, 1, 0, 0));
		return enrollment;
	}

	private UserRoadmap userRoadmap(Long userId, Long roadmapId) {
		UserRoadmap enrollment = new UserRoadmap();
		UserRoadmap.UserRoadmapId id = new UserRoadmap.UserRoadmapId();
		id.setUserId(userId);
		id.setRoadmapId(roadmapId);
		enrollment.setId(id);
		enrollment.setEnrolledAt(LocalDateTime.of(2025, 1, 1, 0, 0));
		return enrollment;
	}

	private RoadmapLesson roadmapLesson(Long roadmapId, Lesson lesson, Integer sortOrder) {
		RoadmapLesson roadmapLesson = new RoadmapLesson();
		RoadmapLesson.RoadmapLessonId id = new RoadmapLesson.RoadmapLessonId();
		id.setRoadmapId(roadmapId);
		id.setLessonId(lesson.getId());
		roadmapLesson.setId(id);
		roadmapLesson.setLesson(lesson);
		roadmapLesson.setSortOrder(sortOrder);
		return roadmapLesson;
	}
}
