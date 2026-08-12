package com.aidigital.aionboarding.service.lesson.services.impl;

import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonStatus;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.material.entities.Material;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.lesson.enums.LessonCreationModeV1;
import com.aidigital.aionboarding.service.lesson.models.CreateLessonInput;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonEntityService;
import com.aidigital.aionboarding.service.lesson.support.LessonGenerationInputValidator;
import com.aidigital.aionboarding.service.lesson.support.LessonGenerationTranscriptCondenser;
import com.aidigital.aionboarding.service.lesson.support.LessonGenerationWorkflow;
import com.aidigital.aionboarding.service.material.models.DuplicateTitle;
import com.aidigital.aionboarding.service.material.models.DuplicateUrl;
import com.aidigital.aionboarding.service.material.models.MaterialPreparationItem;
import com.aidigital.aionboarding.service.material.models.OverlapNotes;
import com.aidigital.aionboarding.service.material.models.PreparationStats;
import com.aidigital.aionboarding.service.material.models.PreparedMaterialsResult;
import com.aidigital.aionboarding.service.material.models.SignalItem;
import com.aidigital.aionboarding.service.material.models.SignalNotes;
import com.aidigital.aionboarding.service.material.models.SourceReferenceItem;
import com.aidigital.aionboarding.service.material.services.MaterialPreparationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonInitialGenerationServiceImplTest {

	@Mock
	private LessonEntityService lessonEntityService;
	@Mock
	private MaterialPreparationService materialPreparationService;
	@Mock
	private LessonGenerationInputValidator inputValidator;
	@Mock
	private LessonGenerationTranscriptCondenser transcriptCondenser;
	@Mock
	private LessonGenerationWorkflow lessonGenerationWorkflow;

	@InjectMocks
	private LessonInitialGenerationServiceImpl service;

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

	private PreparedMaterialsResult preparedMaterials(Material material) {
		Map<String, Object> data = materialMap(material.getId(), material.getTitle());
		MaterialPreparationItem item = new MaterialPreparationItem(material.getId(), 1, data);
		return new PreparedMaterialsResult(
				List.of(item),
				List.of(new SourceReferenceItem(material.getId(), 1, data)),
				List.of("term"),
				new SignalNotes(List.of(new SignalItem(1, "example")), List.of()),
				new OverlapNotes(List.of(new DuplicateTitle("title")), List.of(new DuplicateUrl("url"))),
				new PreparationStats(1, 1_000)
		);
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
	void generateShouldCreateDraftAndDelegateToGenerationWorkflow() {
		// Given
		AppUser viewer = appUser(4L);
		Material material = material(4L);
		CreateLessonInput input = new CreateLessonInput(
				null, "Make a lesson", "standard", "clear", "structured theoretical lesson",
				List.of(4L), List.of(), null, null, LessonCreationModeV1.GENERATE);

		PreparedMaterialsResult prepared = preparedMaterials(material);
		Lesson draft = lesson(40L, "draft");
		Lesson ready = lesson(40L, "ready");

		when(inputValidator.deduplicatePreserveOrder(List.of(4L))).thenReturn(List.of(4L));
		when(inputValidator.buildDraftTitle(List.of(material))).thenReturn(material.getTitle());
		when(lessonEntityService.findMaterialsByIds(List.of(4L))).thenReturn(List.of(material));
		when(materialPreparationService.prepareForMaterialIds(List.of(4L))).thenReturn(prepared);
		when(transcriptCondenser.condense(prepared)).thenReturn(prepared);
		when(lessonEntityService.createDraft(eq(viewer), any(CreateLessonInput.class), eq(List.of(4L))))
				.thenReturn(draft);
		when(lessonGenerationWorkflow.run(eq(draft), eq(prepared), any(CreateLessonInput.class), eq(List.of(4L)),
				eq(material.getTitle()))).thenReturn(ready);

		// When
		Lesson resultLesson = service.generate(viewer, input);

		// Then
		assertThat(resultLesson.getId()).isEqualTo(40L);
		assertThat(resultLesson.getStatus().getCode()).isEqualTo("ready");
	}

	@Test
	void generateShouldDelegateCondensationToTranscriptCondenserAndPassCondensedResultToWorkflow() {
		// Given
		AppUser viewer = appUser(6L);
		Material material = material(6L);
		CreateLessonInput input = new CreateLessonInput(
				null, "Make a lesson", "standard", "clear", "structured theoretical lesson",
				List.of(6L), List.of(), null, null, LessonCreationModeV1.GENERATE);

		PreparedMaterialsResult prepared = preparedMaterials(material);
		PreparedMaterialsResult condensed = preparedMaterials(material);
		Lesson draft = lesson(60L, "draft");
		Lesson ready = lesson(60L, "ready");

		when(inputValidator.deduplicatePreserveOrder(List.of(6L))).thenReturn(List.of(6L));
		when(inputValidator.buildDraftTitle(List.of(material))).thenReturn(material.getTitle());
		when(lessonEntityService.findMaterialsByIds(List.of(6L))).thenReturn(List.of(material));
		when(materialPreparationService.prepareForMaterialIds(List.of(6L))).thenReturn(prepared);
		when(transcriptCondenser.condense(prepared)).thenReturn(condensed);
		when(lessonEntityService.createDraft(eq(viewer), any(CreateLessonInput.class), eq(List.of(6L))))
				.thenReturn(draft);
		when(lessonGenerationWorkflow.run(eq(draft), eq(condensed), any(CreateLessonInput.class), eq(List.of(6L)),
				eq(material.getTitle()))).thenReturn(ready);

		// When
		Lesson resultLesson = service.generate(viewer, input);

		// Then
		assertThat(resultLesson.getId()).isEqualTo(60L);
		verify(transcriptCondenser).condense(prepared);
		ArgumentCaptor<PreparedMaterialsResult> captor = ArgumentCaptor.forClass(PreparedMaterialsResult.class);
		verify(lessonGenerationWorkflow).run(eq(draft), captor.capture(), any(CreateLessonInput.class),
				eq(List.of(6L)), eq(material.getTitle()));
		assertThat(captor.getValue()).isSameAs(condensed);
	}
}
