package com.aidigital.aionboarding.service.lessonactivity.services.impl;

import com.aidigital.aionboarding.domain.common.dictionary.ActivityTypeCode;
import com.aidigital.aionboarding.domain.common.dictionary.LessonPublicationStatusCode;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.learning.services.LearningService;
import com.aidigital.aionboarding.service.lessonactivity.models.ActivityAttemptRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.GenerateActivityResultRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.LessonActivityRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.LessonActivityWithAttemptsRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.SubmitActivityProgressResultRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.UpdateActivityInput;
import com.aidigital.aionboarding.service.lessonactivity.models.UpdateActivityResultRecord;
import com.aidigital.aionboarding.service.lessonactivity.services.LessonActivityAssemblyService;
import com.aidigital.aionboarding.service.lessonactivity.services.LessonActivityManagementService;
import com.aidigital.aionboarding.service.lessonactivity.services.LessonActivityProgressService;
import com.aidigital.aionboarding.service.lessonactivity.services.LessonActivityService;
import com.aidigital.aionboarding.service.lessonactivity.support.LessonActivityAccessPolicy;
import com.aidigital.aionboarding.service.lessonactivity.support.LessonActivityPayloadAssembler;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LessonActivityServiceImpl implements LessonActivityService {

	private final PermissionService permissionService;
	private final LearningService learningService;
	private final LessonActivityAccessPolicy accessPolicy;
	private final LessonActivityProgressService progressService;
	private final LessonActivityManagementService managementService;
	private final LessonActivityAssemblyService assemblyService;
	private final LessonActivityPayloadAssembler payloadAssembler;

	@Override
	@Transactional(readOnly = true)
	public List<LessonActivityRecord> getLessonActivities(AppUser viewer, Long lessonId) {
		// Visibility is already fully gated by requireVisibleLesson (published, or an
		// admin/manager preview) — a viewer who can see the lesson may also see its activity
		// list read-only, even before enrolling. Correct-answer keys stay protected separately
		// via redactQuizAnswersUnlessManager, and submitting/resetting progress remains gated by
		// requireEnrollment on those dedicated endpoints.
		Lesson lesson = requireVisibleLesson(viewer, lessonId);
		List<LessonActivityRecord> activities = assemblyService.getLessonActivitiesForUser(lessonId,
				viewer.internalId());
		return accessPolicy.redactQuizAnswersUnlessManager(activities, viewer, creatorId(lesson));
	}

	@Override
	@Transactional(readOnly = true)
	public LessonActivityWithAttemptsRecord getLessonActivity(AppUser viewer, Long lessonId, Long activityId) {
		Lesson lesson = requireVisibleLesson(viewer, lessonId);
		LessonActivityRecord activity = assemblyService.getLessonActivity(lessonId, activityId, viewer.internalId());
		if (activity == null) {
			throw new AppException(ErrorReason.C001, activityId);
		}
		List<ActivityAttemptRecord> attempts =
				assemblyService.getAttemptsForActivity(lessonId, activityId, viewer.internalId());
		return new LessonActivityWithAttemptsRecord(
				accessPolicy.redactQuizAnswersUnlessManager(activity, viewer, creatorId(lesson)),
				attempts
		);
	}

	Long creatorId(Lesson lesson) {
		return lesson.getCreatedByUser() == null ? null : lesson.getCreatedByUser().getId();
	}

	@Override
	public GenerateActivityResultRecord generateActivity(AppUser viewer, Long lessonId, String type, Integer count) {
		return managementService.generateActivity(viewer, lessonId, type, count);
	}

	@Override
	@Transactional
	public UpdateActivityResultRecord updateActivity(
			AppUser viewer,
			Long lessonId,
			Long activityId,
			UpdateActivityInput request
	) {
		return managementService.updateActivity(viewer, lessonId, activityId, request);
	}

	@Override
	@Transactional
	public SubmitActivityProgressResultRecord submitActivityProgress(
			AppUser viewer,
			Long lessonId,
			Long activityId,
			Map<String, Object> request
	) {
		permissionService.requirePermission(viewer, PermissionKeys.LEARNING_COMPLETE);
		String type = payloadAssembler.stringVal(request.get("type"));
		if (!ActivityTypeCode.QUIZ.equals(type) && !ActivityTypeCode.FLASHCARDS.equals(type)) {
			throw new AppException(ErrorReason.C002, "Unsupported activity type.");
		}

		accessPolicy.requireEnrollment(viewer, lessonId);
		Lesson lesson = accessPolicy.requirePublishedReadyLesson(lessonId);

		var result = ActivityTypeCode.QUIZ.equals(type)
				? progressService.completeQuiz(
				viewer,
				lesson,
				activityId,
				payloadAssembler.asListOfStringLists(request.get("answers"))
		)
				: progressService.completeFlashcards(viewer, lesson, activityId, request);

		return new SubmitActivityProgressResultRecord(
				true,
				result.progress(),
				result.activities(),
				result.enrollment(),
				result.lessonCompleted(),
				result.attempt(),
				result.lessonCompleted()
						? learningService.getCompletedRoadmapsForUserLesson(viewer.internalId(), lessonId)
						: List.of()
		);
	}

	@Override
	@Transactional
	public void resetActivityProgress(AppUser viewer, Long lessonId, Long activityId) {
		permissionService.requirePermission(viewer, PermissionKeys.LEARNING_COMPLETE);
		accessPolicy.requireEnrollment(viewer, lessonId);
		accessPolicy.requirePublishedReadyLesson(lessonId);
		progressService.resetProgress(viewer, lessonId, activityId);
	}

	@Override
	@Transactional
	public List<LessonActivityRecord> deleteActivity(AppUser viewer, Long lessonId, Long activityId) {
		return managementService.deleteActivity(viewer, lessonId, activityId);
	}

	Lesson requireVisibleLesson(AppUser viewer, Long lessonId) {
		Lesson lesson = accessPolicy.requireLesson(lessonId);
		if (!canViewLesson(viewer, lesson)) {
			throw new AppException(ErrorReason.C001, lessonId);
		}
		return lesson;
	}

	boolean canViewLesson(AppUser viewer, Lesson lesson) {
		if (viewer.isAdmin()) {
			return true;
		}
		if (permissionService.userHasPermission(viewer, PermissionKeys.LESSONS_MANAGE)
				&& permissionService.canManageExistingLesson(viewer,
				lesson.getCreatedByUser() == null ? null : lesson.getCreatedByUser().getId())) {
			return true;
		}
		return LessonPublicationStatusCode.PUBLISHED.equals(lesson.getPublicationStatus().getCode());
	}
}
