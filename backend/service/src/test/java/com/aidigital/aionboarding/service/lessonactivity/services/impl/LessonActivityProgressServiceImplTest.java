package com.aidigital.aionboarding.service.lessonactivity.services.impl;

import com.aidigital.aionboarding.domain.common.dictionary.ActivityProgressStatusCode;
import com.aidigital.aionboarding.domain.common.dictionary.ActivityTypeCode;
import com.aidigital.aionboarding.domain.common.dictionary.entities.ActivityProgressStatus;
import com.aidigital.aionboarding.domain.common.dictionary.entities.ActivityType;
import com.aidigital.aionboarding.domain.learning.entities.UserLesson;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.lessonactivity.entities.LessonActivity;
import com.aidigital.aionboarding.domain.lessonactivity.entities.UserLessonActivityAttempt;
import com.aidigital.aionboarding.domain.lessonactivity.entities.UserLessonActivityProgress;
import com.aidigital.aionboarding.domain.lessonactivity.repositories.UserLessonActivityProgressRepository;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.learning.models.LessonEnrollmentRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.ActivityAttemptRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.ActivityCompletionResultRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.ActivityProgressRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.ActivityProgressViewRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.LessonActivityRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.QuizAnswerResultRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.QuizGradingResultRecord;
import com.aidigital.aionboarding.service.lessonactivity.services.LessonActivityAssemblyService;
import com.aidigital.aionboarding.service.lessonactivity.services.LessonActivityGradingService;
import com.aidigital.aionboarding.service.lessonactivity.support.LessonActivityAccessPolicy;
import com.aidigital.aionboarding.service.lessonactivity.support.LessonActivityPayloadAssembler;
import com.aidigital.aionboarding.service.lessonactivity.support.LessonActivityProgressPersistence;
import com.aidigital.aionboarding.service.lessonactivity.support.LessonActivityRecordAssembler;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonActivityProgressServiceImplTest {

	@Mock
	private LessonActivityPersistenceHelper lessonActivityPersistenceHelper;
	@Mock
	private LessonActivityAccessPolicy accessPolicy;
	@Mock
	private LessonActivityGradingService gradingService;
	@Mock
	private LessonActivityAssemblyService assemblyService;
	@Mock
	private LessonActivityPayloadAssembler payloadAssembler;
	@Mock
	private LessonActivityRecordAssembler lessonActivityMapper;
	@Mock
	private LessonActivityProgressPersistence progressPersistence;
	@Mock
	private UserLessonActivityProgressRepository progressRepository;
	@Mock
	private CurrentTime currentTime;

	@InjectMocks
	private LessonActivityProgressServiceImpl service;

	private AppUser viewer() {
		return new AppUser(1L, "clerk-1", "user1@test.com", "User 1", "member", "User", null, null, null);
	}

	private LessonActivity activityOfType(String typeCode) {
		LessonActivity activity = new LessonActivity();
		activity.setId(10L);
		ActivityType type = new ActivityType();
		type.setCode(typeCode);
		activity.setType(type);
		return activity;
	}

	private UserLessonActivityProgress progressOf(String statusCode) {
		UserLessonActivityProgress progress = new UserLessonActivityProgress();
		progress.setId(new UserLessonActivityProgress.UserLessonActivityProgressId());
		progress.setActivity(activityOfType(ActivityTypeCode.QUIZ));
		progress.setLesson(new Lesson());
		ActivityProgressStatus status = new ActivityProgressStatus();
		status.setCode(statusCode);
		progress.setStatus(status);
		progress.setMetadata(new java.util.HashMap<>());
		return progress;
	}

	private LessonActivityRecord activityRecord(String type) {
		return new LessonActivityRecord(
				1L, 100L, type, "Activity", 1, Map.of(), Map.of(), "author", LocalDateTime.now(), null);
	}

	private LessonActivityRecord activityRecordWithProgress(String type, boolean completed, Integer score) {
		ActivityProgressViewRecord view = new ActivityProgressViewRecord(
				"completed", score, Map.of(), LocalDateTime.now(),
				completed ? LocalDateTime.now() : null, completed);
		return new LessonActivityRecord(
				1L, 100L, type, "Activity", 1, Map.of(), Map.of(), "author", LocalDateTime.now(), view);
	}

	@Nested
	class SaveQuizAttempt {

		@Test
		void shouldInsertOnceWhenTheAttemptNumberIsNotContestedTest() {
			// Given:
			AppUser viewer = viewer();
			Lesson lesson = new Lesson();
			LessonActivity activity = activityOfType(ActivityTypeCode.QUIZ);
			QuizGradingResultRecord attempt = new QuizGradingResultRecord(80, true, 4, 5, List.of());
			UserLessonActivityAttempt saved = new UserLessonActivityAttempt();
			saved.setId(42L);
			when(progressPersistence.insertAttemptWithNextNumber(any())).thenReturn(saved);

			// When:
			UserLessonActivityAttempt result = service.saveQuizAttempt(
					viewer, lesson, activity, List.of(List.of("a")), attempt, Map.of());

			// Then:
			assertThat(result).isSameAs(saved);
			verify(progressPersistence, times(1)).insertAttemptWithNextNumber(any());
		}

		@Test
		void shouldRetryOnceWhenAConcurrentSubmissionAlreadyClaimedTheAttemptNumberTest() {
			// Given: a second, truly-concurrent submission for the same (user, activity) commits
			// its insert first, so this caller's first attempt loses the unique-constraint race.
			AppUser viewer = viewer();
			Lesson lesson = new Lesson();
			LessonActivity activity = activityOfType(ActivityTypeCode.QUIZ);
			QuizGradingResultRecord attempt = new QuizGradingResultRecord(60, false, 3, 5, List.of());
			UserLessonActivityAttempt saved = new UserLessonActivityAttempt();
			saved.setId(43L);
			when(progressPersistence.insertAttemptWithNextNumber(any()))
					.thenThrow(new DataIntegrityViolationException("duplicate attempt number"))
					.thenReturn(saved);

			// When:
			UserLessonActivityAttempt result = service.saveQuizAttempt(
					viewer, lesson, activity, List.of(List.of("b")), attempt, Map.of());

			// Then:
			assertThat(result).isSameAs(saved);
			ArgumentCaptor<UserLessonActivityAttempt> captor =
					ArgumentCaptor.forClass(UserLessonActivityAttempt.class);
			verify(progressPersistence, times(2)).insertAttemptWithNextNumber(captor.capture());
			assertThat(captor.getAllValues().get(0)).isSameAs(captor.getAllValues().get(1));
		}
	}

	@Nested
	class ResetProgress {

		@Test
		void shouldResetProgressToNotStartedAndClearLessonCompletionTest() {
			// Given:
			AppUser viewer = viewer();
			Long lessonId = 100L;
			Long activityId = 10L;
			LessonActivity activity = activityOfType(ActivityTypeCode.QUIZ);
			UserLessonActivityProgress progress = progressOf(ActivityProgressStatusCode.FAILED);
			ActivityProgressStatus notStarted = new ActivityProgressStatus();
			notStarted.setCode(ActivityProgressStatusCode.NOT_STARTED);
			LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
			when(lessonActivityPersistenceHelper.findByLessonIdAndId(lessonId, activityId)).thenReturn(activity);
			when(progressPersistence.loadOrCreateProgress(viewer.internalId(), activity, lessonId)).thenReturn(progress);
			when(accessPolicy.progressStatus(ActivityProgressStatusCode.NOT_STARTED)).thenReturn(notStarted);
			when(currentTime.utcDateTime()).thenReturn(now);
			when(progressPersistence.progressRepository()).thenReturn(progressRepository);
			when(progressRepository.save(progress)).thenReturn(progress);

			// When:
			service.resetProgress(viewer, lessonId, activityId);

			// Then:
			assertThat(progress.getStatus()).isSameAs(notStarted);
			assertThat(progress.getScore()).isNull();
			assertThat(progress.getMetadata()).isEmpty();
			assertThat(progress.getCompletedAt()).isNull();
			assertThat(progress.getUpdatedAt()).isEqualTo(now);
			verify(progressRepository).save(progress);
			verify(progressPersistence).clearLessonCompletion(viewer.internalId(), lessonId);
		}
	}

	@Nested
	class CompleteFlashcards {

		@Test
		void shouldMarkFlashcardsCompleteAndBuildCompletionResultTest() {
			// Given:
			AppUser viewer = viewer();
			Long lessonId = 100L;
			Long activityId = 10L;
			Lesson lesson = new Lesson();
			lesson.setId(lessonId);
			LessonActivity activity = activityOfType(ActivityTypeCode.FLASHCARDS);
			activity.setLesson(lesson);
			UserLessonActivityProgress progress = progressOf(ActivityProgressStatusCode.NOT_STARTED);
			ActivityProgressStatus completed = new ActivityProgressStatus();
			completed.setCode(ActivityProgressStatusCode.COMPLETED);
			LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
			Map<String, Object> request = Map.of("reviewedCards", 5);
			when(lessonActivityPersistenceHelper.findByLessonIdAndId(lessonId, activityId)).thenReturn(activity);
			when(payloadAssembler.parseInt(request.get("reviewedCards"), 0)).thenReturn(5);
			when(progressPersistence.loadOrCreateProgress(viewer.internalId(), activity, lessonId)).thenReturn(progress);
			when(accessPolicy.progressStatus(ActivityProgressStatusCode.COMPLETED)).thenReturn(completed);
			when(currentTime.utcDateTime()).thenReturn(now);
			when(progressPersistence.progressRepository()).thenReturn(progressRepository);
			when(progressRepository.save(progress)).thenReturn(progress);
			ActivityProgressRecord progressRecord = new ActivityProgressRecord(activityId, lessonId, "completed", null,
					now, Map.of("reviewedCards", 5, "completedFrom", "flashcards-player"));
			when(lessonActivityMapper.toProgressRecord(progress)).thenReturn(progressRecord);
			LessonActivityProgressServiceImpl spy = spy(service);
			ActivityCompletionResultRecord completion = new ActivityCompletionResultRecord(
					progressRecord, List.of(), false, null, null);
			doReturn(completion).when(spy).buildCompletionResult(viewer, lesson, progressRecord, null);

			// When:
			ActivityCompletionResultRecord result = spy.completeFlashcards(viewer, lesson, activityId, request);

			// Then:
			assertThat(result).isSameAs(completion);
			assertThat(progress.getStatus()).isSameAs(completed);
			assertThat(progress.getMetadata()).containsEntry("reviewedCards", 5);
			assertThat(progress.getCompletedAt()).isEqualTo(now);
			assertThat(progress.getUpdatedAt()).isEqualTo(now);
			verify(progressRepository).save(progress);
		}
	}

	@Nested
	class CompleteQuiz {

		@Test
		void shouldThrowWhenQuizHasNoQuestionsTest() {
			// Given:
			AppUser viewer = viewer();
			Long lessonId = 100L;
			Long activityId = 10L;
			Lesson lesson = new Lesson();
			lesson.setId(lessonId);
			LessonActivity activity = activityOfType(ActivityTypeCode.QUIZ);
			activity.setLesson(lesson);
			when(lessonActivityPersistenceHelper.findByLessonIdAndId(lessonId, activityId)).thenReturn(activity);
			when(gradingService.gradeQuiz(activity.getPayload(), List.of())).thenReturn(
					new QuizGradingResultRecord(0, false, 0, 0, List.of()));

			// When-Then:
			assertThatThrownBy(() -> service.completeQuiz(viewer, lesson, activityId, List.of()))
					.isInstanceOf(AppException.class)
					.hasMessageContaining("no questions");
		}

		@Test
		void shouldSavePassedQuizProgressAndBuildCompletionResultTest() {
			// Given:
			AppUser viewer = viewer();
			Long lessonId = 100L;
			Long activityId = 10L;
			Lesson lesson = new Lesson();
			lesson.setId(lessonId);
			LessonActivity activity = activityOfType(ActivityTypeCode.QUIZ);
			activity.setLesson(lesson);
			QuizGradingResultRecord attempt = new QuizGradingResultRecord(80, true, 4, 5, List.of());
			UserLessonActivityAttempt savedAttempt = new UserLessonActivityAttempt();
			savedAttempt.setId(1L);
			LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
			ActivityProgressStatus completed = new ActivityProgressStatus();
			completed.setCode(ActivityProgressStatusCode.COMPLETED);
			UserLessonActivityProgress progress = progressOf(ActivityProgressStatusCode.NOT_STARTED);
			String submittedAt = "2026-01-01T00:00:00Z";
			Map<String, Object> metadata = new java.util.HashMap<>();
			metadata.put("submittedAt", submittedAt);
			metadata.put("completedFrom", "quiz-player");
			metadata.put("correctCount", 4);
			metadata.put("totalCount", 5);
			metadata.put("passed", true);
			metadata.put("results", List.of());
			ActivityProgressRecord progressRecord = new ActivityProgressRecord(activityId, lessonId, "completed", 80,
					now, metadata);
			ActivityAttemptRecord attemptRecord = new ActivityAttemptRecord(1L, 1L, lessonId, activityId,
					ActivityTypeCode.QUIZ, 1, 80, true, 4, 5, List.of(), List.of(), metadata, now);
			ActivityCompletionResultRecord completion = new ActivityCompletionResultRecord(
					progressRecord, List.of(), false, null, attemptRecord);

			LessonActivityProgressServiceImpl spy = spy(service);
			when(lessonActivityPersistenceHelper.findByLessonIdAndId(lessonId, activityId)).thenReturn(activity);
			when(gradingService.gradeQuiz(activity.getPayload(), List.of(List.of("a")))).thenReturn(attempt);
			when(currentTime.instantString()).thenReturn(submittedAt);
			doReturn(savedAttempt).when(spy).saveQuizAttempt(viewer, lesson, activity, List.of(List.of("a")), attempt,
					metadata);
			when(progressPersistence.loadOrCreateProgress(viewer.internalId(), activity, lessonId)).thenReturn(progress);
			when(accessPolicy.progressStatus(ActivityProgressStatusCode.COMPLETED)).thenReturn(completed);
			when(currentTime.utcDateTime()).thenReturn(now);
			when(progressPersistence.progressRepository()).thenReturn(progressRepository);
			when(progressRepository.save(progress)).thenReturn(progress);
			when(lessonActivityMapper.toProgressRecord(progress)).thenReturn(progressRecord);
			when(lessonActivityMapper.toAttemptRecord(savedAttempt, 80, true, 4, 5, List.of(), List.of(List.of("a"))))
					.thenReturn(attemptRecord);
			doReturn(completion).when(spy).buildCompletionResult(viewer, lesson, progressRecord, attemptRecord);

			// When:
			ActivityCompletionResultRecord result = spy.completeQuiz(viewer, lesson, activityId, List.of(List.of("a")));

			// Then:
			assertThat(result).isSameAs(completion);
			assertThat(progress.getStatus()).isSameAs(completed);
			assertThat(progress.getScore()).isEqualTo(BigDecimal.valueOf(80));
			assertThat(progress.getCompletedAt()).isEqualTo(now);
			verify(progressPersistence, never()).clearLessonCompletion(viewer.internalId(), lessonId);
		}

		@Test
		void shouldClearLessonCompletionWhenQuizFailedTest() {
			// Given:
			AppUser viewer = viewer();
			Long lessonId = 100L;
			Long activityId = 10L;
			Lesson lesson = new Lesson();
			lesson.setId(lessonId);
			LessonActivity activity = activityOfType(ActivityTypeCode.QUIZ);
			activity.setLesson(lesson);
			QuizGradingResultRecord attempt = new QuizGradingResultRecord(60, false, 3, 5, List.of());
			UserLessonActivityAttempt savedAttempt = new UserLessonActivityAttempt();
			savedAttempt.setId(1L);
			LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
			ActivityProgressStatus failed = new ActivityProgressStatus();
			failed.setCode(ActivityProgressStatusCode.FAILED);
			UserLessonActivityProgress progress = progressOf(ActivityProgressStatusCode.NOT_STARTED);
			String submittedAt = "2026-01-01T00:00:00Z";
			Map<String, Object> metadata = new java.util.HashMap<>();
			metadata.put("submittedAt", submittedAt);
			metadata.put("completedFrom", "quiz-player");
			metadata.put("correctCount", 3);
			metadata.put("totalCount", 5);
			metadata.put("passed", false);
			metadata.put("results", List.of());
			ActivityProgressRecord progressRecord = new ActivityProgressRecord(activityId, lessonId, "failed", 60,
					null, metadata);
			ActivityAttemptRecord attemptRecord = new ActivityAttemptRecord(1L, 1L, lessonId, activityId,
					ActivityTypeCode.QUIZ, 1, 60, false, 3, 5, List.of(), List.of(), metadata, now);
			ActivityCompletionResultRecord completion = new ActivityCompletionResultRecord(
					progressRecord, List.of(), false, null, attemptRecord);

			LessonActivityProgressServiceImpl spy = spy(service);
			when(lessonActivityPersistenceHelper.findByLessonIdAndId(lessonId, activityId)).thenReturn(activity);
			when(gradingService.gradeQuiz(activity.getPayload(), List.of(List.of("a")))).thenReturn(attempt);
			when(currentTime.instantString()).thenReturn(submittedAt);
			doReturn(savedAttempt).when(spy).saveQuizAttempt(viewer, lesson, activity, List.of(List.of("a")), attempt,
					metadata);
			when(progressPersistence.loadOrCreateProgress(viewer.internalId(), activity, lessonId)).thenReturn(progress);
			when(accessPolicy.progressStatus(ActivityProgressStatusCode.FAILED)).thenReturn(failed);
			when(currentTime.utcDateTime()).thenReturn(now);
			when(progressPersistence.progressRepository()).thenReturn(progressRepository);
			when(progressRepository.save(progress)).thenReturn(progress);
			when(lessonActivityMapper.toProgressRecord(progress)).thenReturn(progressRecord);
			when(lessonActivityMapper.toAttemptRecord(savedAttempt, 60, false, 3, 5, List.of(), List.of(List.of("a"))))
					.thenReturn(attemptRecord);
			doReturn(completion).when(spy).buildCompletionResult(viewer, lesson, progressRecord, attemptRecord);

			// When:
			spy.completeQuiz(viewer, lesson, activityId, List.of(List.of("a")));

			// Then:
			verify(progressPersistence).clearLessonCompletion(viewer.internalId(), lessonId);
			assertThat(progress.getCompletedAt()).isNull();
		}
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
			LessonActivityProgressServiceImpl spy = spy(service);
			doReturn(enrollment).when(spy).setLessonCompletionForUser(viewer.internalId(), lessonId, true);

			// When:
			ActivityCompletionResultRecord result = spy.buildCompletionResult(viewer, lesson, progress, null);

			// Then:
			assertThat(result.lessonCompleted()).isTrue();
			assertThat(result.enrollment()).isSameAs(enrollment);
			verify(spy).setLessonCompletionForUser(viewer.internalId(), lessonId, true);
			verify(spy, never()).getLessonEnrollmentForUser(viewer.internalId(), lessonId);
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
			LessonActivityProgressServiceImpl spy = spy(service);
			doReturn(enrollment).when(spy).getLessonEnrollmentForUser(viewer.internalId(), lessonId);

			// When:
			ActivityCompletionResultRecord result = spy.buildCompletionResult(viewer, lesson, progress, null);

			// Then:
			assertThat(result.lessonCompleted()).isFalse();
			verify(spy).getLessonEnrollmentForUser(viewer.internalId(), lessonId);
			verify(spy, never()).setLessonCompletionForUser(viewer.internalId(), lessonId, true);
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
			LessonActivityProgressServiceImpl spy = spy(service);
			doReturn(enrollment).when(spy).getLessonEnrollmentForUser(viewer.internalId(), lessonId);

			// When:
			ActivityCompletionResultRecord result = spy.buildCompletionResult(viewer, lesson, progress, null);

			// Then:
			assertThat(result.lessonCompleted()).isFalse();
			verify(spy).getLessonEnrollmentForUser(viewer.internalId(), lessonId);
			verify(spy, never()).setLessonCompletionForUser(viewer.internalId(), lessonId, true);
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
			LessonEnrollmentRecord result = service.setLessonCompletionForUser(userId, lessonId, true);

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
			assertThatThrownBy(() -> service.setLessonCompletionForUser(userId, lessonId, true))
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
			LessonEnrollmentRecord result = service.setLessonCompletionForUser(userId, lessonId, false);

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
			LessonEnrollmentRecord result = service.setLessonCompletionForUser(userId, lessonId, false);

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
			LessonEnrollmentRecord result = service.getLessonEnrollmentForUser(userId, lessonId);

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
			LessonEnrollmentRecord result = service.getLessonEnrollmentForUser(userId, lessonId);

			// Then:
			assertThat(result).isNull();
		}
	}

	@Nested
	class RequireActivity {

		@Test
		void shouldReturnActivityFromPersistenceHelperTest() {
			// Given:
			Long lessonId = 100L;
			Long activityId = 10L;
			LessonActivity activity = activityOfType(ActivityTypeCode.FLASHCARDS);
			when(lessonActivityPersistenceHelper.findByLessonIdAndId(lessonId, activityId)).thenReturn(activity);

			// When:
			LessonActivity result = service.requireActivity(lessonId, activityId);

			// Then:
			assertThat(result).isSameAs(activity);
		}

		@Test
		void shouldRejectNonFlashcardsActivityForFlashcardsCompletionTest() {
			// Given:
			Long lessonId = 100L;
			Long activityId = 10L;
			LessonActivity activity = activityOfType(ActivityTypeCode.QUIZ);
			when(lessonActivityPersistenceHelper.findByLessonIdAndId(lessonId, activityId)).thenReturn(activity);

			// When-Then:
			assertThatThrownBy(() -> service.requireFlashcardsActivity(lessonId, activityId))
					.isInstanceOf(AppException.class)
					.hasMessageContaining("Only flashcard activities");
		}

		@Test
		void shouldRejectNonQuizActivityForQuizCompletionTest() {
			// Given:
			Long lessonId = 100L;
			Long activityId = 10L;
			LessonActivity activity = activityOfType(ActivityTypeCode.FLASHCARDS);
			when(lessonActivityPersistenceHelper.findByLessonIdAndId(lessonId, activityId)).thenReturn(activity);

			// When-Then:
			assertThatThrownBy(() -> service.requireQuizActivity(lessonId, activityId))
					.isInstanceOf(AppException.class)
					.hasMessageContaining("Only quiz activities");
		}
	}

	@Nested
	class BuildQuizMetadata {

		@Test
		void shouldMapAttemptFieldsIntoMetadataTest() {
			// Given:
			LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
			QuizAnswerResultRecord result = new QuizAnswerResultRecord("single_choice", "Q", List.of(), List.of(),
					List.of(), true, "exp");
			QuizGradingResultRecord attempt = new QuizGradingResultRecord(80, true, 4, 5, List.of(result));
			when(currentTime.instantString()).thenReturn("2026-01-01T00:00:00Z");
			Map<String, Object> resultMap = Map.of(
					"type", "single_choice",
					"question", "Q",
					"options", List.of(),
					"selectedAnswers", List.of(),
					"correctAnswers", List.of(),
					"isCorrect", true,
					"explanation", "exp");
			when(lessonActivityMapper.toQuizAnswerResultMap(result)).thenReturn(resultMap);

			// When:
			Map<String, Object> metadata = service.buildQuizMetadata(attempt);

			// Then:
			assertThat(metadata).containsEntry("submittedAt", "2026-01-01T00:00:00Z");
			assertThat(metadata).containsEntry("completedFrom", "quiz-player");
			assertThat(metadata).containsEntry("correctCount", 4);
			assertThat(metadata).containsEntry("totalCount", 5);
			assertThat(metadata).containsEntry("passed", true);
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> results = (List<Map<String, Object>>) metadata.get("results");
			assertThat(results).containsExactly(resultMap);
		}
	}
}
