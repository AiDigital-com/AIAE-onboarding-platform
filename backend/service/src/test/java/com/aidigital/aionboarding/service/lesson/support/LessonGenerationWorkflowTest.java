package com.aidigital.aionboarding.service.lesson.support;

import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonStatus;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.external.openai.model.OpenAiFileInput;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.lesson.enums.LessonCreationModeV1;
import com.aidigital.aionboarding.service.lesson.models.CreateLessonInput;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonEntityService;
import com.aidigital.aionboarding.service.lesson.util.LessonContentUtil;
import com.aidigital.aionboarding.service.lessongen.model.GeneratedContentResult;
import com.aidigital.aionboarding.service.lessongen.model.LessonGenPrompt;
import com.aidigital.aionboarding.service.lessongen.prompt.LessonPromptBuilder;
import com.aidigital.aionboarding.service.lessongen.services.LessonGenService;
import com.aidigital.aionboarding.service.material.models.DuplicateTitle;
import com.aidigital.aionboarding.service.material.models.DuplicateUrl;
import com.aidigital.aionboarding.service.material.models.MaterialPreparationItem;
import com.aidigital.aionboarding.service.material.models.OverlapNotes;
import com.aidigital.aionboarding.service.material.models.PreparationStats;
import com.aidigital.aionboarding.service.material.models.PreparedMaterialsResult;
import com.aidigital.aionboarding.service.material.models.SignalItem;
import com.aidigital.aionboarding.service.material.models.SignalNotes;
import com.aidigital.aionboarding.service.material.models.SourceReferenceItem;
import com.aidigital.aionboarding.service.material.services.MaterialOpenAiFilePreparationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonGenerationWorkflowTest {

	@Mock
	private LessonEntityService lessonEntityService;
	@Mock
	private MaterialOpenAiFilePreparationService materialOpenAiFilePreparationService;
	@Mock
	private LessonPromptBuilder lessonPromptBuilder;
	@Mock
	private LessonGenService lessonGenService;
	@Mock
	private LessonContentUtil lessonContentUtil;
	@Mock
	private CurrentTime currentTime;

	@InjectMocks
	private LessonGenerationWorkflow workflow;

	@BeforeEach
	void setUpCurrentTime() {
		lenient().when(currentTime.utcDateTime()).thenReturn(LocalDateTime.parse("2026-07-03T12:00:00"));
	}

	private CreateLessonInput draftInput(List<Long> materialIds) {
		return new CreateLessonInput(
				"Draft", "Make a lesson", "standard", "clear", "structured theoretical lesson",
				materialIds, List.of(), "desc", null, LessonCreationModeV1.GENERATE);
	}

	private Lesson lesson(Long id, String statusCode) {
		Lesson lesson = new Lesson();
		lesson.setId(id);
		LessonStatus status = new LessonStatus();
		status.setCode(statusCode);
		lesson.setStatus(status);
		return lesson;
	}

	private Map<String, Object> materialMap(Long id, String title) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("id", id);
		map.put("sourceNumber", 1);
		map.put("title", title);
		map.put("description", "");
		map.put("text", "");
		map.put("youtubeUrls", List.of());
		map.put("youtubeVideos", List.of());
		map.put("youtubeTranscripts", List.of());
		map.put("links", List.of());
		map.put("linkAssets", List.of());
		map.put("attachments", List.of());
		return map;
	}

	private PreparedMaterialsResult preparedMaterials(Long materialId, String title) {
		Map<String, Object> data = materialMap(materialId, title);
		MaterialPreparationItem item = new MaterialPreparationItem(materialId, 1, data);
		return new PreparedMaterialsResult(
				List.of(item),
				List.of(new SourceReferenceItem(materialId, 1, data)),
				List.of("term"),
				new SignalNotes(List.of(new SignalItem(1, "example")), List.of()),
				new OverlapNotes(List.of(new DuplicateTitle("title")), List.of(new DuplicateUrl("url"))),
				new PreparationStats(1, 1_000)
		);
	}

	@Test
	void runSuccessShouldMarkLessonReadyTest() {
		// Given
		List<Long> materialIds = List.of(4L);
		CreateLessonInput input = draftInput(materialIds);
		PreparedMaterialsResult prepared = preparedMaterials(4L, "Material 4");
		LessonGenPrompt prompt = new LessonGenPrompt("v1", "v1", "instructions", "input");
		Lesson draft = lesson(40L, "draft");
		Lesson generating = lesson(40L, "generating");
		Lesson ready = lesson(40L, "ready");
		GeneratedContentResult result = new GeneratedContentResult("# Title\nbody", Map.of("model", "gpt-4o-mini"));

		when(lessonPromptBuilder.buildTheoreticalLessonPrompt(
				eq(prepared), anyString(), anyString(), anyString(), anyString(), eq(List.of())))
				.thenReturn(prompt);
		when(lessonEntityService.markGenerating(eq(draft), any())).thenReturn(generating);
		when(lessonGenService.generateLessonContent(prompt)).thenReturn(result);
		when(lessonContentUtil.looksLikeHtml(result.content())).thenReturn(false);
		when(lessonContentUtil.markdownToHtml(result.content())).thenReturn("<h1>Title</h1><p>body</p>");
		when(lessonContentUtil.extractHtmlTitle("<h1>Title</h1><p>body</p>")).thenReturn("Title");
		when(lessonEntityService.markReady(eq(generating), eq("Title"), anyString(), eq("# Title\nbody"), any()))
				.thenReturn(ready);

		// When
		Lesson resultLesson = workflow.run(draft, prepared, input, materialIds, "Draft");

		// Then
		assertThat(resultLesson.getId()).isEqualTo(40L);
		assertThat(resultLesson.getStatus().getCode()).isEqualTo("ready");
	}

	@Test
	void runFailureShouldMarkFailedAndThrowC003Test() {
		// Given
		List<Long> materialIds = List.of(5L);
		CreateLessonInput input = draftInput(materialIds);
		PreparedMaterialsResult prepared = preparedMaterials(5L, "Material 5");
		LessonGenPrompt prompt = new LessonGenPrompt("v1", "v1", "instructions", "input");
		Lesson draft = lesson(50L, "draft");
		Lesson generating = lesson(50L, "generating");
		RuntimeException openAiError = new RuntimeException("OpenAI rate limit");

		when(lessonPromptBuilder.buildTheoreticalLessonPrompt(
				eq(prepared), anyString(), anyString(), anyString(), anyString(), eq(List.of())))
				.thenReturn(prompt);
		when(lessonEntityService.markGenerating(eq(draft), any())).thenReturn(generating);
		when(lessonGenService.generateLessonContent(prompt)).thenThrow(openAiError);

		// Then
		assertThatThrownBy(() -> workflow.run(draft, prepared, input, materialIds, "Draft"))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("Lesson generation failed")
				.satisfies(ex -> assertThat(((AppException) ex).getCode()).isEqualTo(ErrorReason.C003.name()));

		ArgumentCaptor<Map<String, Object>> metaCaptor = ArgumentCaptor.forClass(Map.class);
		verify(lessonEntityService).markFailed(eq(generating), eq("OpenAI rate limit"), metaCaptor.capture());
		assertThat(metaCaptor.getValue()).containsKey("failedAt");
	}

	@Nested
	class FileInputsTests {

		@Test
		void run_aiPath_callsPrepareFileInputsWithMaterialIdsTest() {
			// Given
			List<Long> ids = List.of(7L);
			CreateLessonInput input = draftInput(ids);
			PreparedMaterialsResult prepared = preparedMaterials(7L, "Material 7");
			LessonGenPrompt prompt = new LessonGenPrompt("v1", "v1", "instructions", "input");
			Lesson draft = lesson(70L, "draft");
			Lesson generating = lesson(70L, "generating");
			Lesson ready = lesson(70L, "ready");
			GeneratedContentResult result = new GeneratedContentResult("# Title\nbody", Map.of());

			when(materialOpenAiFilePreparationService.prepareFileInputs(ids)).thenReturn(List.of());
			when(lessonPromptBuilder.buildTheoreticalLessonPrompt(
					eq(prepared), anyString(), anyString(), anyString(), anyString(), anyList()))
					.thenReturn(prompt);
			when(lessonEntityService.markGenerating(eq(draft), any())).thenReturn(generating);
			when(lessonGenService.generateLessonContent(prompt)).thenReturn(result);
			when(lessonContentUtil.looksLikeHtml(result.content())).thenReturn(false);
			when(lessonContentUtil.markdownToHtml(result.content())).thenReturn("<h1>Title</h1>");
			when(lessonContentUtil.extractHtmlTitle("<h1>Title</h1>")).thenReturn("Title");
			when(lessonEntityService.markReady(eq(generating), eq("Title"), anyString(), eq("# Title\nbody"), any()))
					.thenReturn(ready);

			// Execution
			workflow.run(draft, prepared, input, ids, "Draft");

			// Verification
			verify(materialOpenAiFilePreparationService).prepareFileInputs(ids);
		}

		@Test
		void run_aiPath_attachedFilesContainsConvertedFileInputsTest() {
			// Given
			List<Long> ids = List.of(8L);
			CreateLessonInput input = draftInput(ids);
			PreparedMaterialsResult prepared = preparedMaterials(8L, "Material 8");
			LessonGenPrompt prompt = new LessonGenPrompt("v1", "v1", "instructions", "input");
			Lesson draft = lesson(80L, "draft");
			Lesson generating = lesson(80L, "generating");
			Lesson ready = lesson(80L, "ready");
			GeneratedContentResult result = new GeneratedContentResult("# Title\nbody", Map.of());
			OpenAiFileInput fileInput = new OpenAiFileInput("input_file", "file-abc");

			when(materialOpenAiFilePreparationService.prepareFileInputs(ids)).thenReturn(List.of(fileInput));
			when(lessonPromptBuilder.buildTheoreticalLessonPrompt(
					eq(prepared), anyString(), anyString(), anyString(), anyString(), anyList()))
					.thenReturn(prompt);
			when(lessonEntityService.markGenerating(eq(draft), any())).thenReturn(generating);
			when(lessonGenService.generateLessonContent(prompt)).thenReturn(result);
			when(lessonContentUtil.looksLikeHtml(result.content())).thenReturn(false);
			when(lessonContentUtil.markdownToHtml(result.content())).thenReturn("<h1>Title</h1>");
			when(lessonContentUtil.extractHtmlTitle("<h1>Title</h1>")).thenReturn("Title");
			when(lessonEntityService.markReady(eq(generating), eq("Title"), anyString(), eq("# Title\nbody"), any()))
					.thenReturn(ready);

			// Execution
			workflow.run(draft, prepared, input, ids, "Draft");

			// Verification
			ArgumentCaptor<Map<String, Object>> metaCaptor = ArgumentCaptor.forClass(Map.class);
			verify(lessonEntityService).markGenerating(eq(draft), metaCaptor.capture());
			Map<String, Object> generatingMeta = metaCaptor.getValue();
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> attachedFiles = (List<Map<String, Object>>) generatingMeta.get("attachedFiles");
			assertThat(attachedFiles).hasSize(1);
			assertThat(attachedFiles.get(0).get("file_id")).isEqualTo("file-abc");
			assertThat(attachedFiles.get(0).get("type")).isEqualTo("input_file");

			ArgumentCaptor<List<Map<String, Object>>> attachedFilesCaptor = ArgumentCaptor.forClass(List.class);
			verify(lessonPromptBuilder).buildTheoreticalLessonPrompt(
					eq(prepared), anyString(), anyString(), anyString(), anyString(), attachedFilesCaptor.capture());
			assertThat(attachedFilesCaptor.getValue()).containsExactly(Map.of(
					"type", "input_file",
					"file_id", "file-abc"
			));

			ArgumentCaptor<Map<String, Object>> readyMetaCaptor = ArgumentCaptor.forClass(Map.class);
			verify(lessonEntityService).markReady(
					eq(generating), eq("Title"), anyString(), eq("# Title\nbody"), readyMetaCaptor.capture());
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> readyAttachedFiles =
					(List<Map<String, Object>>) readyMetaCaptor.getValue().get("attachedFiles");
			assertThat(readyAttachedFiles).containsExactly(Map.of(
					"type", "input_file",
					"file_id", "file-abc"
			));
		}

		@Test
		void run_aiPath_failureMetadataContainsConvertedFileInputsTest() {
			// Given
			List<Long> ids = List.of(9L);
			CreateLessonInput input = draftInput(ids);
			PreparedMaterialsResult prepared = preparedMaterials(9L, "Material 9");
			LessonGenPrompt prompt = new LessonGenPrompt("v1", "v1", "instructions", "input");
			Lesson draft = lesson(90L, "draft");
			Lesson generating = lesson(90L, "generating");
			OpenAiFileInput fileInput = new OpenAiFileInput("input_file", "file-failure");

			when(materialOpenAiFilePreparationService.prepareFileInputs(ids)).thenReturn(List.of(fileInput));
			when(lessonPromptBuilder.buildTheoreticalLessonPrompt(
					eq(prepared), anyString(), anyString(), anyString(), anyString(), anyList()))
					.thenReturn(prompt);
			when(lessonEntityService.markGenerating(eq(draft), any())).thenReturn(generating);
			when(lessonGenService.generateLessonContent(prompt)).thenThrow(new RuntimeException("OpenAI rate limit"));

			// Execution / Verification
			assertThatThrownBy(() -> workflow.run(draft, prepared, input, ids, "Draft"))
					.isInstanceOf(AppException.class)
					.hasMessageContaining("Lesson generation failed");

			ArgumentCaptor<Map<String, Object>> failedMetaCaptor = ArgumentCaptor.forClass(Map.class);
			verify(lessonEntityService).markFailed(eq(generating), eq("OpenAI rate limit"),
					failedMetaCaptor.capture());
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> failedAttachedFiles =
					(List<Map<String, Object>>) failedMetaCaptor.getValue().get("attachedFiles");
			assertThat(failedAttachedFiles).containsExactly(Map.of(
					"type", "input_file",
					"file_id", "file-failure"
			));
		}
	}
}
