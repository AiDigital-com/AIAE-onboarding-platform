package com.aidigital.aionboarding.service.lessonactivity.services.impl;

import com.aidigital.aionboarding.domain.common.dictionary.ActivityTypeCode;
import com.aidigital.aionboarding.domain.common.dictionary.LessonStatusCode;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.lessonactivity.entities.LessonActivity;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.lessonactivity.models.GenerateActivityResultRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.LessonActivityRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.UpdateActivityInput;
import com.aidigital.aionboarding.service.lessonactivity.models.UpdateActivityResultRecord;
import com.aidigital.aionboarding.service.lessonactivity.services.LessonActivityAssemblyService;
import com.aidigital.aionboarding.service.lessonactivity.services.LessonActivityManagementService;
import com.aidigital.aionboarding.service.lessonactivity.support.LessonActivityAccessPolicy;
import com.aidigital.aionboarding.service.lessonactivity.support.LessonActivityPayloadAssembler;
import com.aidigital.aionboarding.service.lessonactivity.support.LessonActivityRecordAssembler;
import com.aidigital.aionboarding.service.lessongen.model.ActivityRequest;
import com.aidigital.aionboarding.service.lessongen.model.GeneratedActivityResult;
import com.aidigital.aionboarding.service.lessongen.model.LessonGenPrompt;
import com.aidigital.aionboarding.service.lessongen.prompt.ActivityPromptBuilder;
import com.aidigital.aionboarding.service.lessongen.services.LessonGenService;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LessonActivityManagementServiceImpl implements LessonActivityManagementService {

	private final PermissionService permissionService;
	private final ActivityPromptBuilder activityPromptBuilder;
	private final LessonGenService lessonGenService;
	private final LessonActivityAccessPolicy accessPolicy;
	private final LessonActivityAssemblyService assemblyService;
	private final LessonActivityPayloadAssembler payloadAssembler;
	private final LessonActivityRecordAssembler lessonActivityMapper;
	private final LessonActivityPersistenceHelper persistenceHelper;
	private final CurrentTime currentTime;

	@Override
	public GenerateActivityResultRecord generateActivity(AppUser viewer, Long lessonId, String type, Integer count) {
		permissionService.requirePermission(viewer, PermissionKeys.LESSONS_MANAGE_ACTIVITIES);
		Lesson lesson = accessPolicy.requireLesson(lessonId);
		accessPolicy.requireActivityManager(viewer, lesson);

		if (!LessonStatusCode.READY.equals(lesson.getStatus().getCode())) {
			throw new AppException(ErrorReason.C002, "Only ready lessons can have generated activities.");
		}
		if (lesson.getContentHtml().isBlank() && lesson.getContentMarkdown().isBlank()) {
			throw new AppException(ErrorReason.C002, "Lesson content is empty.");
		}

		ActivityRequest activityRequest = activityPromptBuilder.normalizeActivityRequest(type, count);
		LessonGenPrompt prompt = activityPromptBuilder.buildLessonActivityPrompt(
				lesson,
				activityRequest.type(),
				activityRequest.count()
		);
		GeneratedActivityResult generatedActivity = lessonGenService.generateLessonActivityPayload(prompt);
		Map<String, Object> payload = activityPromptBuilder.normalizeGeneratedActivityPayload(
				generatedActivity.payload(),
				activityRequest
		);

		LessonActivity activity = new LessonActivity();
		activity.setLesson(lesson);
		activity.setType(accessPolicy.requireActivityType(activityRequest.type()));
		activity.setTitle(payloadAssembler.stringVal(payload.get("title")));
		activity.setItemCount(payloadAssembler.getActivityItemCount(activityRequest.type(), payload));
		activity.setPayload(payload);
		activity.setGenerationMetadata(generatedActivity.metadata());
		activity.setCreatedBy(viewer.name());
		activity.setCreatedAt(currentTime.utcDateTime());

		return new GenerateActivityResultRecord(
				lessonActivityMapper.toActivityRecord(persistenceHelper.save(activity), null),
				lessonActivityMapper.toPromptRecord(prompt)
		);
	}

	@Override
	@Transactional
	public UpdateActivityResultRecord updateActivity(
			AppUser viewer,
			Long lessonId,
			Long activityId,
			UpdateActivityInput request
	) {
		permissionService.requirePermission(viewer, PermissionKeys.LESSONS_MANAGE_ACTIVITIES);
		Lesson lesson = accessPolicy.requireLesson(lessonId);
		accessPolicy.requireActivityManager(viewer, lesson);

		LessonActivity activity = persistenceHelper.findByLessonIdAndId(lessonId, activityId);

		Map<String, Object> payload = ActivityTypeCode.FLASHCARDS.equals(activity.getType().getCode())
				? activityPromptBuilder.normalizeFlashcardsUpdatePayload(request)
				: activityPromptBuilder.normalizeQuizUpdatePayload(request);

		activity.setTitle(payloadAssembler.stringVal(payload.get("title")));
		activity.setItemCount(payloadAssembler.getActivityItemCount(activity.getType().getCode(), payload));
		activity.setPayload(payload);
		persistenceHelper.save(activity);

		return new UpdateActivityResultRecord(
				lessonActivityMapper.toActivityRecord(activity, null),
				lessonActivityMapper.toLessonWithActivitiesRecord(lesson,
						assemblyService.getLessonActivities(lesson.getId()))
		);
	}

	@Override
	@Transactional
	public List<LessonActivityRecord> deleteActivity(AppUser viewer, Long lessonId, Long activityId) {
		permissionService.requirePermission(viewer, PermissionKeys.LESSONS_MANAGE_ACTIVITIES);
		Lesson lesson = accessPolicy.requireLesson(lessonId);
		accessPolicy.requireActivityManager(viewer, lesson);

		LessonActivity activity = persistenceHelper.findByLessonIdAndId(lessonId, activityId);
		persistenceHelper.deleteActivity(activity);

		return assemblyService.getLessonActivities(lesson.getId());
	}

}
