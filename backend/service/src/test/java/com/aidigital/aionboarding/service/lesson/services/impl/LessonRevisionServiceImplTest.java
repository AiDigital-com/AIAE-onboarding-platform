package com.aidigital.aionboarding.service.lesson.services.impl;

import com.aidigital.aionboarding.domain.common.dictionary.LessonStatusCode;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonStatus;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.lesson.models.LessonDetailRecord;
import com.aidigital.aionboarding.service.lesson.models.LessonRevisionPromptRecord;
import com.aidigital.aionboarding.service.lesson.models.ReviseLessonInput;
import com.aidigital.aionboarding.service.lesson.models.RevisionBriefRecord;
import com.aidigital.aionboarding.service.lesson.models.RevisionProviderMetadataRecord;
import com.aidigital.aionboarding.service.lesson.prompt.LessonRevisionPromptBuilder;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonEntityService;
import com.aidigital.aionboarding.service.lesson.support.LessonRecordAssembler;
import com.aidigital.aionboarding.service.lesson.support.LessonRevisionMetadataMapper;
import com.aidigital.aionboarding.service.lesson.support.LessonRevisionRequestValidator;
import com.aidigital.aionboarding.service.lessongen.model.GeneratedContentResult;
import com.aidigital.aionboarding.service.lessongen.model.GeneratedRevisionBriefResult;
import com.aidigital.aionboarding.service.lessongen.model.LessonGenPrompt;
import com.aidigital.aionboarding.service.lessongen.services.LessonGenService;
import com.aidigital.aionboarding.service.material.models.PreparedMaterialsResult;
import com.aidigital.aionboarding.service.material.services.MaterialPreparationService;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonRevisionServiceImplTest {

	@Mock
	private LessonEntityService lessonEntityService;
	@Mock
	private PermissionService permissionService;
	@Mock
	private MaterialPreparationService materialPreparationService;
	@Mock
	private LessonRecordAssembler lessonMapper;
	@Mock
	private LessonRevisionPromptBuilder lessonRevisionPromptBuilder;
	@Mock
	private LessonGenService lessonGenService;
	@Mock
	private LessonRevisionRequestValidator lessonRevisionRequestValidator;
	@Mock
	private LessonRevisionMetadataMapper lessonRevisionMetadataMapper;
	@Mock
	private CurrentTime currentTime;

	@InjectMocks
	private LessonRevisionServiceImpl service;

	@BeforeEach
	void setUpCurrentTime() {
		lenient().when(currentTime.utcDateTime()).thenReturn(LocalDateTime.parse("2026-07-03T12:00:00"));
	}

	@Test
	void shouldCallLessonGenServiceForBothAiStepsTest() {
		// Given:
		AppUser viewer = new AppUser(1L, "clerk-1", "test@test.com", "Test User", "manager", "Test", null, null, null);
		Long lessonId = 42L;
		ReviseLessonInput request = new ReviseLessonInput("fix typos", List.of());

		Lesson lesson = stubReadyLesson(lessonId);
		stubHappyPathMocks(viewer, lessonId, lesson, request);

		// When:
		service.reviseLesson(viewer, lessonId, request);

		// Then:
		verify(lessonGenService).generateLessonRevisionBrief(any(LessonGenPrompt.class));
		verify(lessonGenService).generateLessonContent(any(LessonGenPrompt.class));
		verify(lessonEntityService).saveRevised(eq(lesson), any());
	}

	@Test
	void shouldCallSaveRevisedAfterBothAiCallsTest() {
		// Given:
		AppUser viewer = new AppUser(1L, "clerk-1", "test@test.com", "Test User", "manager", "Test", null, null, null);
		Long lessonId = 42L;
		ReviseLessonInput request = new ReviseLessonInput("restructure", List.of("tone"));

		Lesson lesson = stubReadyLesson(lessonId);
		stubHappyPathMocks(viewer, lessonId, lesson, request);

		// When:
		service.reviseLesson(viewer, lessonId, request);

		// Then:
		InOrder inOrder = inOrder(lessonGenService, lessonEntityService);
		inOrder.verify(lessonGenService).generateLessonRevisionBrief(any(LessonGenPrompt.class));
		inOrder.verify(lessonGenService).generateLessonContent(any(LessonGenPrompt.class));
		inOrder.verify(lessonEntityService).saveRevised(eq(lesson), any());
	}

	@Test
	void reviseLessonWhenFirstSaveAttemptConflicts_shouldRetryOnceAgainstAFreshlyReloadedLessonTest() {
		// Given: a concurrent editor advanced the lesson's version between this method's load and
		// its save; the AI calls already ran, so the retry re-applies their output onto fresh state.
		AppUser viewer = new AppUser(1L, "clerk-1", "test@test.com", "Test User", "manager", "Test", null, null, null);
		Long lessonId = 43L;
		ReviseLessonInput request = new ReviseLessonInput("fix typos", List.of());

		Lesson lesson = stubReadyLesson(lessonId);
		Lesson freshLesson = mock(Lesson.class);
		when(lessonEntityService.findByIdWithFetches(lessonId)).thenReturn(lesson, freshLesson);
		stubHappyPathMocks(viewer, lessonId, lesson, request);
		when(lessonRevisionMetadataMapper.mergeRevisionEntry(eq(freshLesson), any())).thenReturn(Map.of());
		Lesson savedLesson = mock(Lesson.class);
		when(lessonEntityService.saveRevised(eq(lesson), any()))
				.thenThrow(new ObjectOptimisticLockingFailureException(Lesson.class, lessonId));
		when(lessonEntityService.saveRevised(eq(freshLesson), any())).thenReturn(savedLesson);
		when(lessonMapper.toDetailRecord(savedLesson)).thenReturn(mock(LessonDetailRecord.class));

		// When:
		service.reviseLesson(viewer, lessonId, request);

		// Then:
		verify(lessonRevisionMetadataMapper).applyRevisedContent(lesson, "revised html");
		verify(lessonRevisionMetadataMapper).applyRevisedContent(freshLesson, "revised html");
		verify(lessonEntityService, times(2)).findByIdWithFetches(lessonId);
		verify(lessonEntityService).saveRevised(eq(freshLesson), any());
	}

	@Test
	void reviseLessonWhenBothSaveAttemptsConflict_shouldPropagateTheConflictTest() {
		// Given: a persistent double-conflict; silently retrying forever risks overwriting a
		// third concurrent change, so this surfaces as a client-visible conflict instead.
		AppUser viewer = new AppUser(1L, "clerk-1", "test@test.com", "Test User", "manager", "Test", null, null, null);
		Long lessonId = 44L;
		ReviseLessonInput request = new ReviseLessonInput("fix typos", List.of());

		Lesson lesson = stubReadyLesson(lessonId);
		Lesson freshLesson = mock(Lesson.class);
		when(lessonEntityService.findByIdWithFetches(lessonId)).thenReturn(lesson, freshLesson);
		stubHappyPathMocks(viewer, lessonId, lesson, request);
		lenient().when(lessonRevisionMetadataMapper.mergeRevisionEntry(eq(freshLesson), any())).thenReturn(Map.of());
		when(lessonEntityService.saveRevised(any(), any()))
				.thenThrow(new ObjectOptimisticLockingFailureException(Lesson.class, lessonId));

		// When-Then:
		assertThatThrownBy(() -> service.reviseLesson(viewer, lessonId, request))
				.isInstanceOf(ObjectOptimisticLockingFailureException.class);
		verify(lessonEntityService, times(2)).saveRevised(any(), any());
	}

	// -------------------------------------------------------------------------
	// Setup helpers
	// -------------------------------------------------------------------------

	private Lesson stubReadyLesson(Long lessonId) {
		Lesson lesson = mock(Lesson.class);
		LessonStatus readyStatus = new LessonStatus();
		readyStatus.setCode(LessonStatusCode.READY);
		when(lesson.getStatus()).thenReturn(readyStatus);
		when(lessonEntityService.findByIdWithFetches(lessonId)).thenReturn(lesson);
		return lesson;
	}

	private void stubHappyPathMocks(AppUser viewer, Long lessonId, Lesson lesson, ReviseLessonInput request) {
		// Permission and validation
		LessonRevisionRequestValidator.ValidatedRevisionRequest validated =
				new LessonRevisionRequestValidator.ValidatedRevisionRequest(
						request.revisionRequest(), request.selectedOptions() == null ? List.of() :
						request.selectedOptions());
		when(lessonRevisionRequestValidator.validate(eq(viewer), eq(lesson), eq(request))).thenReturn(validated);

		// Mapper and materials
		LessonDetailRecord lessonRecord = mock(LessonDetailRecord.class);
		when(lessonMapper.toDetailRecord(lesson)).thenReturn(lessonRecord);
		PreparedMaterialsResult preparedMaterials = mock(PreparedMaterialsResult.class);
		when(materialPreparationService.prepareForLesson(lessonId)).thenReturn(preparedMaterials);

		// Planner prompt
		LessonRevisionPromptRecord plannerPrompt =
				new LessonRevisionPromptRecord("v1", "ck-plan", "plan instructions", "plan input");
		when(lessonRevisionPromptBuilder.buildLessonRevisionPlannerPrompt(
				any(), any(), any(), any())).thenReturn(plannerPrompt);

		// Planner AI call
		Map<String, Object> briefMap = Map.of(
				"changeScope", "targeted",
				"userIntent", "fix typos",
				"editInstructions", List.of(),
				"preserveRules", List.of(),
				"riskNotes", List.of()
		);
		Map<String, Object> plannerMetadata = Map.of(
				"provider", "openai",
				"model", "gpt-4o-mini",
				"promptVersion", "v1",
				"promptCacheKey", "ck-plan",
				"rawOutput", "{}"
		);
		when(lessonGenService.generateLessonRevisionBrief(any(LessonGenPrompt.class)))
				.thenReturn(new GeneratedRevisionBriefResult(briefMap, plannerMetadata));

		RevisionBriefRecord revisionBrief = new RevisionBriefRecord(
				"targeted", "fix typos", List.of(), List.of(), List.of());
		when(lessonRevisionMetadataMapper.buildRevisionBrief(briefMap)).thenReturn(revisionBrief);

		// Writer prompt
		LessonRevisionPromptRecord writerPrompt =
				new LessonRevisionPromptRecord("v1", "ck-write", "write instructions", "write input");
		when(lessonRevisionPromptBuilder.buildLessonRevisionWriterPrompt(
				any(), any(), any(), any(), any())).thenReturn(writerPrompt);

		// Writer AI call
		Map<String, Object> writerMetadata = Map.of(
				"provider", "openai",
				"model", "gpt-4o-mini",
				"promptVersion", "v1",
				"promptCacheKey", "ck-write",
				"rawOutput", ""
		);
		when(lessonGenService.generateLessonContent(any(LessonGenPrompt.class)))
				.thenReturn(new GeneratedContentResult("revised html", writerMetadata));

		// Provider metadata mapping stubs (needed for both planner and writer results)
		RevisionProviderMetadataRecord plannerProviderMetadata = new RevisionProviderMetadataRecord(
				"openai", "gpt-4o-mini", "v1", "ck-plan", "{}");
		when(lessonRevisionMetadataMapper.buildProviderMetadata(plannerMetadata)).thenReturn(plannerProviderMetadata);
		RevisionProviderMetadataRecord writerProviderMetadata = new RevisionProviderMetadataRecord(
				"openai", "gpt-4o-mini", "v1", "ck-write", "");
		when(lessonRevisionMetadataMapper.buildProviderMetadata(writerMetadata)).thenReturn(writerProviderMetadata);

		// Metadata mapper and save — lenient because the conflict-retry tests override
		// saveRevised(lesson, ...) to throw instead of returning this default.
		when(lessonRevisionMetadataMapper.mergeRevisionEntry(eq(lesson), any())).thenReturn(Map.of());
		Lesson savedLesson = mock(Lesson.class);
		lenient().when(lessonEntityService.saveRevised(eq(lesson), any())).thenReturn(savedLesson);
		LessonDetailRecord savedRecord = mock(LessonDetailRecord.class);
		lenient().when(lessonMapper.toDetailRecord(savedLesson)).thenReturn(savedRecord);
	}
}
