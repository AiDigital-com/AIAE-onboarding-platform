package com.aidigital.aionboarding.service.lessonactivity.services.impl;

import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.learning.services.RoadmapEnrollmentSyncService;
import com.aidigital.aionboarding.service.lesson.support.LessonVisibilityCase;
import com.aidigital.aionboarding.service.lesson.support.LessonVisibilityPolicy;
import com.aidigital.aionboarding.service.lessonactivity.models.ActivityAttemptRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.LessonActivityRecord;
import com.aidigital.aionboarding.service.lessonactivity.services.LessonActivityAssemblyService;
import com.aidigital.aionboarding.service.lessonactivity.services.LessonActivityManagementService;
import com.aidigital.aionboarding.service.lessonactivity.services.LessonActivityProgressService;
import com.aidigital.aionboarding.service.lessonactivity.support.LessonActivityAccessPolicy;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonActivityServiceImplTest {

	@Mock
	private PermissionService permissionService;
	@Mock
	private RoadmapEnrollmentSyncService roadmapEnrollmentSyncService;
	@Mock
	private LessonActivityAccessPolicy accessPolicy;
	@Mock
	private LessonActivityProgressService progressService;
	@Mock
	private LessonActivityManagementService managementService;
	@Mock
	private LessonActivityAssemblyService assemblyService;
	@Mock
	private LessonVisibilityPolicy lessonVisibilityPolicy;

	@InjectMocks
	private LessonActivityServiceImpl service;

	@Nested
	class getLessonActivities {

		@Test
		void getLessonActivities_calledByNonEnrolledLearnerOnPublishedLesson_returnsActivitiesWithoutRequiringEnrollmentTest() {
			// Given: a learner who has NOT enrolled in this lesson, but it is published — proves
			// the bug fix where browsing/previewing a lesson from the Library no longer requires
			// "Add to My Lessons" first (PAC_055).
			AppUser viewer = learnerViewer();
			Long lessonId = 10L;

			Lesson lesson = mock(Lesson.class);
			when(accessPolicy.requireLesson(lessonId)).thenReturn(lesson);
			when(lessonVisibilityPolicy.isVisible(viewer, lesson)).thenReturn(true);
			when(assemblyService.getLessonActivitiesForUser(lessonId, viewer.internalId())).thenReturn(List.of());
			when(accessPolicy.redactQuizAnswersUnlessManager(List.of(), viewer, null)).thenReturn(List.of());

			// Execution
			List<LessonActivityRecord> result = service.getLessonActivities(viewer, lessonId);

			// Verification
			assertThat(result).isEmpty();
			verify(accessPolicy, never()).requireEnrollment(any(), anyLong());
		}

		@Test
		void getLessonActivities_calledForLessonNotVisibleToLearner_throwsNotFoundTest() {
			// Given: a lesson the shared visibility policy rejects for this viewer (e.g. a
			// private lesson the learner is not enrolled in) — a plain learner may not preview
			// its activities at all. The rule matrix itself lives in LessonVisibilityPolicyTest;
			// this only proves requireVisibleLesson enforces whatever the policy decides.
			AppUser viewer = learnerViewer();
			Long lessonId = 15L;

			Lesson lesson = mock(Lesson.class);
			when(accessPolicy.requireLesson(lessonId)).thenReturn(lesson);
			when(lessonVisibilityPolicy.isVisible(viewer, lesson)).thenReturn(false);

			// Execution
			AppException thrown = assertThrows(AppException.class, () ->
					service.getLessonActivities(viewer, lessonId));

			// Verification
			assertThat(thrown.getCode()).isEqualTo(ErrorReason.C001.name());
			verify(assemblyService, never()).getLessonActivitiesForUser(any(), any());
		}

		@Test
		void getLessonActivities_calledByAdmin_returnsActivitiesTest() {
			// Given:
			AppUser viewer = adminViewer();
			Long lessonId = 20L;

			// Admin skips publication check — just needs to be found
			Lesson lesson = mock(Lesson.class);
			when(accessPolicy.requireLesson(lessonId)).thenReturn(lesson);
			when(lessonVisibilityPolicy.isVisible(viewer, lesson)).thenReturn(true);
			when(assemblyService.getLessonActivitiesForUser(lessonId, viewer.internalId())).thenReturn(List.of());
			when(accessPolicy.redactQuizAnswersUnlessManager(List.of(), viewer, null)).thenReturn(List.of());

			// Execution
			List<LessonActivityRecord> result = service.getLessonActivities(viewer, lessonId);

			// Verification
			assertThat(result).isEmpty();
		}
	}

	@Nested
	class canViewLesson {

		/**
		 * Driven by the same {@code visibilityMatrix()} that exercises the real
		 * {@link LessonVisibilityPolicy} in {@code LessonVisibilityPolicyTest}, so this
		 * pure-delegation test and that rule-correctness test cannot drift apart: this only
		 * proves {@code canViewLesson} returns whatever the policy decides for a given case,
		 * never re-deriving the rule itself.
		 */
		@ParameterizedTest(name = "{0}")
		@MethodSource("com.aidigital.aionboarding.service.lesson.support.LessonVisibilityPolicyTest#visibilityMatrix")
		void delegatesToLessonVisibilityPolicyTest(LessonVisibilityCase visibilityCase) {
			// Given:
			AppUser viewer = learnerViewer();
			Lesson lesson = mock(Lesson.class);
			when(lessonVisibilityPolicy.isVisible(viewer, lesson)).thenReturn(visibilityCase.expectedVisible());

			// Execution
			boolean result = service.canViewLesson(viewer, lesson);

			// Verification
			assertThat(result).isEqualTo(visibilityCase.expectedVisible());
		}
	}

	@Nested
	class getLessonActivity {

		@Test
		void getLessonActivity_calledByNonEnrolledLearnerOnPublishedLesson_returnsActivityWithoutRequiringEnrollmentTest() {
			// Given:
			AppUser viewer = learnerViewer();
			Long lessonId = 40L;
			Long activityId = 1L;

			Lesson lesson = mock(Lesson.class);
			when(accessPolicy.requireLesson(lessonId)).thenReturn(lesson);
			when(lessonVisibilityPolicy.isVisible(viewer, lesson)).thenReturn(true);

			LessonActivityRecord activityRecord = mock(LessonActivityRecord.class);
			when(assemblyService.getLessonActivity(lessonId, activityId, viewer.internalId())).thenReturn(activityRecord);
			when(assemblyService.getAttemptsForActivity(lessonId, activityId, viewer.internalId()))
					.thenReturn(List.<ActivityAttemptRecord>of());
			when(accessPolicy.redactQuizAnswersUnlessManager(activityRecord, viewer, null)).thenReturn(activityRecord);

			// Execution
			service.getLessonActivity(viewer, lessonId, activityId);

			// Verification
			verify(accessPolicy, never()).requireEnrollment(any(), anyLong());
		}

		@Test
		void getLessonActivity_calledByAdmin_returnsActivityTest() {
			// Given:
			AppUser viewer = adminViewer();
			Long lessonId = 50L;
			Long activityId = 2L;

			// Admin skips publication check — just needs to be found
			Lesson lesson = mock(Lesson.class);
			when(accessPolicy.requireLesson(lessonId)).thenReturn(lesson);
			when(lessonVisibilityPolicy.isVisible(viewer, lesson)).thenReturn(true);

			LessonActivityRecord activityRecord = mock(LessonActivityRecord.class);
			when(assemblyService.getLessonActivity(lessonId, activityId, viewer.internalId())).thenReturn(activityRecord);
			when(assemblyService.getAttemptsForActivity(lessonId, activityId, viewer.internalId()))
					.thenReturn(List.<ActivityAttemptRecord>of());
			when(accessPolicy.redactQuizAnswersUnlessManager(activityRecord, viewer, null)).thenReturn(activityRecord);

			// Execution
			service.getLessonActivity(viewer, lessonId, activityId);

			// Verification
			verify(accessPolicy, never()).requireEnrollment(any(), anyLong());
		}
	}

	// -------------------------------------------------------------------------
	// Setup helpers
	// -------------------------------------------------------------------------

	private AppUser learnerViewer() {
		return new AppUser(1L, "clerk-1", "learner@test.com", "Learner", "learner", "Learner", null, null, null);
	}

	private AppUser adminViewer() {
		return new AppUser(2L, "clerk-2", "admin@test.com", "Admin", "admin", "Admin", null, null, null);
	}
}
