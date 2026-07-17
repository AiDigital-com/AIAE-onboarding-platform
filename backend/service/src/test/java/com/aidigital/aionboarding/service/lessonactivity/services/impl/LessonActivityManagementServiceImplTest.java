package com.aidigital.aionboarding.service.lessonactivity.services.impl;

import com.aidigital.aionboarding.domain.common.dictionary.LessonStatusCode;
import com.aidigital.aionboarding.domain.common.dictionary.entities.ActivityType;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonStatus;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.lessonactivity.models.ActivityPromptRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.GenerateActivityResultRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.LessonActivityRecord;
import com.aidigital.aionboarding.service.lessonactivity.services.LessonActivityAssemblyService;
import com.aidigital.aionboarding.service.lessonactivity.support.LessonActivityAccessPolicy;
import com.aidigital.aionboarding.service.lessonactivity.support.LessonActivityPayloadAssembler;
import com.aidigital.aionboarding.service.lessonactivity.support.LessonActivityRecordAssembler;
import com.aidigital.aionboarding.service.lessongen.model.ActivityRequest;
import com.aidigital.aionboarding.service.lessongen.model.GeneratedActivityResult;
import com.aidigital.aionboarding.service.lessongen.model.LessonGenPrompt;
import com.aidigital.aionboarding.service.lessongen.prompt.ActivityPromptBuilder;
import com.aidigital.aionboarding.service.lessongen.services.LessonGenService;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonActivityManagementServiceImplTest {

	@Mock
	private PermissionService permissionService;
	@Mock
	private ActivityPromptBuilder activityPromptBuilder;
	@Mock
	private LessonGenService lessonGenService;
	@Mock
	private LessonActivityAccessPolicy accessPolicy;
	@Mock
	private LessonActivityAssemblyService assemblyService;
	@Mock
	private LessonActivityPayloadAssembler payloadAssembler;
	@Mock
	private LessonActivityRecordAssembler lessonActivityMapper;
	@Mock
	private LessonActivityPersistenceHelper persistenceHelper;

	@Spy
	private CurrentTime currentTime = new CurrentTime();

	@InjectMocks
	private LessonActivityManagementServiceImpl service;

	@Test
	void generateActivity_called_callsPersistenceHelperSaveWithActivityEntity() {
		// Given:
		AppUser viewer = new AppUser(1L, "clerk-1", "admin@test.com", "Admin", "admin", "Admin", null, null, null);
		Long lessonId = 42L;

		Lesson lesson = mock(Lesson.class);
		LessonStatus readyStatus = new LessonStatus();
		readyStatus.setCode(LessonStatusCode.READY);
		when(lesson.getStatus()).thenReturn(readyStatus);
		when(lesson.getContentHtml()).thenReturn("some content");
		when(accessPolicy.requireLesson(lessonId)).thenReturn(lesson);

		ActivityRequest activityRequest = new ActivityRequest("QUIZ", 5, Map.of());
		when(activityPromptBuilder.normalizeActivityRequest("QUIZ", 5)).thenReturn(activityRequest);

		LessonGenPrompt prompt = new LessonGenPrompt("v1", "ck-1", "instructions", "input");
		when(activityPromptBuilder.buildLessonActivityPrompt(lesson, "QUIZ", 5)).thenReturn(prompt);

		Map<String, Object> rawPayload = Map.of("title", "Quiz 1", "items", java.util.List.of());
		GeneratedActivityResult generatedActivity = new GeneratedActivityResult(rawPayload, Map.of("model", "gpt-4o" +
				"-mini"));
		when(lessonGenService.generateLessonActivityPayload(prompt)).thenReturn(generatedActivity);

		Map<String, Object> normalizedPayload = Map.of("title", "Quiz 1", "items", java.util.List.of());
		when(activityPromptBuilder.normalizeGeneratedActivityPayload(rawPayload, activityRequest)).thenReturn(normalizedPayload);

		ActivityType activityType = mock(ActivityType.class);
		when(accessPolicy.requireActivityType("QUIZ")).thenReturn(activityType);
		when(payloadAssembler.stringVal(normalizedPayload.get("title"))).thenReturn("Quiz 1");
		when(payloadAssembler.getActivityItemCount("QUIZ", normalizedPayload)).thenReturn(3);

		com.aidigital.aionboarding.domain.lessonactivity.entities.LessonActivity savedActivity =
				mock(com.aidigital.aionboarding.domain.lessonactivity.entities.LessonActivity.class);
		when(persistenceHelper.save(any())).thenReturn(savedActivity);

		LessonActivityRecord activityRecord = mock(LessonActivityRecord.class);
		when(lessonActivityMapper.toActivityRecord(savedActivity, null)).thenReturn(activityRecord);
		ActivityPromptRecord promptRecord = mock(ActivityPromptRecord.class);
		when(lessonActivityMapper.toPromptRecord(prompt)).thenReturn(promptRecord);

		// Execution
		GenerateActivityResultRecord result = service.generateActivity(viewer, lessonId, "QUIZ", 5);

		// Verification
		verify(persistenceHelper).save(any());
		verify(lessonGenService).generateLessonActivityPayload(prompt);
	}
}
