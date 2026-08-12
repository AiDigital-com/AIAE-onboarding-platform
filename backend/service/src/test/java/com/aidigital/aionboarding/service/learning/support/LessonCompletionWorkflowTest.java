package com.aidigital.aionboarding.service.learning.support;

import com.aidigital.aionboarding.domain.learning.entities.UserLesson;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.common.time.CurrentTimeImpl;
import com.aidigital.aionboarding.service.learning.models.CompletedRoadmapRecord;
import com.aidigital.aionboarding.service.learning.models.LessonEnrollmentRecord;
import com.aidigital.aionboarding.service.learning.models.LessonEnrollmentResultRecord;
import com.aidigital.aionboarding.service.learning.services.LearningEnrollmentService;
import com.aidigital.aionboarding.service.learning.services.RoadmapEnrollmentSyncService;
import com.aidigital.aionboarding.service.learning.services.entity.LearningEnrollmentEntityService;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonEntityService;
import org.instancio.Instancio;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonCompletionWorkflowTest {

	@Mock
	private LessonEntityService lessonEntityService;
	@Mock
	private LearningEnrollmentEntityService learningEnrollmentEntityService;
	@Mock
	private LearningEnrollmentService learningEnrollmentService;
	@Mock
	private RoadmapEnrollmentSyncService roadmapEnrollmentSyncService;
	@Mock
	private LearningEnrollmentSupport learningEnrollmentSupport;
	@Mock
	private LearningActivityCompletionPolicy learningActivityCompletionPolicy;
	@Spy
	private CurrentTime currentTime = new CurrentTimeImpl();

	@InjectMocks
	private LessonCompletionWorkflow workflow;

	@Nested
	class MarkComplete {

		@Test
		void shouldEnsureActivitiesPassedAndReturnNewlyCompletedRoadmapsTest() {
			// Given:
			Long userId = 1L;
			Long lessonId = 10L;
			Lesson lesson = Instancio.create(Lesson.class);
			UserLesson.UserLessonId id = new UserLesson.UserLessonId();
			id.setUserId(userId);
			id.setLessonId(lessonId);
			UserLesson enrollment = new UserLesson();
			enrollment.setId(id);
			LessonEnrollmentRecord enrollmentRecord = Instancio.create(LessonEnrollmentRecord.class);
			CompletedRoadmapRecord completedRoadmap = Instancio.create(CompletedRoadmapRecord.class);

			when(lessonEntityService.getReference(lessonId)).thenReturn(lesson);
			when(learningEnrollmentService.isEnrollable(lesson)).thenReturn(true);
			when(learningEnrollmentSupport.userLessonId(userId, lessonId)).thenReturn(id);
			when(learningEnrollmentEntityService.findUserLessonById(id)).thenReturn(Optional.of(enrollment));
			when(roadmapEnrollmentSyncService.getCompletedRoadmapsForUserLesson(userId, lessonId))
					.thenReturn(List.of(completedRoadmap));
			when(learningEnrollmentSupport.toLessonEnrollment(enrollment)).thenReturn(enrollmentRecord);

			// When:
			LessonEnrollmentResultRecord result = workflow.setLessonCompletion(userId, lessonId, true);

			// Then:
			assertThat(enrollment.getCompletedAt()).isNotNull();
			assertThat(result.ok()).isTrue();
			assertThat(result.enrollment()).isSameAs(enrollmentRecord);
			assertThat(result.completedRoadmaps()).containsExactly(completedRoadmap);
			verify(learningActivityCompletionPolicy).ensureAllActivitiesPassed(userId, lessonId);
			verify(learningEnrollmentEntityService).save(enrollment);
		}
	}

	@Nested
	class MarkIncomplete {

		@Test
		void shouldClearCompletionAndReturnNoCompletedRoadmapsTest() {
			// Given:
			Long userId = 1L;
			Long lessonId = 10L;
			Lesson lesson = Instancio.create(Lesson.class);
			UserLesson.UserLessonId id = new UserLesson.UserLessonId();
			UserLesson enrollment = new UserLesson();
			enrollment.setId(id);
			enrollment.setCompletedAt(java.time.LocalDateTime.of(2026, 1, 1, 0, 0));
			LessonEnrollmentRecord enrollmentRecord = Instancio.create(LessonEnrollmentRecord.class);

			when(lessonEntityService.getReference(lessonId)).thenReturn(lesson);
			when(learningEnrollmentService.isEnrollable(lesson)).thenReturn(true);
			when(learningEnrollmentSupport.userLessonId(userId, lessonId)).thenReturn(id);
			when(learningEnrollmentEntityService.findUserLessonById(id)).thenReturn(Optional.of(enrollment));
			when(learningEnrollmentSupport.toLessonEnrollment(enrollment)).thenReturn(enrollmentRecord);

			// When:
			LessonEnrollmentResultRecord result = workflow.setLessonCompletion(userId, lessonId, false);

			// Then:
			assertThat(enrollment.getCompletedAt()).isNull();
			assertThat(result.completedRoadmaps()).isEmpty();
			verify(learningActivityCompletionPolicy, never()).ensureAllActivitiesPassed(userId, lessonId);
			verify(roadmapEnrollmentSyncService, never()).getCompletedRoadmapsForUserLesson(userId, lessonId);
		}
	}

	@Nested
	class ErrorPaths {

		@Test
		void shouldThrowWhenLessonIsNotEnrollableTest() {
			// Given:
			Long userId = 1L;
			Long lessonId = 10L;
			Lesson lesson = Instancio.create(Lesson.class);
			when(lessonEntityService.getReference(lessonId)).thenReturn(lesson);
			when(learningEnrollmentService.isEnrollable(lesson)).thenReturn(false);

			// When-Then:
			assertThatThrownBy(() -> workflow.setLessonCompletion(userId, lessonId, true))
					.isInstanceOf(AppException.class);
			verify(learningEnrollmentEntityService, never()).findUserLessonById(org.mockito.ArgumentMatchers.any());
		}

		@Test
		void shouldThrowWhenLearnerHasNoMatchingEnrollmentTest() {
			// Given:
			Long userId = 1L;
			Long lessonId = 10L;
			Lesson lesson = Instancio.create(Lesson.class);
			UserLesson.UserLessonId id = new UserLesson.UserLessonId();
			when(lessonEntityService.getReference(lessonId)).thenReturn(lesson);
			when(learningEnrollmentService.isEnrollable(lesson)).thenReturn(true);
			when(learningEnrollmentSupport.userLessonId(userId, lessonId)).thenReturn(id);
			when(learningEnrollmentEntityService.findUserLessonById(id)).thenReturn(Optional.empty());

			// When-Then:
			assertThatThrownBy(() -> workflow.setLessonCompletion(userId, lessonId, true))
					.isInstanceOf(AppException.class);
		}
	}
}
