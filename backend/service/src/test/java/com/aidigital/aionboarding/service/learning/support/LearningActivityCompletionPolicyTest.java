package com.aidigital.aionboarding.service.learning.support;

import com.aidigital.aionboarding.domain.common.dictionary.ActivityTypeCode;
import com.aidigital.aionboarding.domain.common.dictionary.entities.ActivityType;
import com.aidigital.aionboarding.domain.lessonactivity.entities.LessonActivity;
import com.aidigital.aionboarding.domain.lessonactivity.entities.UserLessonActivityProgress;
import com.aidigital.aionboarding.domain.lessonactivity.repositories.LessonActivityRepository;
import com.aidigital.aionboarding.domain.lessonactivity.repositories.UserLessonActivityProgressRepository;
import com.aidigital.aionboarding.service.common.error.AppException;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.instancio.Select.field;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningActivityCompletionPolicyTest {

	@Mock
	private LessonActivityRepository lessonActivityRepository;

	@Mock
	private UserLessonActivityProgressRepository activityProgressRepository;

	@InjectMocks
	private LearningActivityCompletionPolicy policy;

	@Test
	void shouldReturnWithoutThrowingWhenNoActivitiesExistTest() {
		// Given:
		Long userId = 1L;
		Long lessonId = 10L;
		when(lessonActivityRepository.findByLessonIdOrderByCreatedAtAsc(lessonId)).thenReturn(List.of());

		// When:
		policy.ensureAllActivitiesPassed(userId, lessonId);

		// Then: no exception thrown
		assertThat(true).isTrue();
	}

	@Test
	void shouldThrowWhenActivityHasNoProgressTest() {
		// Given:
		Long userId = 1L;
		Long lessonId = 10L;
		Long activityId = 100L;
		LessonActivity activity = activityWithType(activityId, ActivityTypeCode.FLASHCARDS);
		UserLessonActivityProgress.UserLessonActivityProgressId progressId = progressId(userId, activityId);

		when(lessonActivityRepository.findByLessonIdOrderByCreatedAtAsc(lessonId)).thenReturn(List.of(activity));
		when(activityProgressRepository.findById(progressId)).thenReturn(Optional.empty());

		// When-Then:
		assertThatThrownBy(() -> policy.ensureAllActivitiesPassed(userId, lessonId))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("Complete all lesson activities");
	}

	@Test
	void shouldThrowWhenActivityProgressHasNoCompletedAtTest() {
		// Given:
		Long userId = 1L;
		Long lessonId = 10L;
		Long activityId = 100L;
		LessonActivity activity = activityWithType(activityId, ActivityTypeCode.FLASHCARDS);
		UserLessonActivityProgress progress = progressWith(userId, activityId, null, null);

		when(lessonActivityRepository.findByLessonIdOrderByCreatedAtAsc(lessonId)).thenReturn(List.of(activity));
		when(activityProgressRepository.findById(progressId(userId, activityId))).thenReturn(Optional.of(progress));

		// When-Then:
		assertThatThrownBy(() -> policy.ensureAllActivitiesPassed(userId, lessonId))
				.isInstanceOf(AppException.class);
	}

	@Test
	void shouldThrowWhenQuizScoreIsBelowPassThresholdTest() {
		// Given:
		Long userId = 1L;
		Long lessonId = 10L;
		Long activityId = 100L;
		LessonActivity activity = activityWithType(activityId, ActivityTypeCode.QUIZ);
		UserLessonActivityProgress progress = progressWith(userId, activityId, LocalDateTime.now(), new BigDecimal("75"
		));

		when(lessonActivityRepository.findByLessonIdOrderByCreatedAtAsc(lessonId)).thenReturn(List.of(activity));
		when(activityProgressRepository.findById(progressId(userId, activityId))).thenReturn(Optional.of(progress));

		// When-Then:
		assertThatThrownBy(() -> policy.ensureAllActivitiesPassed(userId, lessonId))
				.isInstanceOf(AppException.class);
	}

	@Test
	void shouldThrowWhenQuizScoreIsNullTest() {
		// Given:
		Long userId = 1L;
		Long lessonId = 10L;
		Long activityId = 100L;
		LessonActivity activity = activityWithType(activityId, ActivityTypeCode.QUIZ);
		UserLessonActivityProgress progress = progressWith(userId, activityId, LocalDateTime.now(), null);

		when(lessonActivityRepository.findByLessonIdOrderByCreatedAtAsc(lessonId)).thenReturn(List.of(activity));
		when(activityProgressRepository.findById(progressId(userId, activityId))).thenReturn(Optional.of(progress));

		// When-Then:
		assertThatThrownBy(() -> policy.ensureAllActivitiesPassed(userId, lessonId))
				.isInstanceOf(AppException.class);
	}

	@Test
	void shouldPassWhenQuizScoreIsAtPassThresholdTest() {
		// Given:
		Long userId = 1L;
		Long lessonId = 10L;
		Long activityId = 100L;
		LessonActivity activity = activityWithType(activityId, ActivityTypeCode.QUIZ);
		UserLessonActivityProgress progress = progressWith(userId, activityId, LocalDateTime.now(), new BigDecimal("80"
		));

		when(lessonActivityRepository.findByLessonIdOrderByCreatedAtAsc(lessonId)).thenReturn(List.of(activity));
		when(activityProgressRepository.findById(progressId(userId, activityId))).thenReturn(Optional.of(progress));

		// When:
		policy.ensureAllActivitiesPassed(userId, lessonId);

		// Then: no exception thrown
		assertThat(true).isTrue();
	}

	@Test
	void shouldPassWhenQuizScoreIsAbovePassThresholdTest() {
		// Given:
		Long userId = 1L;
		Long lessonId = 10L;
		Long activityId = 100L;
		LessonActivity activity = activityWithType(activityId, ActivityTypeCode.QUIZ);
		UserLessonActivityProgress progress = progressWith(userId, activityId, LocalDateTime.now(), new BigDecimal("95"
		));

		when(lessonActivityRepository.findByLessonIdOrderByCreatedAtAsc(lessonId)).thenReturn(List.of(activity));
		when(activityProgressRepository.findById(progressId(userId, activityId))).thenReturn(Optional.of(progress));

		// When:
		policy.ensureAllActivitiesPassed(userId, lessonId);

		// Then: no exception thrown
		assertThat(true).isTrue();
	}

	@Test
	void shouldPassWhenNonQuizActivityIsCompletedTest() {
		// Given:
		Long userId = 1L;
		Long lessonId = 10L;
		Long activityId = 100L;
		LessonActivity activity = activityWithType(activityId, ActivityTypeCode.FLASHCARDS);
		UserLessonActivityProgress progress = progressWith(userId, activityId, LocalDateTime.now(), null);

		when(lessonActivityRepository.findByLessonIdOrderByCreatedAtAsc(lessonId)).thenReturn(List.of(activity));
		when(activityProgressRepository.findById(progressId(userId, activityId))).thenReturn(Optional.of(progress));

		// When:
		policy.ensureAllActivitiesPassed(userId, lessonId);

		// Then: no exception thrown
		assertThat(true).isTrue();
	}

	private LessonActivity activityWithType(Long activityId, String typeCode) {
		ActivityType type = Instancio.of(ActivityType.class)
				.set(field(ActivityType::getCode), typeCode)
				.create();
		LessonActivity activity = Instancio.create(LessonActivity.class);
		activity.setId(activityId);
		activity.setType(type);
		return activity;
	}

	private UserLessonActivityProgress.UserLessonActivityProgressId progressId(Long userId, Long activityId) {
		UserLessonActivityProgress.UserLessonActivityProgressId id =
				new UserLessonActivityProgress.UserLessonActivityProgressId();
		id.setUserId(userId);
		id.setActivityId(activityId);
		return id;
	}

	private UserLessonActivityProgress progressWith(Long userId, Long activityId, LocalDateTime completedAt,
	                                                BigDecimal score) {
		UserLessonActivityProgress progress = Instancio.create(UserLessonActivityProgress.class);
		progress.setId(progressId(userId, activityId));
		progress.setCompletedAt(completedAt);
		progress.setScore(score);
		return progress;
	}
}
