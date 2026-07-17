package com.aidigital.aionboarding.service.lesson.services.impl;

import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonStatus;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.material.entities.Material;
import com.aidigital.aionboarding.external.openai.model.OpenAiFileInput;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.lesson.enums.LessonCreationModeV1;
import com.aidigital.aionboarding.service.lesson.models.CreateLessonInput;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonEntityService;
import com.aidigital.aionboarding.service.lesson.support.LessonGenerationInputValidator;
import com.aidigital.aionboarding.service.lesson.support.LessonGenerationTranscriptCondenser;
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
import com.aidigital.aionboarding.service.material.services.MaterialPreparationService;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonInitialGenerationServiceImplTest {

	private static final int CONDENSATION_THRESHOLD = 12_000;

	@Mock
	private LessonEntityService lessonEntityService;
	@Mock
	private MaterialPreparationService materialPreparationService;
	@Mock
	private MaterialOpenAiFilePreparationService materialOpenAiFilePreparationService;
	@Mock
	private LessonPromptBuilder lessonPromptBuilder;
	@Mock
	private LessonGenService lessonGenService;
	@Mock
	private LessonContentUtil lessonContentUtil;
	@Mock
	private LessonGenerationInputValidator inputValidator;
	@Mock
	private LessonGenerationTranscriptCondenser transcriptCondenser;
	@Mock
	private CurrentTime currentTime;

	@InjectMocks
	private LessonInitialGenerationServiceImpl service;

	@BeforeEach
	void setUpCurrentTime() {
		lenient().when(currentTime.utcDateTime()).thenReturn(LocalDateTime.parse("2026-07-03T12:00:00"));
	}

	@Test
	void createManualLessonShouldReturnReadyLesson() {
		// Given
		AppUser viewer = appUser(1L);
		CreateLessonInput input = new CreateLessonInput(
				"Manual", "", "standard", "clear", "structured theoretical lesson",
				List.of(), List.of(), "desc", "<p>manual</p>", LessonCreationModeV1.CREATE_MANUAL);
		Lesson ready = lesson(10L, "ready");
		when(inputValidator.deduplicatePreserveOrder(List.of())).thenReturn(List.of());
		when(lessonEntityService.createManualLesson(eq(viewer), eq(input), eq(List.of())))
				.thenReturn(ready);

		// When
		Lesson result = service.generate(viewer, input);

		// Then
		assertThat(result.getId()).isEqualTo(10L);
	}

	@Test
	void createManualLessonShouldRejectBlankTitle() {
		// Given
		AppUser viewer = appUser(1L);
		CreateLessonInput input = new CreateLessonInput(
				"", "", "standard", "clear", "structured theoretical lesson",
				List.of(), List.of(), "desc", "<p>manual</p>", LessonCreationModeV1.CREATE_MANUAL);
		when(inputValidator.deduplicatePreserveOrder(List.of())).thenReturn(List.of());
		doThrow(new AppException(ErrorReason.V001, "Manual lesson requires a title and non-empty content."))
				.when(inputValidator).validateManualLesson("", "<p>manual</p>");

		// Then
		assertThatThrownBy(() -> service.generate(viewer, input))
				.isInstanceOf(AppException.class)
				.satisfies(ex -> assertThat(((AppException) ex).getCode()).isEqualTo(ErrorReason.V001.name()));
	}

	@Test
	void generateWithoutMaterialsOrInstructionsShouldThrowValidationError() {
		// Given
		AppUser viewer = appUser(2L);
		CreateLessonInput input = new CreateLessonInput(
				null, "", "standard", "clear", "structured theoretical lesson",
				List.of(), List.of(), null, null, LessonCreationModeV1.GENERATE);
		when(inputValidator.deduplicatePreserveOrder(List.of())).thenReturn(List.of());
		when(lessonEntityService.findMaterialsByIds(List.of())).thenReturn(List.of());
		doThrow(new AppException(ErrorReason.V001,
				"Select at least one material or describe what the lesson should be about."))
				.when(inputValidator).validateMaterialsUsable(List.of(), List.of(), "");

		// Then
		assertThatThrownBy(() -> service.generate(viewer, input))
				.isInstanceOf(AppException.class)
				.satisfies(ex -> assertThat(((AppException) ex).getCode()).isEqualTo(ErrorReason.V001.name()));
	}

	@Test
	void generateShouldRejectUnusableMaterial() {
		// Given
		AppUser viewer = appUser(3L);
		Material material = material(3L);
		material.setTitle("");
		material.setDescription("");
		material.setTextContent("");
		CreateLessonInput input = new CreateLessonInput(
				null, "Make a lesson", "standard", "clear", "structured theoretical lesson",
				List.of(3L), List.of(), null, null, LessonCreationModeV1.GENERATE);

		when(inputValidator.deduplicatePreserveOrder(List.of(3L))).thenReturn(List.of(3L));
		when(lessonEntityService.findMaterialsByIds(List.of(3L))).thenReturn(List.of(material));
		doThrow(new AppException(ErrorReason.V001, "Material 3 has no usable content."))
				.when(inputValidator).validateMaterialsUsable(List.of(3L), List.of(material), "Make a lesson");

		// Then
		assertThatThrownBy(() -> service.generate(viewer, input))
				.isInstanceOf(AppException.class)
				.satisfies(ex -> assertThat(((AppException) ex).getCode()).isEqualTo(ErrorReason.V001.name()));
	}

	@Test
	void generateSuccessShouldMarkLessonReady() {
		// Given
		AppUser viewer = appUser(4L);
		Material material = material(4L);
		CreateLessonInput input = new CreateLessonInput(
				null, "Make a lesson", "standard", "clear", "structured theoretical lesson",
				List.of(4L), List.of(), null, null, LessonCreationModeV1.GENERATE);

		PreparedMaterialsResult prepared = preparedMaterials(material, false);
		LessonGenPrompt prompt = new LessonGenPrompt("v1", "v1", "instructions", "input");
		Lesson draft = lesson(40L, "draft");
		Lesson generating = lesson(40L, "generating");
		Lesson ready = lesson(40L, "ready");
		GeneratedContentResult result = new GeneratedContentResult("# Title\nbody", Map.of("model", "gpt-4o-mini"));

		when(inputValidator.deduplicatePreserveOrder(List.of(4L))).thenReturn(List.of(4L));
		when(inputValidator.buildDraftTitle(List.of(material))).thenReturn(material.getTitle());
		when(lessonEntityService.findMaterialsByIds(List.of(4L))).thenReturn(List.of(material));
		when(materialPreparationService.prepareForMaterialIds(List.of(4L))).thenReturn(prepared);
		when(transcriptCondenser.condense(prepared)).thenReturn(prepared);
		when(lessonPromptBuilder.buildTheoreticalLessonPrompt(
				eq(prepared), anyString(), anyString(), anyString(), anyString(), eq(List.of())))
				.thenReturn(prompt);
		when(lessonEntityService.createDraft(eq(viewer), any(CreateLessonInput.class), eq(List.of(4L))))
				.thenReturn(draft);
		when(lessonEntityService.markGenerating(eq(draft), any())).thenReturn(generating);
		when(lessonGenService.generateLessonContent(prompt)).thenReturn(result);
		when(lessonContentUtil.looksLikeHtml(result.content())).thenReturn(false);
		when(lessonContentUtil.markdownToHtml(result.content())).thenReturn("<h1>Title</h1><p>body</p>");
		when(lessonContentUtil.extractHtmlTitle("<h1>Title</h1><p>body</p>")).thenReturn("Title");
		when(lessonEntityService.markReady(eq(generating), eq("Title"), anyString(), eq("# Title\nbody"), any()))
				.thenReturn(ready);

		// When
		Lesson resultLesson = service.generate(viewer, input);

		// Then
		assertThat(resultLesson.getId()).isEqualTo(40L);
		assertThat(resultLesson.getStatus().getCode()).isEqualTo("ready");
	}

	@Test
	void generateFailureShouldMarkFailedAndThrowC003() {
		// Given
		AppUser viewer = appUser(5L);
		Material material = material(5L);
		CreateLessonInput input = new CreateLessonInput(
				null, "Make a lesson", "standard", "clear", "structured theoretical lesson",
				List.of(5L), List.of(), null, null, LessonCreationModeV1.GENERATE);

		PreparedMaterialsResult prepared = preparedMaterials(material, false);
		LessonGenPrompt prompt = new LessonGenPrompt("v1", "v1", "instructions", "input");
		Lesson draft = lesson(50L, "draft");
		Lesson generating = lesson(50L, "generating");
		RuntimeException openAiError = new RuntimeException("OpenAI rate limit");

		when(inputValidator.deduplicatePreserveOrder(List.of(5L))).thenReturn(List.of(5L));
		when(inputValidator.buildDraftTitle(List.of(material))).thenReturn(material.getTitle());
		when(lessonEntityService.findMaterialsByIds(List.of(5L))).thenReturn(List.of(material));
		when(materialPreparationService.prepareForMaterialIds(List.of(5L))).thenReturn(prepared);
		when(transcriptCondenser.condense(prepared)).thenReturn(prepared);
		when(lessonPromptBuilder.buildTheoreticalLessonPrompt(
				eq(prepared), anyString(), anyString(), anyString(), anyString(), eq(List.of())))
				.thenReturn(prompt);
		when(lessonEntityService.createDraft(eq(viewer), any(CreateLessonInput.class), eq(List.of(5L))))
				.thenReturn(draft);
		when(lessonEntityService.markGenerating(eq(draft), any())).thenReturn(generating);
		when(lessonGenService.generateLessonContent(prompt)).thenThrow(openAiError);

		// Then
		assertThatThrownBy(() -> service.generate(viewer, input))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("Lesson generation failed")
				.satisfies(ex -> assertThat(((AppException) ex).getCode()).isEqualTo(ErrorReason.C003.name()));

		ArgumentCaptor<Map<String, Object>> metaCaptor = ArgumentCaptor.forClass(Map.class);
		verify(lessonEntityService).markFailed(eq(generating), eq("OpenAI rate limit"), metaCaptor.capture());
		assertThat(metaCaptor.getValue()).containsKey("failedAt");
	}

	@Test
	void generateShouldDelegateCondensationToTranscriptCondenser() {
		// Given
		AppUser viewer = appUser(6L);
		Material material = material(6L);
		CreateLessonInput input = new CreateLessonInput(
				null, "Make a lesson", "standard", "clear", "structured theoretical lesson",
				List.of(6L), List.of(), null, null, LessonCreationModeV1.GENERATE);

		String longText = "x".repeat(CONDENSATION_THRESHOLD + 1);
		PreparedMaterialsResult prepared = preparedMaterialsWithTranscript(material, longText);
		PreparedMaterialsResult condensed = preparedMaterials(material, false);
		LessonGenPrompt prompt = new LessonGenPrompt("v1", "v1", "instructions", "input");
		Lesson draft = lesson(60L, "draft");
		Lesson generating = lesson(60L, "generating");
		Lesson ready = lesson(60L, "ready");
		GeneratedContentResult result = new GeneratedContentResult("# Title\nbody", Map.of());

		when(inputValidator.deduplicatePreserveOrder(List.of(6L))).thenReturn(List.of(6L));
		when(inputValidator.buildDraftTitle(List.of(material))).thenReturn(material.getTitle());
		when(lessonEntityService.findMaterialsByIds(List.of(6L))).thenReturn(List.of(material));
		when(materialPreparationService.prepareForMaterialIds(List.of(6L))).thenReturn(prepared);
		when(transcriptCondenser.condense(prepared)).thenReturn(condensed);
		when(lessonPromptBuilder.buildTheoreticalLessonPrompt(
				eq(condensed), anyString(), anyString(), anyString(), anyString(), eq(List.of())))
				.thenReturn(prompt);
		when(lessonEntityService.createDraft(eq(viewer), any(CreateLessonInput.class), eq(List.of(6L))))
				.thenReturn(draft);
		when(lessonEntityService.markGenerating(eq(draft), any())).thenReturn(generating);
		when(lessonGenService.generateLessonContent(prompt)).thenReturn(result);
		when(lessonContentUtil.looksLikeHtml(result.content())).thenReturn(false);
		when(lessonContentUtil.markdownToHtml(result.content())).thenReturn("<h1>Title</h1><p>body</p>");
		when(lessonContentUtil.extractHtmlTitle("<h1>Title</h1><p>body</p>")).thenReturn("Title");
		when(lessonEntityService.markReady(eq(generating), eq("Title"), anyString(), eq("# Title\nbody"), any()))
				.thenReturn(ready);

		// When
		Lesson resultLesson = service.generate(viewer, input);

		// Then
		assertThat(resultLesson.getId()).isEqualTo(60L);
		verify(transcriptCondenser).condense(prepared);
		ArgumentCaptor<PreparedMaterialsResult> captor = ArgumentCaptor.forClass(PreparedMaterialsResult.class);
		verify(lessonPromptBuilder).buildTheoreticalLessonPrompt(captor.capture(), anyString(), anyString(),
				anyString(), anyString(), eq(List.of()));
		assertThat(captor.getValue()).isSameAs(condensed);
	}

	@Nested
	class GenerateFileInputsTests {

		@Test
		void generate_aiPath_callsPrepareFileInputsWithMaterialIds() {
			// Given
			AppUser viewer = appUser(7L);
			Material material = material(7L);
			List<Long> ids = List.of(7L);
			CreateLessonInput input = new CreateLessonInput(
					null, "Make a lesson", "standard", "clear", "structured theoretical lesson",
					ids, List.of(), null, null, LessonCreationModeV1.GENERATE);

			PreparedMaterialsResult prepared = preparedMaterials(material, false);
			LessonGenPrompt prompt = new LessonGenPrompt("v1", "v1", "instructions", "input");
			Lesson draft = lesson(70L, "draft");
			Lesson generating = lesson(70L, "generating");
			Lesson ready = lesson(70L, "ready");
			GeneratedContentResult result = new GeneratedContentResult("# Title\nbody", Map.of());

			when(inputValidator.deduplicatePreserveOrder(ids)).thenReturn(ids);
			when(inputValidator.buildDraftTitle(List.of(material))).thenReturn(material.getTitle());
			when(lessonEntityService.findMaterialsByIds(ids)).thenReturn(List.of(material));
			when(materialPreparationService.prepareForMaterialIds(ids)).thenReturn(prepared);
			when(transcriptCondenser.condense(prepared)).thenReturn(prepared);
			when(materialOpenAiFilePreparationService.prepareFileInputs(ids)).thenReturn(List.of());
			when(lessonPromptBuilder.buildTheoreticalLessonPrompt(
					eq(prepared), anyString(), anyString(), anyString(), anyString(), anyList()))
					.thenReturn(prompt);
			when(lessonEntityService.createDraft(eq(viewer), any(CreateLessonInput.class), eq(ids))).thenReturn(draft);
			when(lessonEntityService.markGenerating(eq(draft), any())).thenReturn(generating);
			when(lessonGenService.generateLessonContent(prompt)).thenReturn(result);
			when(lessonContentUtil.looksLikeHtml(result.content())).thenReturn(false);
			when(lessonContentUtil.markdownToHtml(result.content())).thenReturn("<h1>Title</h1>");
			when(lessonContentUtil.extractHtmlTitle("<h1>Title</h1>")).thenReturn("Title");
			when(lessonEntityService.markReady(eq(generating), eq("Title"), anyString(), eq("# Title\nbody"), any()))
					.thenReturn(ready);

			// Execution
			service.generate(viewer, input);

			// Verification
			verify(materialOpenAiFilePreparationService).prepareFileInputs(ids);
		}

		@Test
		void generate_aiPath_attachedFilesContainsConvertedFileInputs() {
			// Given
			AppUser viewer = appUser(8L);
			Material material = material(8L);
			List<Long> ids = List.of(8L);
			CreateLessonInput input = new CreateLessonInput(
					null, "Make a lesson", "standard", "clear", "structured theoretical lesson",
					ids, List.of(), null, null, LessonCreationModeV1.GENERATE);

			PreparedMaterialsResult prepared = preparedMaterials(material, false);
			LessonGenPrompt prompt = new LessonGenPrompt("v1", "v1", "instructions", "input");
			Lesson draft = lesson(80L, "draft");
			Lesson generating = lesson(80L, "generating");
			Lesson ready = lesson(80L, "ready");
			GeneratedContentResult result = new GeneratedContentResult("# Title\nbody", Map.of());
			OpenAiFileInput fileInput = new OpenAiFileInput("input_file", "file-abc");

			when(inputValidator.deduplicatePreserveOrder(ids)).thenReturn(ids);
			when(inputValidator.buildDraftTitle(List.of(material))).thenReturn(material.getTitle());
			when(lessonEntityService.findMaterialsByIds(ids)).thenReturn(List.of(material));
			when(materialPreparationService.prepareForMaterialIds(ids)).thenReturn(prepared);
			when(transcriptCondenser.condense(prepared)).thenReturn(prepared);
			when(materialOpenAiFilePreparationService.prepareFileInputs(ids)).thenReturn(List.of(fileInput));
			when(lessonPromptBuilder.buildTheoreticalLessonPrompt(
					eq(prepared), anyString(), anyString(), anyString(), anyString(), anyList()))
					.thenReturn(prompt);
			when(lessonEntityService.createDraft(eq(viewer), any(CreateLessonInput.class), eq(ids))).thenReturn(draft);
			when(lessonEntityService.markGenerating(eq(draft), any())).thenReturn(generating);
			when(lessonGenService.generateLessonContent(prompt)).thenReturn(result);
			when(lessonContentUtil.looksLikeHtml(result.content())).thenReturn(false);
			when(lessonContentUtil.markdownToHtml(result.content())).thenReturn("<h1>Title</h1>");
			when(lessonContentUtil.extractHtmlTitle("<h1>Title</h1>")).thenReturn("Title");
			when(lessonEntityService.markReady(eq(generating), eq("Title"), anyString(), eq("# Title\nbody"), any()))
					.thenReturn(ready);

			// Execution
			service.generate(viewer, input);

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
		void generate_aiPath_failureMetadataContainsConvertedFileInputs() {
			// Given
			AppUser viewer = appUser(9L);
			Material material = material(9L);
			List<Long> ids = List.of(9L);
			CreateLessonInput input = new CreateLessonInput(
					null, "Make a lesson", "standard", "clear", "structured theoretical lesson",
					ids, List.of(), null, null, LessonCreationModeV1.GENERATE);

			PreparedMaterialsResult prepared = preparedMaterials(material, false);
			LessonGenPrompt prompt = new LessonGenPrompt("v1", "v1", "instructions", "input");
			Lesson draft = lesson(90L, "draft");
			Lesson generating = lesson(90L, "generating");
			OpenAiFileInput fileInput = new OpenAiFileInput("input_file", "file-failure");

			when(inputValidator.deduplicatePreserveOrder(ids)).thenReturn(ids);
			when(inputValidator.buildDraftTitle(List.of(material))).thenReturn(material.getTitle());
			when(lessonEntityService.findMaterialsByIds(ids)).thenReturn(List.of(material));
			when(materialPreparationService.prepareForMaterialIds(ids)).thenReturn(prepared);
			when(transcriptCondenser.condense(prepared)).thenReturn(prepared);
			when(materialOpenAiFilePreparationService.prepareFileInputs(ids)).thenReturn(List.of(fileInput));
			when(lessonPromptBuilder.buildTheoreticalLessonPrompt(
					eq(prepared), anyString(), anyString(), anyString(), anyString(), anyList()))
					.thenReturn(prompt);
			when(lessonEntityService.createDraft(eq(viewer), any(CreateLessonInput.class), eq(ids))).thenReturn(draft);
			when(lessonEntityService.markGenerating(eq(draft), any())).thenReturn(generating);
			when(lessonGenService.generateLessonContent(prompt)).thenThrow(new RuntimeException("OpenAI rate limit"));

			// Execution / Verification
			assertThatThrownBy(() -> service.generate(viewer, input))
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

	private AppUser appUser(Long id) {
		return new AppUser(id, "clerk-" + id, "user@example.com", "User " + id, "admin", "User", null, null, null);
	}

	private Material material(Long id) {
		Material material = new Material();
		material.setId(id);
		material.setTitle("Material " + id);
		material.setDescription("Description " + id);
		material.setTextContent("Text " + id);
		return material;
	}

	private Lesson lesson(Long id, String statusCode) {
		Lesson lesson = new Lesson();
		lesson.setId(id);
		LessonStatus status = new LessonStatus();
		status.setCode(statusCode);
		lesson.setStatus(status);
		return lesson;
	}

	private PreparedMaterialsResult preparedMaterials(Material material, boolean withLongTranscript) {
		Map<String, Object> data = materialMap(material.getId(), material.getTitle());
		MaterialPreparationItem item = new MaterialPreparationItem(material.getId(), 1, data);
		int chars = withLongTranscript ? 100_000 : 1_000;
		return new PreparedMaterialsResult(
				List.of(item),
				List.of(new SourceReferenceItem(material.getId(), 1, data)),
				List.of("term"),
				new SignalNotes(List.of(new SignalItem(1, "example")), List.of()),
				new OverlapNotes(List.of(new DuplicateTitle("title")), List.of(new DuplicateUrl("url"))),
				new PreparationStats(1, chars)
		);
	}

	private PreparedMaterialsResult preparedMaterialsWithTranscript(Material material, String transcriptText) {
		Map<String, Object> data = materialMap(material.getId(), "Long transcript");
		Map<String, Object> transcript = new LinkedHashMap<>();
		transcript.put("url", "https://youtube.com/watch?v=abc");
		transcript.put("preparedText", transcriptText);
		transcript.put("wasCondensed", false);
		data.put("youtubeTranscripts", List.of(transcript));
		MaterialPreparationItem item = new MaterialPreparationItem(material.getId(), 1, data);
		return new PreparedMaterialsResult(
				List.of(item),
				List.of(new SourceReferenceItem(material.getId(), 1, data)),
				List.of(),
				new SignalNotes(List.of(), List.of()),
				new OverlapNotes(List.of(), List.of()),
				new PreparationStats(1, transcriptText.length())
		);
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
}
