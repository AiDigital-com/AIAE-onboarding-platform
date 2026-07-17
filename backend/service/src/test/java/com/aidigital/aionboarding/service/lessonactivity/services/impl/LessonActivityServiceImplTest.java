package com.aidigital.aionboarding.service.lessonactivity.services.impl;

import com.aidigital.aionboarding.domain.common.dictionary.LessonPublicationStatusCode;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonPublicationStatus;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.learning.services.LearningService;
import com.aidigital.aionboarding.service.lessonactivity.models.ActivityAttemptRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.LessonActivityRecord;
import com.aidigital.aionboarding.service.lessonactivity.services.LessonActivityAssemblyService;
import com.aidigital.aionboarding.service.lessonactivity.services.LessonActivityManagementService;
import com.aidigital.aionboarding.service.lessonactivity.services.LessonActivityProgressService;
import com.aidigital.aionboarding.service.lessonactivity.support.LessonActivityAccessPolicy;
import com.aidigital.aionboarding.service.lessonactivity.support.LessonActivityPayloadAssembler;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
	private LearningService learningService;
	@Mock
	private LessonActivityAccessPolicy accessPolicy;
	@Mock
	private LessonActivityProgressService progressService;
	@Mock
	private LessonActivityManagementService managementService;
	@Mock
	private LessonActivityAssemblyService assemblyService;
	@Mock
	private LessonActivityPayloadAssembler payloadAssembler;

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

			Lesson lesson = publishedLesson();
			when(accessPolicy.requireLesson(lessonId)).thenReturn(lesson);
			when(assemblyService.getLessonActivitiesForUser(lessonId, viewer.internalId())).thenReturn(List.of());
			when(accessPolicy.redactQuizAnswersUnlessManager(List.of(), viewer, null)).thenReturn(List.of());

			// Execution
			List<LessonActivityRecord> result = service.getLessonActivities(viewer, lessonId);

			// Verification
			assertThat(result).isEmpty();
			verify(accessPolicy, never()).requireEnrollment(any(), anyLong());
		}

		@Test
		void getLessonActivities_calledForUnpublishedLessonByLearner_throwsNotFoundTest() {
			// Given: an unpublished lesson — a plain learner may not preview it at all
			AppUser viewer = learnerViewer();
			Long lessonId = 15L;

			Lesson lesson = mock(Lesson.class);
			LessonPublicationStatus privateStatus = new LessonPublicationStatus();
			privateStatus.setCode(LessonPublicationStatusCode.PRIVATE);
			when(lesson.getPublicationStatus()).thenReturn(privateStatus);
			when(accessPolicy.requireLesson(lessonId)).thenReturn(lesson);

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
			when(assemblyService.getLessonActivitiesForUser(lessonId, viewer.internalId())).thenReturn(List.of());
			when(accessPolicy.redactQuizAnswersUnlessManager(List.of(), viewer, null)).thenReturn(List.of());

			// Execution
			List<LessonActivityRecord> result = service.getLessonActivities(viewer, lessonId);

			// Verification
			assertThat(result).isEmpty();
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

			Lesson lesson = publishedLesson();
			when(accessPolicy.requireLesson(lessonId)).thenReturn(lesson);

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

	private Lesson publishedLesson() {
		Lesson lesson = mock(Lesson.class);
		LessonPublicationStatus publishedStatus = new LessonPublicationStatus();
		publishedStatus.setCode(LessonPublicationStatusCode.PUBLISHED);
		when(lesson.getPublicationStatus()).thenReturn(publishedStatus);
		return lesson;
	}
}
