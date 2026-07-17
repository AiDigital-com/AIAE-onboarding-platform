package com.aidigital.aionboarding.service.lesson.services.impl;

import com.aidigital.aionboarding.domain.common.dictionary.LessonPublicationStatusCode;
import com.aidigital.aionboarding.domain.common.dictionary.LessonStatusCode;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonPublicationStatus;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonStatus;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.lesson.entities.LessonAssistantConversation;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.observability.SecurityMetrics;
import com.aidigital.aionboarding.service.common.observability.enums.ContinuationRejectionReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.learning.services.entity.LearningEnrollmentEntityService;
import com.aidigital.aionboarding.service.lesson.enums.LessonAssistantPreset;
import com.aidigital.aionboarding.service.lesson.models.AskLessonResultRecord;
import com.aidigital.aionboarding.service.lesson.models.LessonAssistantConversationRecord;
import com.aidigital.aionboarding.service.lesson.prompt.LessonAssistantPromptBuilder;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonAssistantConversationEntityService;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonEntityService;
import com.aidigital.aionboarding.service.lesson.support.LessonAssistantConversationAssembler;
import com.aidigital.aionboarding.service.lesson.support.LessonRecordAssembler;
import com.aidigital.aionboarding.service.lessongen.model.GeneratedContentResult;
import com.aidigital.aionboarding.service.lessongen.model.LessonGenPrompt;
import com.aidigital.aionboarding.service.lessongen.services.LessonGenService;
import com.aidigital.aionboarding.service.material.models.PreparedMaterialsResult;
import com.aidigital.aionboarding.service.material.services.MaterialPreparationService;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import com.aidigital.aionboarding.service.user.services.entity.UserEntityService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonAssistantServiceImplTest {

	@Mock
	private LessonEntityService lessonEntityService;
	@Mock
	private LearningEnrollmentEntityService learningEnrollmentEntityService;
	@Mock
	private PermissionService permissionService;
	@Mock
	private MaterialPreparationService materialPreparationService;
	@Mock
	private LessonRecordAssembler lessonMapper;
	@Mock
	private LessonGenService lessonGenService;
	@Mock
	private LessonAssistantPromptBuilder lessonAssistantPromptBuilder;
	@Mock
	private UserEntityService userEntityService;
	@Mock
	private LessonAssistantConversationEntityService lessonAssistantConversationEntityService;
	@Mock
	private LessonAssistantConversationAssembler lessonAssistantConversationAssembler;
	@Mock
	private SecurityMetrics securityMetrics;
	@Mock
	private CurrentTime currentTime;

	@InjectMocks
	private LessonAssistantServiceImpl service;

	@Nested
	class ask {

		@Test
		void ask_calledWithFileInputs_passesFileInputsToLessonGenService() {
			// Given:
			AppUser viewer = viewer();
			Long lessonId = 10L;
			String question = "What is this lesson about?";

			List<Map<String, Object>> fileInputs = List.of(
					Map.of("type", "input_file", "file_id", "file-abc123")
			);
			Map<String, Object> promptMap = Map.of(
					"version", "lesson-reader-assistant-v1",
					"cacheKey", "ck-lesson-10",
					"instructions", "You are an assistant.",
					"input", "User question: " + question,
					"fileInputs", fileInputs
			);

			stubHappyPath(viewer, lessonId, promptMap,
					new GeneratedContentResult("Here is what this lesson is about.", Map.of(
							"provider", "openai",
							"model", "gpt-4o-mini",
							"promptVersion", "lesson-reader-assistant-v1",
							"promptCacheKey", "ck-lesson-10"
					)));

			ArgumentCaptor<LessonGenPrompt> promptCaptor = ArgumentCaptor.forClass(LessonGenPrompt.class);

			// Execution
			service.ask(viewer, lessonId, question, List.of(), LessonAssistantPreset.REGULAR);

			// Verification
			verify(lessonGenService).generateLessonContent(promptCaptor.capture());
			assertThat(promptCaptor.getValue().fileInputs()).hasSize(1);
			assertThat(promptCaptor.getValue().fileInputs().get(0).get("file_id")).isEqualTo("file-abc123");
		}

		@Test
		void ask_calledWithoutFileInputs_callsLessonGenServiceWithEmptyFileInputs() {
			// Given:
			AppUser viewer = viewer();
			Long lessonId = 20L;
			String question = "Explain the main concept.";

			Map<String, Object> promptMap = Map.of(
					"version", "lesson-reader-assistant-v1",
					"cacheKey", "ck-lesson-20",
					"instructions", "You are an assistant.",
					"input", "User question: " + question,
					"fileInputs", List.of()
			);

			stubHappyPath(viewer, lessonId, promptMap,
					new GeneratedContentResult("The main concept is X.", Map.of(
							"provider", "openai",
							"model", "gpt-4o-mini",
							"promptVersion", "lesson-reader-assistant-v1",
							"promptCacheKey", "ck-lesson-20"
					)));

			ArgumentCaptor<LessonGenPrompt> promptCaptor = ArgumentCaptor.forClass(LessonGenPrompt.class);

			// Execution
			service.ask(viewer, lessonId, question, List.of(), LessonAssistantPreset.REGULAR);

			// Verification
			verify(lessonGenService).generateLessonContent(promptCaptor.capture());
			assertThat(promptCaptor.getValue().fileInputs()).isEmpty();
		}

		@Test
		void ask_lessonGenServiceReturnsResult_returnsAssembledResponse() {
			// Given:
			AppUser viewer = viewer();
			Long lessonId = 30L;
			String question = "Summarize the lesson.";

			List<Map<String, Object>> fileInputs = List.of(
					Map.of("type", "input_file", "file_id", "file-xyz")
			);
			Map<String, Object> promptMap = Map.of(
					"version", "lesson-reader-assistant-v1",
					"cacheKey", "ck-lesson-30",
					"instructions", "You are an assistant.",
					"input", "User question: " + question,
					"fileInputs", fileInputs
			);

			Map<String, Object> resultMetadata = Map.of(
					"provider", "openai",
					"model", "gpt-4o-mini",
					"promptVersion", "lesson-reader-assistant-v1",
					"promptCacheKey", "ck-lesson-30"
			);

			stubHappyPath(viewer, lessonId, promptMap,
					new GeneratedContentResult("The lesson covers topic A, B, and C.", resultMetadata));

			// Execution
			AskLessonResultRecord result =
					service.ask(viewer, lessonId, question, List.of(), LessonAssistantPreset.REGULAR);

			// Verification
			assertThat(result.answer()).isEqualTo("The lesson covers topic A, B, and C.");
			assertThat(result.metadata()).containsEntry("provider", "openai");
			assertThat(result.metadata()).containsEntry("model", "gpt-4o-mini");
			assertThat(result.metadata()).containsEntry("promptVersion", "lesson-reader-assistant-v1");
			assertThat(result.metadata()).containsEntry("attachedFileCount", 1);
		}

		@Test
		void askWhenNoSavedConversationExists_shouldBuildFullPromptAndNotChainOntoAnyResponseTest() {
			// Given:
			AppUser viewer = viewer();
			Long lessonId = 40L;
			String question = "What is this lesson about?";

			Map<String, Object> promptMap = Map.of(
					"version", "lesson-reader-assistant-v1",
					"cacheKey", "ck-lesson-40",
					"instructions", "You are an assistant.",
					"input", "User question: " + question,
					"fileInputs", List.of()
			);
			stubHappyPath(viewer, lessonId, promptMap,
					new GeneratedContentResult("Answer.", Map.of("responseId", "resp-1")));

			ArgumentCaptor<LessonGenPrompt> promptCaptor = ArgumentCaptor.forClass(LessonGenPrompt.class);

			// When:
			service.ask(viewer, lessonId, question, List.of(), LessonAssistantPreset.REGULAR);

			// Then:
			verify(materialPreparationService).prepareForLesson(lessonId);
			verify(lessonAssistantPromptBuilder).buildLessonAssistantPrompt(
					any(), any(), anyString(), any(), any(), eq(false));
			verify(lessonGenService).generateLessonContent(promptCaptor.capture());
			assertThat(promptCaptor.getValue().store()).isTrue();
			assertThat(promptCaptor.getValue().previousResponseId()).isNull();
		}

		@Test
		void askWhenSavedConversationHasAStoredResponseId_shouldSkipMaterialPreparationAndChainOntoStoredResponseTest() {
			// Given: the client sends no continuation hint at all — the server alone decides
			// this is a follow-up, based only on its own saved conversation row.
			AppUser viewer = viewer();
			Long lessonId = 50L;
			String question = "Continue with the next part.";
			String storedResponseId = "resp-previous-1";

			Map<String, Object> promptMap = Map.of(
					"version", "lesson-reader-assistant-v1",
					"cacheKey", "ck-lesson-50",
					"instructions", "You are an assistant.",
					"input", "User question: " + question,
					"fileInputs", List.of()
			);
			stubHappyPath(viewer, lessonId, promptMap,
					new GeneratedContentResult("Next part.", Map.of("responseId", "resp-2")));

			LessonAssistantConversation existing = new LessonAssistantConversation();
			existing.setId(500L);
			existing.setLastResponseId(storedResponseId);
			when(lessonAssistantConversationEntityService.findByUserIdAndLessonId(viewer.internalId(), lessonId))
					.thenReturn(Optional.of(existing));

			ArgumentCaptor<LessonGenPrompt> promptCaptor = ArgumentCaptor.forClass(LessonGenPrompt.class);

			// When:
			service.ask(viewer, lessonId, question, List.of(), LessonAssistantPreset.SMALL_PORTIONS);

			// Then:
			verify(materialPreparationService, never()).prepareForLesson(any());
			verify(lessonAssistantPromptBuilder).buildLessonAssistantPrompt(
					any(), any(), anyString(), any(), any(), eq(true));
			verify(lessonGenService).generateLessonContent(promptCaptor.capture());
			assertThat(promptCaptor.getValue().store()).isTrue();
			assertThat(promptCaptor.getValue().previousResponseId()).isEqualTo(storedResponseId);
		}

		@Test
		void askShouldLoadContinuationStateScopedExactlyToTheAuthenticatedViewerAndThisLessonTest() {
			// Given: there is no client-supplied continuation parameter at all any more — the
			// only way a cross-user/cross-lesson leak could happen is the server looking up the
			// wrong (userId, lessonId) pair itself.
			AppUser viewer = viewer();
			Long lessonId = 55L;
			String question = "What is this lesson about?";

			Map<String, Object> promptMap = Map.of(
					"version", "lesson-reader-assistant-v1",
					"cacheKey", "ck-lesson-55",
					"instructions", "You are an assistant.",
					"input", "User question: " + question,
					"fileInputs", List.of()
			);
			stubHappyPath(viewer, lessonId, promptMap, new GeneratedContentResult("Answer.", Map.of()));

			// When:
			service.ask(viewer, lessonId, question, List.of(), LessonAssistantPreset.REGULAR);

			// Then:
			verify(lessonAssistantConversationEntityService).findByUserIdAndLessonId(viewer.internalId(), lessonId);
		}

		@Test
		void askResultMetadataShouldNotExposeTheProviderResponseIdToTheClientTest() {
			// Given:
			AppUser viewer = viewer();
			Long lessonId = 56L;
			String question = "What is this lesson about?";

			Map<String, Object> promptMap = Map.of(
					"version", "lesson-reader-assistant-v1",
					"cacheKey", "ck-lesson-56",
					"instructions", "You are an assistant.",
					"input", "User question: " + question,
					"fileInputs", List.of()
			);
			stubHappyPath(viewer, lessonId, promptMap,
					new GeneratedContentResult("Answer.", Map.of("responseId", "resp-secret")));

			// When:
			AskLessonResultRecord result = service.ask(viewer, lessonId, question, List.of(),
					LessonAssistantPreset.REGULAR);

			// Then:
			assertThat(result.metadata()).doesNotContainKey("responseId");
		}

		@Test
		void askOnSuccess_shouldSaveQuestionAndAnswerToTheLearnersConversationTest() {
			// Given:
			AppUser viewer = viewer();
			Long lessonId = 60L;
			String question = "What is this lesson about?";

			Map<String, Object> promptMap = Map.of(
					"version", "lesson-reader-assistant-v1",
					"cacheKey", "ck-lesson-60",
					"instructions", "You are an assistant.",
					"input", "User question: " + question,
					"fileInputs", List.of()
			);
			stubHappyPath(viewer, lessonId, promptMap,
					new GeneratedContentResult("This lesson covers onboarding.", Map.of("responseId", "resp-60")));

			// When:
			service.ask(viewer, lessonId, question, List.of(), LessonAssistantPreset.SMALL_PORTIONS);

			// Then:
			verify(lessonAssistantConversationAssembler)
					.appendTurn(null, question, "This lesson covers onboarding.");
			ArgumentCaptor<LessonAssistantConversation> savedCaptor =
					ArgumentCaptor.forClass(LessonAssistantConversation.class);
			verify(lessonAssistantConversationEntityService).save(savedCaptor.capture());
			assertThat(savedCaptor.getValue().getPreset()).isEqualTo("small_portions");
			assertThat(savedCaptor.getValue().getLastResponseId()).isEqualTo("resp-60");
		}

		@Test
		void askWhenFirstSaveAttemptHitsAConcurrentTabConflict_shouldRetryOnceAgainstFreshStateTest() {
			// Given: two tabs raced to save a turn onto the same conversation; the first attempt
			// in this call loses the optimistic-lock check, so the turn is retried against a
			// freshly reloaded row instead of being dropped.
			AppUser viewer = viewer();
			Long lessonId = 100L;
			String question = "Explain more.";

			Map<String, Object> promptMap = Map.of(
					"version", "lesson-reader-assistant-v1",
					"cacheKey", "ck-lesson-100",
					"instructions", "You are an assistant.",
					"input", "User question: " + question,
					"fileInputs", List.of()
			);
			stubHappyPath(viewer, lessonId, promptMap,
					new GeneratedContentResult("More detail.", Map.of("responseId", "resp-100")));

			LessonAssistantConversation freshFromOtherTab = new LessonAssistantConversation();
			freshFromOtherTab.setId(700L);
			when(lessonAssistantConversationEntityService.findByUserIdAndLessonId(viewer.internalId(), lessonId))
					.thenReturn(Optional.empty(), Optional.of(freshFromOtherTab));
			when(lessonAssistantConversationEntityService.save(any()))
					.thenThrow(new ObjectOptimisticLockingFailureException(LessonAssistantConversation.class, 700L))
					.thenAnswer(invocation -> invocation.getArgument(0));

			// When:
			AskLessonResultRecord result =
					service.ask(viewer, lessonId, question, List.of(), LessonAssistantPreset.REGULAR);

			// Then:
			assertThat(result.answer()).isEqualTo("More detail.");
			verify(lessonAssistantConversationEntityService, times(2)).save(any());
			verify(lessonAssistantConversationEntityService, times(2))
					.findByUserIdAndLessonId(viewer.internalId(), lessonId);
			verify(securityMetrics, never()).invalidContinuation(any());
		}

		@Test
		void askWhenBothSaveAttemptsConflict_shouldRecordStaleVersionMetricButStillReturnTheAnswerTest() {
			// Given: a persistent double-conflict (e.g. a third concurrent writer racing the
			// retry too) — the live answer must still succeed for this caller even though the
			// turn cannot be persisted to history.
			AppUser viewer = viewer();
			Long lessonId = 101L;
			String question = "Explain more.";

			Map<String, Object> promptMap = Map.of(
					"version", "lesson-reader-assistant-v1",
					"cacheKey", "ck-lesson-101",
					"instructions", "You are an assistant.",
					"input", "User question: " + question,
					"fileInputs", List.of()
			);
			stubHappyPath(viewer, lessonId, promptMap,
					new GeneratedContentResult("More detail.", Map.of("responseId", "resp-101")));

			when(lessonAssistantConversationEntityService.save(any()))
					.thenThrow(new ObjectOptimisticLockingFailureException(LessonAssistantConversation.class, 700L));

			// When:
			AskLessonResultRecord result =
					service.ask(viewer, lessonId, question, List.of(), LessonAssistantPreset.REGULAR);

			// Then:
			assertThat(result.answer()).isEqualTo("More detail.");
			verify(lessonAssistantConversationEntityService, times(2)).save(any());
			verify(securityMetrics).invalidContinuation(ContinuationRejectionReason.STALE_VERSION);
		}
	}

	@Nested
	class getConversation {

		@Test
		void getConversationWhenNoneSaved_shouldReturnEmptyConversationTest() {
			// Given:
			AppUser viewer = viewer();
			Long lessonId = 70L;
			when(learningEnrollmentEntityService.findUserLessonByUserIdAndLessonId(viewer.internalId(), lessonId))
					.thenReturn(Optional.of(mock(com.aidigital.aionboarding.domain.learning.entities.UserLesson.class)));
			when(lessonAssistantConversationEntityService.findByUserIdAndLessonId(viewer.internalId(), lessonId))
					.thenReturn(Optional.empty());

			// When:
			var result = service.getConversation(viewer, lessonId);

			// Then:
			assertThat(result.messages()).isEmpty();
			assertThat(result.preset()).isEqualTo("regular");
		}

		@Test
		void getConversationWhenSaved_shouldReturnAssembledRecordTest() {
			// Given:
			AppUser viewer = viewer();
			Long lessonId = 71L;
			var conversation = new LessonAssistantConversation();
			var expectedRecord = new LessonAssistantConversationRecord(
					List.of(new com.aidigital.aionboarding.service.lesson.models.ChatTurn("user", "Hi")), "regular");
			when(learningEnrollmentEntityService.findUserLessonByUserIdAndLessonId(viewer.internalId(), lessonId))
					.thenReturn(Optional.of(mock(com.aidigital.aionboarding.domain.learning.entities.UserLesson.class)));
			when(lessonAssistantConversationEntityService.findByUserIdAndLessonId(viewer.internalId(), lessonId))
					.thenReturn(Optional.of(conversation));
			when(lessonAssistantConversationAssembler.toRecord(conversation)).thenReturn(expectedRecord);

			// When:
			var result = service.getConversation(viewer, lessonId);

			// Then:
			assertThat(result).isSameAs(expectedRecord);
		}

		@Test
		void getConversationWhenNotEnrolled_shouldThrowTest() {
			// Given:
			AppUser viewer = viewer();
			Long lessonId = 72L;
			when(learningEnrollmentEntityService.findUserLessonByUserIdAndLessonId(viewer.internalId(), lessonId))
					.thenReturn(Optional.empty());

			// When-Then:
			org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.getConversation(viewer, lessonId))
					.isInstanceOf(AppException.class);
		}
	}

	@Nested
	class clearConversation {

		@Test
		void clearConversation_shouldDeleteTheLearnersConversationTest() {
			// Given:
			AppUser viewer = viewer();
			Long lessonId = 80L;

			// When:
			service.clearConversation(viewer, lessonId);

			// Then:
			verify(lessonAssistantConversationEntityService)
					.deleteByUserIdAndLessonId(viewer.internalId(), lessonId);
		}
	}

	// -------------------------------------------------------------------------
	// Setup helpers
	// -------------------------------------------------------------------------

	private AppUser viewer() {
		return new AppUser(1L, "clerk-1", "user@example.com", "Test User", "learner", "User", null, null, null);
	}

	private void stubHappyPath(AppUser viewer, Long lessonId, Map<String, Object> promptMap,
	                           GeneratedContentResult genResult) {
		// Enrollment check passes
		when(learningEnrollmentEntityService.findUserLessonByUserIdAndLessonId(viewer.internalId(), lessonId))
				.thenReturn(Optional.of(mock(com.aidigital.aionboarding.domain.learning.entities.UserLesson.class)));

		// Lesson is READY + PUBLISHED
		Lesson lesson = mock(Lesson.class);
		LessonStatus readyStatus = new LessonStatus();
		readyStatus.setCode(LessonStatusCode.READY);
		LessonPublicationStatus publishedStatus = new LessonPublicationStatus();
		publishedStatus.setCode(LessonPublicationStatusCode.PUBLISHED);
		when(lesson.getStatus()).thenReturn(readyStatus);
		when(lesson.getPublicationStatus()).thenReturn(publishedStatus);
		when(lessonEntityService.findByIdWithFetches(lessonId)).thenReturn(lesson);

		// Mapper returns empty map
		when(lessonMapper.toDetailMap(lesson)).thenReturn(Map.of());

		// Material preparation - lenient since follow-up-mode tests never invoke this
		PreparedMaterialsResult preparedMaterials = mock(PreparedMaterialsResult.class);
		lenient().when(preparedMaterials.toLegacyMap()).thenReturn(Map.of("materials", List.of()));
		lenient().when(materialPreparationService.prepareForLesson(lessonId)).thenReturn(preparedMaterials);

		// Prompt builder returns the specified prompt map
		when(lessonAssistantPromptBuilder.buildLessonAssistantPrompt(
				any(), any(), anyString(), any(), any(), anyBoolean())).thenReturn(promptMap);

		// LessonGenService returns the specified result
		when(lessonGenService.generateLessonContent(any(LessonGenPrompt.class))).thenReturn(genResult);

		// Conversation persistence - lenient since not every test asserts on it or overrides it
		lenient().when(lessonAssistantConversationEntityService.findByUserIdAndLessonId(anyLong(), anyLong()))
				.thenReturn(Optional.empty());
		lenient().when(userEntityService.getReference(anyLong())).thenReturn(mock(User.class));
		lenient().when(currentTime.utcDateTime()).thenReturn(LocalDateTime.of(2026, 1, 1, 0, 0));
		lenient().when(lessonAssistantConversationAssembler.appendTurn(any(), anyString(), anyString()))
				.thenReturn(List.of());
		lenient().when(lessonAssistantConversationEntityService.save(any()))
				.thenAnswer(invocation -> invocation.getArgument(0));
	}
}
