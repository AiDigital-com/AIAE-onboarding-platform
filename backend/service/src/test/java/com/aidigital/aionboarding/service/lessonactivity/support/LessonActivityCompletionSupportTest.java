package com.aidigital.aionboarding.service.lessonactivity.support;

import com.aidigital.aionboarding.domain.common.dictionary.ActivityTypeCode;
import com.aidigital.aionboarding.domain.learning.entities.UserLesson;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.learning.models.LessonEnrollmentRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.ActivityCompletionResultRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.ActivityProgressRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.ActivityProgressViewRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.LessonActivityRecord;
import com.aidigital.aionboarding.service.lessonactivity.services.LessonActivityAssemblyService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonActivityCompletionSupportTest {

	@Mock
	private LessonActivityAccessPolicy accessPolicy;
	@Mock
	private LessonActivityAssemblyService assemblyService;
	@Mock
	private LessonActivityRecordAssembler lessonActivityMapper;
	@Mock
	private LessonActivityProgressPersistence progressPersistence;
	@Mock
	private CurrentTime currentTime;

	@InjectMocks
	private LessonActivityCompletionSupport support;

	private AppUser viewer() {
		return new AppUser(1L, "clerk-1", "user1@test.com", "User 1", "member", "User", null, null, null);
	}

	private LessonActivityRecord activityRecordWithProgress(String type, boolean completed, Integer score) {
		ActivityProgressViewRecord view = new ActivityProgressViewRecord(
				"completed", score, Map.of(), LocalDateTime.now(),
				completed ? LocalDateTime.now() : null, completed);
		return new LessonActivityRecord(
				1L, 100L, type, "Activity", 1, Map.of(), Map.of(), "author", LocalDateTime.now(), view);
	}

	@Nested
	class BuildCompletionResult {

		@Test
		void shouldMarkLessonCompleteWhenAllActivitiesPassedTest() {
			// Given:
			AppUser viewer = viewer();
			Long lessonId = 100L;
			Long lessonCreatedByUserId = 50L;
			Lesson lesson = new Lesson();
			lesson.setId(lessonId);
			User creator = new User();
			creator.setId(lessonCreatedByUserId);
			lesson.setCreatedByUser(creator);
			LessonActivityRecord flashcard = activityRecordWithProgress(ActivityTypeCode.FLASHCARDS, true, null);
			LessonActivityRecord quiz = activityRecordWithProgress(ActivityTypeCode.QUIZ, true, 80);
			List<LessonActivityRecord> activities = List.of(flashcard, quiz);
			ActivityProgressRecord progress = new ActivityProgressRecord(10L, lessonId, "completed", null, null,
					Map.of());
			LessonEnrollmentRecord enrollment = new LessonEnrollmentRecord(lessonId, LocalDateTime.now(),
					LocalDateTime.now(), true);
			when(assemblyService.getLessonActivitiesForUser(lessonId, viewer.internalId())).thenReturn(activities);
			when(accessPolicy.redactQuizAnswersUnlessManager(activities, viewer, lessonCreatedByUserId))
					.thenReturn(activities);
			when(lessonActivityMapper.isActivityPassed(flashcard)).thenReturn(true);
			when(lessonActivityMapper.isActivityPassed(quiz)).thenReturn(true);
			when(progressPersistence.findUserLesson(viewer.internalId(), lessonId)).thenReturn(Optional.of(new UserLesson()));
			when(progressPersistence.saveUserLesson(any())).thenAnswer(invocation -> invocation.getArgument(0));
			when(lessonActivityMapper.toEnrollmentRecord(any())).thenReturn(enrollment);

			// When:
			ActivityCompletionResultRecord result = support.buildCompletionResult(viewer, lesson, progress, null);

			// Then:
			assertThat(result.lessonCompleted()).isTrue();
			assertThat(result.enrollment()).isSameAs(enrollment);
		}

		@Test
		void shouldNotMarkLessonCompleteWhenActivitiesListEmptyTest() {
			// Given:
			AppUser viewer = viewer();
			Long lessonId = 100L;
			Long lessonCreatedByUserId = 50L;
			Lesson lesson = new Lesson();
			lesson.setId(lessonId);
			User creator = new User();
			creator.setId(lessonCreatedByUserId);
			lesson.setCreatedByUser(creator);
			List<LessonActivityRecord> activities = List.of();
			ActivityProgressRecord progress = new ActivityProgressRecord(10L, lessonId, "completed", null, null,
					Map.of());
			LessonEnrollmentRecord enrollment = new LessonEnrollmentRecord(lessonId, LocalDateTime.now(), null, false);
			when(assemblyService.getLessonActivitiesForUser(lessonId, viewer.internalId())).thenReturn(activities);
			when(accessPolicy.redactQuizAnswersUnlessManager(activities, viewer, lessonCreatedByUserId))
					.thenReturn(activities);
			when(progressPersistence.findUserLesson(viewer.internalId(), lessonId)).thenReturn(Optional.of(new UserLesson()));
			when(lessonActivityMapper.toEnrollmentRecord(any())).thenReturn(enrollment);

			// When:
			ActivityCompletionResultRecord result = support.buildCompletionResult(viewer, lesson, progress, null);

			// Then:
			assertThat(result.lessonCompleted()).isFalse();
			verify(progressPersistence, never()).saveUserLesson(any());
		}

		@Test
		void shouldNotMarkLessonCompleteWhenSomeActivityNotPassedTest() {
			// Given:
			AppUser viewer = viewer();
			Long lessonId = 100L;
			Long lessonCreatedByUserId = 50L;
			Lesson lesson = new Lesson();
			lesson.setId(lessonId);
			User creator = new User();
			creator.setId(lessonCreatedByUserId);
			lesson.setCreatedByUser(creator);
			LessonActivityRecord flashcard = activityRecordWithProgress(ActivityTypeCode.FLASHCARDS, true, null);
			LessonActivityRecord quiz = activityRecordWithProgress(ActivityTypeCode.QUIZ, false, 60);
			List<LessonActivityRecord> activities = List.of(flashcard, quiz);
			ActivityProgressRecord progress = new ActivityProgressRecord(10L, lessonId, "failed", 60, null, Map.of());
			LessonEnrollmentRecord enrollment = new LessonEnrollmentRecord(lessonId, LocalDateTime.now(), null, false);
			when(assemblyService.getLessonActivitiesForUser(lessonId, viewer.internalId())).thenReturn(activities);
			when(accessPolicy.redactQuizAnswersUnlessManager(activities, viewer, lessonCreatedByUserId))
					.thenReturn(activities);
			when(lessonActivityMapper.isActivityPassed(flashcard)).thenReturn(true);
			when(lessonActivityMapper.isActivityPassed(quiz)).thenReturn(false);
			when(progressPersistence.findUserLesson(viewer.internalId(), lessonId)).thenReturn(Optional.of(new UserLesson()));
			when(lessonActivityMapper.toEnrollmentRecord(any())).thenReturn(enrollment);

			// When:
			ActivityCompletionResultRecord result = support.buildCompletionResult(viewer, lesson, progress, null);

			// Then:
			assertThat(result.lessonCompleted()).isFalse();
			verify(progressPersistence, never()).saveUserLesson(any());
		}
	}

	@Nested
	class SetLessonCompletionForUser {

		@Test
		void shouldSetCompletedAtWhenAllActivitiesPassedTest() {
			// Given:
			Long userId = 1L;
			Long lessonId = 100L;
			LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
			UserLesson enrollment = new UserLesson();
			LessonEnrollmentRecord record = new LessonEnrollmentRecord(lessonId, now, now, true);
			LessonActivityRecord flashcard = activityRecordWithProgress(ActivityTypeCode.FLASHCARDS, true, null);
			LessonActivityRecord quiz = activityRecordWithProgress(ActivityTypeCode.QUIZ, true, 80);
			when(assemblyService.getLessonActivitiesForUser(lessonId, userId)).thenReturn(List.of(flashcard, quiz));
			when(lessonActivityMapper.isActivityPassed(flashcard)).thenReturn(true);
			when(lessonActivityMapper.isActivityPassed(quiz)).thenReturn(true);
			when(progressPersistence.findUserLesson(userId, lessonId)).thenReturn(Optional.of(enrollment));
			when(currentTime.utcDateTime()).thenReturn(now);
			when(progressPersistence.saveUserLesson(enrollment)).thenReturn(enrollment);
			when(lessonActivityMapper.toEnrollmentRecord(enrollment)).thenReturn(record);

			// When:
			LessonEnrollmentRecord result = support.setLessonCompletionForUser(userId, lessonId, true);

			// Then:
			assertThat(result).isSameAs(record);
			assertThat(enrollment.getCompletedAt()).isEqualTo(now);
		}

		@Test
		void shouldThrowWhenMarkingCompleteButNotAllActivitiesPassedTest() {
			// Given:
			Long userId = 1L;
			Long lessonId = 100L;
			LessonActivityRecord flashcard = activityRecordWithProgress(ActivityTypeCode.FLASHCARDS, true, null);
			LessonActivityRecord quiz = activityRecordWithProgress(ActivityTypeCode.QUIZ, false, 60);
			when(assemblyService.getLessonActivitiesForUser(lessonId, userId)).thenReturn(List.of(flashcard, quiz));
			when(lessonActivityMapper.isActivityPassed(flashcard)).thenReturn(true);
			when(lessonActivityMapper.isActivityPassed(quiz)).thenReturn(false);

			// When-Then:
			assertThatThrownBy(() -> support.setLessonCompletionForUser(userId, lessonId, true))
					.isInstanceOf(AppException.class)
					.hasMessageContaining("Complete all lesson activities");
			verifyNoInteractions(progressPersistence);
		}

		@Test
		void shouldClearCompletedAtWhenMarkingIncompleteTest() {
			// Given:
			Long userId = 1L;
			Long lessonId = 100L;
			UserLesson enrollment = new UserLesson();
			enrollment.setCompletedAt(LocalDateTime.of(2025, 1, 1, 0, 0));
			LessonEnrollmentRecord record = new LessonEnrollmentRecord(lessonId, null, null, false);
			when(progressPersistence.findUserLesson(userId, lessonId)).thenReturn(Optional.of(enrollment));
			when(progressPersistence.saveUserLesson(enrollment)).thenReturn(enrollment);
			when(lessonActivityMapper.toEnrollmentRecord(enrollment)).thenReturn(record);

			// When:
			LessonEnrollmentRecord result = support.setLessonCompletionForUser(userId, lessonId, false);

			// Then:
			assertThat(result).isSameAs(record);
			assertThat(enrollment.getCompletedAt()).isNull();
		}

		@Test
		void shouldReturnNullWhenEnrollmentMissingTest() {
			// Given:
			Long userId = 1L;
			Long lessonId = 100L;
			when(progressPersistence.findUserLesson(userId, lessonId)).thenReturn(Optional.empty());

			// When:
			LessonEnrollmentRecord result = support.setLessonCompletionForUser(userId, lessonId, false);

			// Then:
			assertThat(result).isNull();
			verify(progressPersistence, never()).saveUserLesson(any());
		}
	}

	@Nested
	class GetLessonEnrollmentForUser {

		@Test
		void shouldReturnRecordWhenEnrollmentExistsTest() {
			// Given:
			Long userId = 1L;
			Long lessonId = 100L;
			UserLesson enrollment = new UserLesson();
			LessonEnrollmentRecord record = new LessonEnrollmentRecord(lessonId, null, null, false);
			when(progressPersistence.findUserLesson(userId, lessonId)).thenReturn(Optional.of(enrollment));
			when(lessonActivityMapper.toEnrollmentRecord(enrollment)).thenReturn(record);

			// When:
			LessonEnrollmentRecord result = support.getLessonEnrollmentForUser(userId, lessonId);

			// Then:
			assertThat(result).isSameAs(record);
		}

		@Test
		void shouldReturnNullWhenEnrollmentMissingTest() {
			// Given:
			Long userId = 1L;
			Long lessonId = 100L;
			when(progressPersistence.findUserLesson(userId, lessonId)).thenReturn(Optional.empty());

			// When:
			LessonEnrollmentRecord result = support.getLessonEnrollmentForUser(userId, lessonId);

			// Then:
			assertThat(result).isNull();
		}
	}
}
