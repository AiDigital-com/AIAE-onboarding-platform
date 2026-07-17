package com.aidigital.aionboarding.service.lesson.services.impl;

import com.aidigital.aionboarding.domain.common.dictionary.LessonPublicationStatusCode;
import com.aidigital.aionboarding.domain.common.dictionary.LessonStatusCode;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonPublicationStatus;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonStatus;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.lesson.repositories.LessonSearchSummaryProjection;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.lesson.models.LessonDetailRecord;
import com.aidigital.aionboarding.service.lesson.models.LessonListQuery;
import com.aidigital.aionboarding.service.lesson.models.LessonSearchSummaryRecord;
import com.aidigital.aionboarding.service.lesson.models.LessonSortField;
import com.aidigital.aionboarding.service.lesson.models.LessonVisibilityFilter;
import com.aidigital.aionboarding.service.lesson.models.UpdateLessonContentInput;
import com.aidigital.aionboarding.service.lesson.services.LessonAssetService;
import com.aidigital.aionboarding.service.lesson.services.LessonInitialGenerationService;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonEntityService;
import com.aidigital.aionboarding.service.lesson.support.LessonDetailEnricher;
import com.aidigital.aionboarding.service.lesson.support.LessonHtmlSanitizer;
import com.aidigital.aionboarding.service.lesson.support.LessonRecordAssembler;
import com.aidigital.aionboarding.service.lessonactivity.support.LessonActivityAccessPolicy;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import com.aidigital.aionboarding.service.roadmap.services.entity.RoadmapEntityService;
import com.aidigital.aionboarding.service.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonServiceImplTest {

	@Mock
	private RoadmapEntityService roadmapEntityService;
	@Mock
	private PermissionService permissionService;
	@Mock
	private LessonRecordAssembler lessonMapper;
	@Mock
	private LessonAssetService lessonAssetService;
	@Mock
	private LessonInitialGenerationService lessonInitialGenerationService;
	@Mock
	private LessonEntityService lessonEntityService;
	@Mock
	private LessonActivityAccessPolicy lessonActivityAccessPolicy;
	@Mock
	private LessonDetailEnricher lessonDetailEnricher;
	@Mock
	private StorageService storageService;
	@Mock
	private CurrentTime currentTime;
	@Mock
	private LessonHtmlSanitizer lessonHtmlSanitizer;

	@InjectMocks
	private LessonServiceImpl service;

	@BeforeEach
	void setUp() {
		lenient().when(lessonHtmlSanitizer.sanitize(org.mockito.ArgumentMatchers.anyString()))
				.thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Nested
	class getAllLessons {

		@Test
		void getAllLessonsBuildsVisibilityFilterFromViewerAndDelegatesToEntityServiceTest() {
			// Given:
			AppUser viewer = adminViewer();
			LessonListQuery query = new LessonListQuery(
					null, null, null, null, null, null, null, null, null, LessonSortField.CREATED_AT,
					Sort.Direction.DESC
			);
			LessonVisibilityFilter expectedFilter = new LessonVisibilityFilter(true, false, 2L);
			LessonSearchSummaryProjection projection = mock(LessonSearchSummaryProjection.class);
			LessonSearchSummaryRecord summary = mock(LessonSearchSummaryRecord.class);
			Page<LessonSearchSummaryProjection> projectionPage = new PageImpl<>(List.of(projection));

			when(permissionService.userHasPermission(viewer, PermissionKeys.LESSONS_MANAGE)).thenReturn(false);
			when(lessonEntityService.searchSummaries(query, expectedFilter, 0, 20)).thenReturn(projectionPage);
			when(lessonMapper.toListItemRecord(projection)).thenReturn(summary);

			// When:
			Page<LessonSearchSummaryRecord> result = service.getAllLessons(viewer, query, 0, 20);

			// Then:
			assertThat(result.getContent()).containsExactly(summary);
			verify(lessonEntityService).searchSummaries(query, expectedFilter, 0, 20);
		}
	}

	@Nested
	class countLessons {

		@Test
		void countLessonsBuildsVisibilityFilterFromViewerAndDelegatesToEntityServiceTest() {
			// Given:
			AppUser viewer = adminViewer();
			LessonListQuery query = new LessonListQuery(
					null, null, null, null, null, null, null, null, null, LessonSortField.CREATED_AT,
					Sort.Direction.DESC
			);
			LessonVisibilityFilter expectedFilter = new LessonVisibilityFilter(true, false, 2L);

			when(permissionService.userHasPermission(viewer, PermissionKeys.LESSONS_MANAGE)).thenReturn(false);
			when(lessonEntityService.countSummaries(query, expectedFilter)).thenReturn(11L);

			// When:
			long result = service.countLessons(viewer, query);

			// Then:
			assertThat(result).isEqualTo(11L);
			verify(lessonEntityService).countSummaries(query, expectedFilter);
		}
	}

	@Nested
	class getLesson {

		@Test
		void getLesson_unenrolledLearner_throwsC001() {
			// Given
			AppUser viewer = learnerViewer();
			Long lessonId = 10L;
			Lesson lesson = publishedLesson(lessonId);

			when(lessonEntityService.findByIdWithFetches(lessonId)).thenReturn(lesson);
			when(permissionService.userHasPermission(viewer, PermissionKeys.LESSONS_MANAGE)).thenReturn(false);
			doThrow(new AppException(ErrorReason.C001, "Lesson is not in My Lessons."))
					.when(lessonActivityAccessPolicy).requireEnrollment(viewer, lessonId);

			// Execution
			AppException thrown = assertThrows(AppException.class, () -> service.getLesson(viewer, lessonId));

			// Verification
			assertThat(thrown.getCode()).isEqualTo(ErrorReason.C001.name());
			verify(lessonActivityAccessPolicy).requireEnrollment(viewer, lessonId);
		}

		@Test
		void getLesson_adminViewerOnUnpublishedLesson_returnsDetailWithoutEnrollmentCheck() {
			// Given
			AppUser viewer = adminViewer();
			Long lessonId = 20L;
			Lesson lesson = mock(Lesson.class);
			LessonPublicationStatus pubStatus = new LessonPublicationStatus();
			pubStatus.setCode(LessonPublicationStatusCode.PRIVATE);
			lenient().when(lesson.getPublicationStatus()).thenReturn(pubStatus);
			LessonDetailRecord expectedRecord = mock(LessonDetailRecord.class);

			when(lessonEntityService.findByIdWithFetches(lessonId)).thenReturn(lesson);
			when(lessonDetailEnricher.toEnrichedDetailRecord(viewer, lesson)).thenReturn(expectedRecord);

			// Execution
			LessonDetailRecord result = service.getLesson(viewer, lessonId);

			// Verification
			assertThat(result).isEqualTo(expectedRecord);
			verify(lessonActivityAccessPolicy, never()).requireEnrollment(any(), anyLong());
		}

		@Test
		void getLesson_lessonManageHolder_returnsDetailWithoutEnrollmentCheck() {
			// Given
			AppUser viewer = learnerViewer();
			Long lessonId = 30L;
			Lesson lesson = publishedLesson(lessonId);
			LessonDetailRecord expectedRecord = mock(LessonDetailRecord.class);

			when(lessonEntityService.findByIdWithFetches(lessonId)).thenReturn(lesson);
			when(permissionService.userHasPermission(viewer, PermissionKeys.LESSONS_MANAGE)).thenReturn(true);
			when(lessonDetailEnricher.toEnrichedDetailRecord(viewer, lesson)).thenReturn(expectedRecord);

			// Execution
			LessonDetailRecord result = service.getLesson(viewer, lessonId);

			// Verification
			assertThat(result).isEqualTo(expectedRecord);
			verify(lessonActivityAccessPolicy, never()).requireEnrollment(any(), anyLong());
		}

		/**
		 * Regression test: a LESSONS_MANAGE holder must be able to open a published lesson
		 * authored by someone else. The Library list already shows such lessons via
		 * {@code canView}'s published-lesson fallback, so rejecting the open with C001
		 * ("Lesson is not in My Lessons") was a bug — a manager could see a lesson card they
		 * could not actually open.
		 */
		@Test
		void getLesson_lessonManageHolderNotLessonCreator_returnsDetailWithoutEnrollmentCheckTest() {
			// Given
			AppUser viewer = learnerViewer();
			Long lessonId = 31L;
			Lesson lesson = publishedLesson(lessonId);
			com.aidigital.aionboarding.domain.user.entities.User otherCreator =
					mock(com.aidigital.aionboarding.domain.user.entities.User.class);
			lenient().when(otherCreator.getId()).thenReturn(viewer.internalId() + 1);
			lenient().when(lesson.getCreatedByUser()).thenReturn(otherCreator);
			LessonDetailRecord expectedRecord = mock(LessonDetailRecord.class);

			when(lessonEntityService.findByIdWithFetches(lessonId)).thenReturn(lesson);
			when(permissionService.userHasPermission(viewer, PermissionKeys.LESSONS_MANAGE)).thenReturn(true);
			when(lessonDetailEnricher.toEnrichedDetailRecord(viewer, lesson)).thenReturn(expectedRecord);

			// Execution
			LessonDetailRecord result = service.getLesson(viewer, lessonId);

			// Verification: canView's own ownership branch still runs (and fails, since the
			// viewer isn't the creator) but its published-lesson fallback grants visibility, and
			// requireLearnerAccess bypasses the enrollment check for any LESSONS_MANAGE holder.
			assertThat(result).isEqualTo(expectedRecord);
			verify(lessonActivityAccessPolicy, never()).requireEnrollment(any(), anyLong());
		}

		@Test
		void getLesson_enrolledLearnerOnPublishedLesson_returnsDetail() {
			// Given
			AppUser viewer = learnerViewer();
			Long lessonId = 40L;
			Lesson lesson = publishedLesson(lessonId);
			LessonDetailRecord expectedRecord = mock(LessonDetailRecord.class);

			when(lessonEntityService.findByIdWithFetches(lessonId)).thenReturn(lesson);
			when(permissionService.userHasPermission(viewer, PermissionKeys.LESSONS_MANAGE)).thenReturn(false);
			// requireEnrollment does NOT throw - viewer is enrolled
			when(lessonDetailEnricher.toEnrichedDetailRecord(viewer, lesson)).thenReturn(expectedRecord);

			// Execution
			LessonDetailRecord result = service.getLesson(viewer, lessonId);

			// Verification
			assertThat(result).isEqualTo(expectedRecord);
			verify(lessonActivityAccessPolicy).requireEnrollment(viewer, lessonId);
		}

		@Test
		void getLesson_enrolledLearner_delegatesDetailEnrichment() {
			// Given
			AppUser viewer = learnerViewer();
			Long lessonId = 50L;
			Lesson lesson = publishedLesson(lessonId);
			LessonDetailRecord expectedRecord = mock(LessonDetailRecord.class);

			when(lessonEntityService.findByIdWithFetches(lessonId)).thenReturn(lesson);
			when(permissionService.userHasPermission(viewer, PermissionKeys.LESSONS_MANAGE)).thenReturn(false);
			// enrolled - no throw from requireEnrollment
			when(lessonDetailEnricher.toEnrichedDetailRecord(viewer, lesson)).thenReturn(expectedRecord);

			// Execution
			LessonDetailRecord result = service.getLesson(viewer, lessonId);

			// Verification
			assertThat(result).isEqualTo(expectedRecord);
			verify(lessonDetailEnricher).toEnrichedDetailRecord(viewer, lesson);
		}
	}

	@Nested
	class getLessonGenerationStatus {

		@Test
		void shouldReturnStatusCodeWhenLessonIsVisibleToViewerTest() {
			// Given: a published lesson visible to any viewer via canView's published fallback
			AppUser viewer = learnerViewer();
			Long lessonId = 60L;
			Lesson lesson = publishedLesson(lessonId);
			LessonStatus generatingStatus = new LessonStatus();
			generatingStatus.setCode(LessonStatusCode.GENERATING);
			lenient().when(lesson.getStatus()).thenReturn(generatingStatus);

			when(lessonEntityService.getReference(lessonId)).thenReturn(lesson);

			// When:
			String result = service.getLessonGenerationStatus(viewer, lessonId);

			// Then:
			assertThat(result).isEqualTo(LessonStatusCode.GENERATING);
		}

		@Test
		void shouldThrowC001WhenLessonIsNotVisibleToViewerTest() {
			// Given: a private lesson the viewer neither owns nor manages
			AppUser viewer = learnerViewer();
			Long lessonId = 61L;
			Lesson lesson = mock(Lesson.class);
			LessonPublicationStatus privateStatus = new LessonPublicationStatus();
			privateStatus.setCode(LessonPublicationStatusCode.PRIVATE);
			lenient().when(lesson.getPublicationStatus()).thenReturn(privateStatus);
			lenient().when(lesson.getCreatedByUser()).thenReturn(null);

			when(lessonEntityService.getReference(lessonId)).thenReturn(lesson);
			when(permissionService.userHasPermission(viewer, PermissionKeys.LESSONS_MANAGE)).thenReturn(false);

			// When-Then:
			AppException thrown = assertThrows(AppException.class,
					() -> service.getLessonGenerationStatus(viewer, lessonId));
			assertThat(thrown.getCode()).isEqualTo(ErrorReason.C001.name());
		}
	}

	@Nested
	class canView {

		@Test
		void canView_unenrolledLearnerOnPublishedLesson_returnsTrueWithoutThrowing() {
			// Given
			AppUser viewer = learnerViewer();
			Lesson lesson = publishedLesson(1L);
			when(permissionService.userHasPermission(viewer, PermissionKeys.LESSONS_MANAGE)).thenReturn(false);

			// Execution
			boolean result = service.canView(viewer, lesson);

			// Verification - must return boolean, never throw
			assertThat(result).isTrue();
			verify(lessonActivityAccessPolicy, never()).requireEnrollment(any(), anyLong());
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

	private Lesson publishedLesson(Long id) {
		Lesson lesson = mock(Lesson.class);
		LessonPublicationStatus pubStatus = new LessonPublicationStatus();
		pubStatus.setCode(LessonPublicationStatusCode.PUBLISHED);
		lenient().when(lesson.getPublicationStatus()).thenReturn(pubStatus);
		lenient().when(lesson.getCreatedByUser()).thenReturn(null);
		lenient().when(lesson.getId()).thenReturn(id);
		return lesson;
	}

	@Nested
	class UpdateLessonContent {

		@Test
		void shouldConfirmUploadWhenCoverImageStorageKeyChangesTest() {
			// Given:
			AppUser viewer = adminViewer();
			Long lessonId = 50L;
			Lesson lesson = new Lesson();
			lesson.setId(lessonId);
			lesson.setTitle("Existing title");
			lesson.setContentHtml("<p>Existing content</p>");
			lesson.setCoverImageStorageKey("old-key");
			when(lessonEntityService.getReference(lessonId)).thenReturn(lesson);
			when(permissionService.canManageExistingLesson(viewer, null)).thenReturn(true);
			when(lessonEntityService.save(lesson)).thenReturn(lesson);
			LessonDetailRecord expectedRecord = mock(LessonDetailRecord.class);
			when(lessonMapper.toDetailRecord(lesson)).thenReturn(expectedRecord);

			UpdateLessonContentInput input = new UpdateLessonContentInput(
					Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
					Optional.of("new-key"), Optional.empty(), Optional.empty()
			);

			// When:
			LessonDetailRecord result = service.updateLessonContent(viewer, lessonId, input);

			// Then:
			assertThat(result).isSameAs(expectedRecord);
			assertThat(lesson.getCoverImageStorageKey()).isEqualTo("new-key");
			verify(storageService).confirmUpload(viewer, "new-key");
		}

		@Test
		void shouldNotReconfirmAnUnchangedCoverImageStorageKeyTest() {
			// Given:
			AppUser viewer = adminViewer();
			Long lessonId = 51L;
			Lesson lesson = new Lesson();
			lesson.setId(lessonId);
			lesson.setTitle("Existing title");
			lesson.setContentHtml("<p>Existing content</p>");
			lesson.setCoverImageStorageKey("same-key");
			when(lessonEntityService.getReference(lessonId)).thenReturn(lesson);
			when(permissionService.canManageExistingLesson(viewer, null)).thenReturn(true);
			when(lessonEntityService.save(lesson)).thenReturn(lesson);
			when(lessonMapper.toDetailRecord(lesson)).thenReturn(mock(LessonDetailRecord.class));

			UpdateLessonContentInput input = new UpdateLessonContentInput(
					Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
					Optional.of("same-key"), Optional.empty(), Optional.empty()
			);

			// When:
			service.updateLessonContent(viewer, lessonId, input);

			// Then:
			verifyNoInteractions(storageService);
		}

		@Test
		void shouldClearFailureWhenSavingContentOnAFailedLessonTest() {
			// Given: a lesson stuck in "failed" status (e.g. from an OpenAI timeout) whose
			// content the user manually rewrote from the failed-lesson recovery UI.
			AppUser viewer = adminViewer();
			Long lessonId = 52L;
			Lesson lesson = new Lesson();
			lesson.setId(lessonId);
			lesson.setTitle("Existing title");
			lesson.setContentHtml("");
			when(lessonEntityService.getReference(lessonId)).thenReturn(lesson);
			when(permissionService.canManageExistingLesson(viewer, null)).thenReturn(true);
			when(lessonEntityService.save(lesson)).thenReturn(lesson);
			when(lessonMapper.toDetailRecord(lesson)).thenReturn(mock(LessonDetailRecord.class));

			UpdateLessonContentInput input = new UpdateLessonContentInput(
					Optional.empty(), Optional.empty(), Optional.of("<p>Manually written content</p>"),
					Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()
			);

			// When:
			service.updateLessonContent(viewer, lessonId, input);

			// Then:
			verify(lessonEntityService).clearFailureIfPresent(lesson);
		}

		@Test
		void shouldPersistTheSanitizerOutputRatherThanRawSubmittedHtmlTest() {
			// Given: a manually-authored update containing a script tag the sanitizer would strip.
			AppUser viewer = adminViewer();
			Long lessonId = 53L;
			Lesson lesson = new Lesson();
			lesson.setId(lessonId);
			lesson.setTitle("Existing title");
			lesson.setContentHtml("<p>Existing content</p>");
			when(lessonEntityService.getReference(lessonId)).thenReturn(lesson);
			when(permissionService.canManageExistingLesson(viewer, null)).thenReturn(true);
			when(lessonEntityService.save(lesson)).thenReturn(lesson);
			when(lessonMapper.toDetailRecord(lesson)).thenReturn(mock(LessonDetailRecord.class));
			when(lessonHtmlSanitizer.sanitize("<p>New content</p><script>evil()</script>"))
					.thenReturn("<p>New content</p>");

			UpdateLessonContentInput input = new UpdateLessonContentInput(
					Optional.empty(), Optional.empty(), Optional.of("<p>New content</p><script>evil()</script>"),
					Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()
			);

			// When:
			service.updateLessonContent(viewer, lessonId, input);

			// Then:
			assertThat(lesson.getContentHtml()).isEqualTo("<p>New content</p>");
		}
	}
}
