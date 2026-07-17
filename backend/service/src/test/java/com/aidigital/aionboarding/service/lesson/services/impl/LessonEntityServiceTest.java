package com.aidigital.aionboarding.service.lesson.services.impl;

import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonContentFormat;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonPublicationStatus;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonStatus;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.lesson.entities.LessonMaterial;
import com.aidigital.aionboarding.domain.lesson.repositories.LessonRepository;
import com.aidigital.aionboarding.domain.lesson.repositories.LessonSearchSummaryProjection;
import com.aidigital.aionboarding.domain.material.entities.Material;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.service.common.dictionary.services.entity.LessonContentFormatEntityService;
import com.aidigital.aionboarding.service.common.dictionary.services.entity.LessonPublicationStatusEntityService;
import com.aidigital.aionboarding.service.common.dictionary.services.entity.LessonStatusEntityService;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.lesson.enums.LessonCreationModeV1;
import com.aidigital.aionboarding.service.lesson.models.CreateLessonInput;
import com.aidigital.aionboarding.service.lesson.models.LessonListQuery;
import com.aidigital.aionboarding.service.lesson.models.LessonSortField;
import com.aidigital.aionboarding.service.lesson.models.LessonVisibilityFilter;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonEntityService;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonMaterialEntityService;
import com.aidigital.aionboarding.service.lesson.support.LessonHtmlSanitizer;
import com.aidigital.aionboarding.service.lesson.support.LessonSpecificationBuilder;
import com.aidigital.aionboarding.service.material.services.entity.MaterialEntityService;
import com.aidigital.aionboarding.service.user.services.entity.UserEntityService;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonEntityServiceTest {

	@Mock
	private LessonRepository lessonRepository;
	@Mock
	private LessonMaterialEntityService lessonMaterialEntityService;
	@Mock
	private UserEntityService userEntityService;
	@Mock
	private MaterialEntityService materialEntityService;
	@Mock
	private LessonStatusEntityService lessonStatusEntityService;
	@Mock
	private LessonPublicationStatusEntityService lessonPublicationStatusEntityService;
	@Mock
	private LessonContentFormatEntityService lessonContentFormatEntityService;
	@Mock
	private LessonSpecificationBuilder lessonSpecificationBuilder;
	@Mock
	private LessonHtmlSanitizer lessonHtmlSanitizer;
	@Mock
	private CurrentTime currentTime;

	@InjectMocks
	private LessonEntityService lessonEntityService;

	@BeforeEach
	void setUp() {
		lenient().when(currentTime.utcDateTime()).thenReturn(LocalDateTime.parse("2026-07-03T12:00:00"));
		lenient().when(lessonHtmlSanitizer.sanitize(org.mockito.ArgumentMatchers.anyString()))
				.thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void findMaterialsByIdsShouldReturnMaterialsInInputOrderTest() {
		// Given:
		Material m1 = material(1L);
		Material m2 = material(2L);
		when(materialEntityService.findAllById(eq(List.of(2L, 1L)))).thenReturn(List.of(m2, m1));

		// When:
		List<Material> result = lessonEntityService.findMaterialsByIds(List.of(2L, 1L));

		// Then:
		assertThat(result).containsExactly(m2, m1);
	}

	@Test
	void createDraftShouldPersistLessonWithDraftStatusAndLinkedMaterialsTest() {
		// Given
		AppUser viewer = appUser(7L);
		User owner = user(7L);
		CreateLessonInput input = new CreateLessonInput(
				"Draft title", "instructions", "deep", "friendly", "article",
				List.of(1L, 2L), List.of("tag"), "description", null, LessonCreationModeV1.GENERATE);

		Material material1 = material(1L);
		Material material2 = material(2L);

		when(userEntityService.findById(7L)).thenReturn(Optional.of(owner));
		when(lessonStatusEntityService.getReferenceByCode("draft")).thenReturn(status("draft"));
		when(lessonPublicationStatusEntityService.getReferenceByCode("private")).thenReturn(publication("private"));
		when(lessonContentFormatEntityService.getReferenceByCode("markdown")).thenReturn(format("markdown"));
		when(lessonRepository.save(any(Lesson.class))).thenAnswer(invocation -> {
			Lesson lesson = invocation.getArgument(0);
			lesson.setId(100L);
			return lesson;
		});
		when(materialEntityService.findAllById(List.of(1L, 2L))).thenReturn(List.of(material1, material2));

		// When
		Lesson result = lessonEntityService.createDraft(viewer, input, List.of(1L, 2L));

		// Then
		assertThat(result.getId()).isEqualTo(100L);
		assertThat(result.getStatus().getCode()).isEqualTo("draft");
		assertThat(result.getPublicationStatus().getCode()).isEqualTo("private");
		assertThat(result.getContentFormat().getCode()).isEqualTo("markdown");
		assertThat(result.getCreatedBy()).isEqualTo("User 7");

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<LessonMaterial>> linksCaptor = ArgumentCaptor.forClass(List.class);
		verify(lessonMaterialEntityService).saveAll(linksCaptor.capture());
		assertThat(linksCaptor.getValue().get(0).getMaterial().getId()).isEqualTo(1L);
		assertThat(linksCaptor.getValue().get(1).getMaterial().getId()).isEqualTo(2L);
	}

	@Test
	void createManualLessonShouldPersistReadyLessonTest() {
		// Given
		AppUser viewer = appUser(8L);
		User owner = user(8L);
		CreateLessonInput input = new CreateLessonInput(
				"Manual", "", "standard", "clear", "structured theoretical lesson",
				List.of(), List.of(), "desc", "<p>html</p>", LessonCreationModeV1.CREATE_MANUAL);

		when(userEntityService.findById(8L)).thenReturn(Optional.of(owner));
		when(lessonStatusEntityService.getReferenceByCode("ready")).thenReturn(status("ready"));
		when(lessonPublicationStatusEntityService.getReferenceByCode("private")).thenReturn(publication("private"));
		when(lessonContentFormatEntityService.getReferenceByCode("markdown")).thenReturn(format("markdown"));
		when(lessonRepository.save(any(Lesson.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// When
		Lesson result = lessonEntityService.createManualLesson(viewer, input, List.of());

		// Then
		assertThat(result.getStatus().getCode()).isEqualTo("ready");
		assertThat(result.getContentHtml()).isEqualTo("<p>html</p>");
		assertThat(result.getTitle()).isEqualTo("Manual");
	}

	@Test
	void createManualLessonShouldPersistTheSanitizerOutputRatherThanRawInputTest() {
		// Given: the sanitizer would strip a script tag out of the authored HTML.
		AppUser viewer = appUser(9L);
		User owner = user(9L);
		CreateLessonInput input = new CreateLessonInput(
				"Manual", "", "standard", "clear", "structured theoretical lesson",
				List.of(), List.of(), "desc", "<p>html</p><script>evil()</script>",
				LessonCreationModeV1.CREATE_MANUAL);

		when(userEntityService.findById(9L)).thenReturn(Optional.of(owner));
		when(lessonStatusEntityService.getReferenceByCode("ready")).thenReturn(status("ready"));
		when(lessonPublicationStatusEntityService.getReferenceByCode("private")).thenReturn(publication("private"));
		when(lessonContentFormatEntityService.getReferenceByCode("markdown")).thenReturn(format("markdown"));
		when(lessonRepository.save(any(Lesson.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(lessonHtmlSanitizer.sanitize("<p>html</p><script>evil()</script>")).thenReturn("<p>html</p>");

		// When
		Lesson result = lessonEntityService.createManualLesson(viewer, input, List.of());

		// Then
		assertThat(result.getContentHtml()).isEqualTo("<p>html</p>");
	}

	@Test
	void markReadyShouldUpdateStatusAndContentTest() {
		// Given
		Lesson lesson = new Lesson();
		lesson.setId(5L);
		lesson.setStatus(status("generating"));
		when(lessonStatusEntityService.getReferenceByCode("ready")).thenReturn(status("ready"));
		when(lessonRepository.save(any(Lesson.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Map<String, Object> meta = Map.of("provider", "openai");

		// When
		Lesson result = lessonEntityService.markReady(lesson, "Final", "<h1>Final</h1>", "# Final", meta);

		// Then
		assertThat(result.getStatus().getCode()).isEqualTo("ready");
		assertThat(result.getTitle()).isEqualTo("Final");
		assertThat(result.getContentHtml()).isEqualTo("<h1>Final</h1>");
		assertThat(result.getContentMarkdown()).isEqualTo("# Final");
		assertThat(result.getErrorMessage()).isEmpty();
		assertThat(result.getGenerationMetadata()).containsEntry("provider", "openai");
	}

	@Test
	void markReadyShouldPersistTheSanitizerOutputRatherThanRawGeneratedHtmlTest() {
		// Given: AI-generated HTML is just as untrusted as manual authoring, so it must pass
		// through the same sanitizer before being persisted.
		Lesson lesson = new Lesson();
		lesson.setId(5L);
		lesson.setStatus(status("generating"));
		when(lessonStatusEntityService.getReferenceByCode("ready")).thenReturn(status("ready"));
		when(lessonRepository.save(any(Lesson.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(lessonHtmlSanitizer.sanitize("<h1>Final</h1><script>evil()</script>")).thenReturn("<h1>Final</h1>");

		// When
		Lesson result = lessonEntityService.markReady(lesson, "Final", "<h1>Final</h1><script>evil()</script>", "# " +
				"Final", Map.of());

		// Then
		assertThat(result.getContentHtml()).isEqualTo("<h1>Final</h1>");
	}

	@Test
	void markFailedShouldUpdateStatusAndErrorMessageTest() {
		// Given
		Lesson lesson = new Lesson();
		lesson.setId(6L);
		lesson.setStatus(status("generating"));
		when(lessonStatusEntityService.getReferenceByCode("failed")).thenReturn(status("failed"));
		when(lessonRepository.save(any(Lesson.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// When
		Lesson result = lessonEntityService.markFailed(lesson, "OpenAI timeout", Map.of());

		// Then
		assertThat(result.getStatus().getCode()).isEqualTo("failed");
		assertThat(result.getErrorMessage()).isEqualTo("OpenAI timeout");
	}

	@Test
	void clearFailureIfPresentShouldTransitionFailedLessonToReadyAndClearErrorMessageTest() {
		// Given
		Lesson lesson = new Lesson();
		lesson.setId(9L);
		lesson.setStatus(status("failed"));
		lesson.setErrorMessage("OpenAI timeout");
		when(lessonStatusEntityService.getReferenceByCode("ready")).thenReturn(status("ready"));

		// When
		lessonEntityService.clearFailureIfPresent(lesson);

		// Then
		assertThat(lesson.getStatus().getCode()).isEqualTo("ready");
		assertThat(lesson.getErrorMessage()).isEmpty();
	}

	@Test
	void clearFailureIfPresentShouldBeNoOpWhenLessonIsNotFailedTest() {
		// Given
		Lesson lesson = new Lesson();
		lesson.setId(10L);
		lesson.setStatus(status("ready"));
		lesson.setErrorMessage("");

		// When
		lessonEntityService.clearFailureIfPresent(lesson);

		// Then
		assertThat(lesson.getStatus().getCode()).isEqualTo("ready");
		verify(lessonStatusEntityService, never()).getReferenceByCode("ready");
	}

	@Test
	void shouldReturnLessonWhenFindByIdWithFetchesFoundTest() {
		// Given:
		Lesson lesson = new Lesson();
		lesson.setId(42L);
		when(lessonRepository.findByIdWithFetches(eq(42L))).thenReturn(Optional.of(lesson));

		// When:
		Lesson result = lessonEntityService.findByIdWithFetches(42L);

		// Then:
		assertThat(result).isSameAs(lesson);
	}

	@Test
	void shouldThrowWhenFindByIdWithFetchesNotFoundTest() {
		// Given:
		when(lessonRepository.findByIdWithFetches(eq(99L))).thenReturn(Optional.empty());

		// When-Then:
		assertThatThrownBy(() -> lessonEntityService.findByIdWithFetches(99L))
				.isInstanceOf(AppException.class);
	}

	@Test
	void getReferenceShouldThrowWhenMissingTest() {
		// Given
		when(lessonRepository.findById(999L)).thenReturn(Optional.empty());

		// Then
		assertThatThrownBy(() -> lessonEntityService.getReference(999L))
				.isInstanceOf(AppException.class)
				.satisfies(ex -> assertThat(((AppException) ex).getCode()).isEqualTo(ErrorReason.C001.name()));
	}

	@Test
	void searchSummariesShouldResolveDictionaryIdsAndDelegateToRepositoryWithBuiltSpecificationTest() {
		// Given:
		LessonStatus readyStatus = status(com.aidigital.aionboarding.domain.common.dictionary.LessonStatusCode.READY);
		readyStatus.setId(1L);
		LessonPublicationStatus publishedStatus = publication(
				com.aidigital.aionboarding.domain.common.dictionary.LessonPublicationStatusCode.PUBLISHED
		);
		publishedStatus.setId(2L);
		LessonListQuery query = new LessonListQuery(
				"term", List.of("design"), null, null, null, null, null,
				"quiz", true, LessonSortField.CREATED_AT, Sort.Direction.DESC
		);
		LessonVisibilityFilter visibility = new LessonVisibilityFilter(true, false, 5L);
		LessonSearchSummaryProjection projection = Instancio.create(LessonSearchSummaryProjection.class);
		@SuppressWarnings("unchecked")
		Specification<Lesson> specification = (Specification<Lesson>) org.mockito.Mockito.mock(Specification.class);
		Page<LessonSearchSummaryProjection> expectedPage = new PageImpl<>(List.of(projection));

		when(lessonStatusEntityService.getReferenceByCode(com.aidigital.aionboarding.domain.common.dictionary.LessonStatusCode.READY))
				.thenReturn(readyStatus);
		when(lessonPublicationStatusEntityService.getReferenceByCode(
				com.aidigital.aionboarding.domain.common.dictionary.LessonPublicationStatusCode.PUBLISHED
		)).thenReturn(publishedStatus);
		when(lessonSpecificationBuilder.build(query, visibility, null, null, 1L, 2L)).thenReturn(specification);
		when(lessonRepository.searchSummaries(eq(specification), any(Pageable.class))).thenReturn(expectedPage);

		// When:
		Page<LessonSearchSummaryProjection> result = lessonEntityService.searchSummaries(query, visibility, 0, 20);

		// Then:
		assertThat(result.getContent()).containsExactly(projection);
		verify(lessonSpecificationBuilder).build(query, visibility, null, null, 1L, 2L);
	}

	@Test
	void countSummariesShouldResolveDictionaryIdsAndDelegateToRepositoryCountWithBuiltSpecificationTest() {
		// Given:
		LessonStatus readyStatus = status(com.aidigital.aionboarding.domain.common.dictionary.LessonStatusCode.READY);
		readyStatus.setId(1L);
		LessonPublicationStatus publishedStatus = publication(
				com.aidigital.aionboarding.domain.common.dictionary.LessonPublicationStatusCode.PUBLISHED
		);
		publishedStatus.setId(2L);
		LessonListQuery query = new LessonListQuery(
				"term", List.of("design"), null, null, null, null, null,
				"quiz", true, LessonSortField.CREATED_AT, Sort.Direction.DESC
		);
		LessonVisibilityFilter visibility = new LessonVisibilityFilter(true, false, 5L);
		@SuppressWarnings("unchecked")
		Specification<Lesson> specification = (Specification<Lesson>) org.mockito.Mockito.mock(Specification.class);

		when(lessonStatusEntityService.getReferenceByCode(com.aidigital.aionboarding.domain.common.dictionary.LessonStatusCode.READY))
				.thenReturn(readyStatus);
		when(lessonPublicationStatusEntityService.getReferenceByCode(
				com.aidigital.aionboarding.domain.common.dictionary.LessonPublicationStatusCode.PUBLISHED
		)).thenReturn(publishedStatus);
		when(lessonSpecificationBuilder.build(query, visibility, null, null, 1L, 2L)).thenReturn(specification);
		when(lessonRepository.count(specification)).thenReturn(6L);

		// When:
		long result = lessonEntityService.countSummaries(query, visibility);

		// Then:
		assertThat(result).isEqualTo(6L);
		verify(lessonSpecificationBuilder).build(query, visibility, null, null, 1L, 2L);
	}

	@org.junit.jupiter.api.Nested
	class FindIdsWithTeacherVideoIn {

		@Test
		void shouldNotQueryRepositoryWhenLessonIdsEmptyTest() {
			// When:
			java.util.Set<Long> result = lessonEntityService.findIdsWithTeacherVideoIn(List.of());

			// Then:
			assertThat(result).isEmpty();
			verify(lessonRepository, never()).findIdsWithTeacherVideoIn(any());
		}

		@Test
		void shouldDelegateToRepositoryWhenLessonIdsPresentTest() {
			// Given:
			List<Long> lessonIds = List.of(1L, 2L);
			when(lessonRepository.findIdsWithTeacherVideoIn(lessonIds)).thenReturn(java.util.Set.of(1L));

			// When:
			java.util.Set<Long> result = lessonEntityService.findIdsWithTeacherVideoIn(lessonIds);

			// Then:
			assertThat(result).containsExactly(1L);
		}
	}

	private AppUser appUser(Long id) {
		return new AppUser(id, "clerk-" + id, "user@example.com", "User " + id, "admin", "User", null, null, null);
	}

	private User user(Long id) {
		User user = new User();
		user.setId(id);
		user.setName("User " + id);
		user.setEmail("user" + id + "@example.com");
		return user;
	}

	private Material material(Long id) {
		Material material = new Material();
		material.setId(id);
		return material;
	}

	private LessonStatus status(String code) {
		LessonStatus status = new LessonStatus();
		status.setCode(code);
		return status;
	}

	private LessonPublicationStatus publication(String code) {
		LessonPublicationStatus status = new LessonPublicationStatus();
		status.setCode(code);
		return status;
	}

	private LessonContentFormat format(String code) {
		LessonContentFormat format = new LessonContentFormat();
		format.setCode(code);
		return format;
	}
}
