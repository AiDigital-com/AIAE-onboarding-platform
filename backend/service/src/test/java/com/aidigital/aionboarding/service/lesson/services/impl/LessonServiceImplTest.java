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
import com.aidigital.aionboarding.service.lesson.enums.LessonStatusAction;
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
import com.aidigital.aionboarding.service.lesson.support.LessonMutationSupport;
import com.aidigital.aionboarding.service.lesson.support.LessonRecordAssembler;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonServiceImplTest {

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
	private LessonDetailEnricher lessonDetailEnricher;
	@Mock
	private LessonMutationSupport lessonMutationSupport;
	@Mock
	private CurrentTime currentTime;

	@InjectMocks
	private LessonServiceImpl service;

	private AppUser learnerViewer() {
		return new AppUser(1L, "clerk-1", "learner@test.com", "Learner", "learner", "Learner", null, null, null);
	}

	private AppUser adminViewer() {
		return new AppUser(2L, "clerk-2", "admin@test.com", "Admin", "admin", "Admin", null, null, null);
	}

	@Nested
	class GetAllLessons {

		@Test
		void getAllLessonsBuildsVisibilityFilterFromViewerAndDelegatesToEntityServiceTest() {
			// Given:
			AppUser viewer = adminViewer();
			LessonListQuery query = new LessonListQuery(
					null, null, null, null, null, null, null, null, null, null, LessonSortField.CREATED_AT,
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
	class CountLessons {

		@Test
		void countLessonsBuildsVisibilityFilterFromViewerAndDelegatesToEntityServiceTest() {
			// Given:
			AppUser viewer = adminViewer();
			LessonListQuery query = new LessonListQuery(
					null, null, null, null, null, null, null, null, null, null, LessonSortField.CREATED_AT,
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
	class GetLesson {

		@Test
		void shouldThrowWhenLessonMutationSupportRejectsVisibilityTest() {
			// Given:
			AppUser viewer = learnerViewer();
			Long lessonId = 10L;
			Lesson lesson = mock(Lesson.class);
			when(lessonEntityService.findByIdWithFetches(lessonId)).thenReturn(lesson);
			when(lessonMutationSupport.canView(viewer, lesson)).thenReturn(false);

			// Execution
			AppException thrown = assertThrows(AppException.class, () -> service.getLesson(viewer, lessonId));

			// Verification
			assertThat(thrown.getCode()).isEqualTo(ErrorReason.C001.name());
		}

		@Test
		void shouldRequireLearnerAccessAndDelegateEnrichmentTest() {
			// Given
			AppUser viewer = learnerViewer();
			Long lessonId = 50L;
			Lesson lesson = mock(Lesson.class);
			LessonDetailRecord expectedRecord = mock(LessonDetailRecord.class);

			when(lessonEntityService.findByIdWithFetches(lessonId)).thenReturn(lesson);
			when(lessonMutationSupport.canView(viewer, lesson)).thenReturn(true);
			when(lessonDetailEnricher.toEnrichedDetailRecord(viewer, lesson)).thenReturn(expectedRecord);

			// Execution
			LessonDetailRecord result = service.getLesson(viewer, lessonId);

			// Verification
			assertThat(result).isEqualTo(expectedRecord);
			verify(lessonMutationSupport).requireLearnerAccess(viewer, lesson);
			verify(lessonDetailEnricher).toEnrichedDetailRecord(viewer, lesson);
		}
	}

	@Nested
	class GetLessonGenerationStatus {

		@Test
		void shouldReturnStatusCodeWhenLessonIsVisibleToViewerTest() {
			// Given:
			AppUser viewer = learnerViewer();
			Long lessonId = 60L;
			Lesson lesson = mock(Lesson.class);
			LessonStatus generatingStatus = new LessonStatus();
			generatingStatus.setCode(LessonStatusCode.GENERATING);
			when(lesson.getStatus()).thenReturn(generatingStatus);

			when(lessonEntityService.getReference(lessonId)).thenReturn(lesson);
			when(lessonMutationSupport.canView(viewer, lesson)).thenReturn(true);

			// When:
			String result = service.getLessonGenerationStatus(viewer, lessonId);

			// Then:
			assertThat(result).isEqualTo(LessonStatusCode.GENERATING);
		}

		@Test
		void shouldThrowC001WhenLessonIsNotVisibleToViewerTest() {
			// Given:
			AppUser viewer = learnerViewer();
			Long lessonId = 61L;
			Lesson lesson = mock(Lesson.class);

			when(lessonEntityService.getReference(lessonId)).thenReturn(lesson);
			when(lessonMutationSupport.canView(viewer, lesson)).thenReturn(false);

			// When-Then:
			AppException thrown = assertThrows(AppException.class,
					() -> service.getLessonGenerationStatus(viewer, lessonId));
			assertThat(thrown.getCode()).isEqualTo(ErrorReason.C001.name());
		}
	}

	@Nested
	class UpdateLessonContent {

		@Test
		void shouldApplyUpdateValidateAndSaveTest() {
			// Given:
			AppUser viewer = adminViewer();
			Long lessonId = 50L;
			Lesson lesson = new Lesson();
			lesson.setId(lessonId);
			when(lessonMutationSupport.requireManageable(viewer, lessonId)).thenReturn(lesson);
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
			verify(lessonMutationSupport).applyContentUpdate(viewer, lesson, input);
			verify(lessonMutationSupport).requirePublishableContent(lesson);
			verify(lessonEntityService).clearFailureIfPresent(lesson);
		}
	}

	@Nested
	class ChangeLessonStatus {

		private LessonStatus status(String code) {
			LessonStatus status = new LessonStatus();
			status.setCode(code);
			return status;
		}

		private LessonPublicationStatus publicationStatus(String code) {
			LessonPublicationStatus status = new LessonPublicationStatus();
			status.setCode(code);
			return status;
		}

		private Lesson readyLesson(Long id, String publicationStatusCode) {
			Lesson lesson = new Lesson();
			lesson.setId(id);
			lesson.setTitle("Title");
			lesson.setContentHtml("<p>Body</p>");
			lesson.setStatus(status(LessonStatusCode.READY));
			lesson.setPublicationStatus(publicationStatus(publicationStatusCode));
			return lesson;
		}

		@Test
		void shouldRequirePublishArchivePermissionTest() {
			// Given:
			AppUser viewer = adminViewer();
			Long lessonId = 60L;
			Lesson lesson = readyLesson(lessonId, LessonPublicationStatusCode.PUBLISHED);
			when(lessonMutationSupport.requireManageable(viewer, lessonId)).thenReturn(lesson);
			when(lessonEntityService.findPublicationStatus(LessonPublicationStatusCode.PRIVATE))
					.thenReturn(publicationStatus(LessonPublicationStatusCode.PRIVATE));
			when(lessonEntityService.save(lesson)).thenReturn(lesson);
			when(lessonMapper.toDetailRecord(lesson)).thenReturn(mock(LessonDetailRecord.class));

			// When:
			service.changeLessonStatus(viewer, lessonId, LessonStatusAction.UNPUBLISH);

			// Then:
			verify(permissionService).requirePermission(viewer, PermissionKeys.LESSONS_PUBLISH_ARCHIVE);
		}

		@Test
		void shouldWritePrivateAndLeavePublishedAtUntouchedOnUnpublishTest() {
			// Given: a published lesson with a pre-existing publishedAt timestamp
			AppUser viewer = adminViewer();
			Long lessonId = 61L;
			Lesson lesson = readyLesson(lessonId, LessonPublicationStatusCode.PUBLISHED);
			LocalDateTime originalPublishedAt = LocalDateTime.of(2026, 1, 1, 0, 0);
			lesson.setPublishedAt(originalPublishedAt);
			LessonPublicationStatus privateStatus = publicationStatus(LessonPublicationStatusCode.PRIVATE);
			when(lessonMutationSupport.requireManageable(viewer, lessonId)).thenReturn(lesson);
			when(lessonEntityService.findPublicationStatus(LessonPublicationStatusCode.PRIVATE))
					.thenReturn(privateStatus);
			when(lessonEntityService.save(lesson)).thenReturn(lesson);
			when(lessonMapper.toDetailRecord(lesson)).thenReturn(mock(LessonDetailRecord.class));

			// When:
			service.changeLessonStatus(viewer, lessonId, LessonStatusAction.UNPUBLISH);

			// Then: unpublish writes private but never touches the original publish timestamp
			assertThat(lesson.getPublicationStatus()).isSameAs(privateStatus);
			assertThat(lesson.getPublishedAt()).isEqualTo(originalPublishedAt);
		}

		@Test
		void shouldWritePublishedAndSetPublishedAtOnPublishTest() {
			// Given: a ready, private lesson being published for the first time
			AppUser viewer = adminViewer();
			Long lessonId = 62L;
			Lesson lesson = readyLesson(lessonId, LessonPublicationStatusCode.PRIVATE);
			LessonPublicationStatus publishedStatus = publicationStatus(LessonPublicationStatusCode.PUBLISHED);
			LocalDateTime now = LocalDateTime.of(2026, 2, 1, 0, 0);
			when(lessonMutationSupport.requireManageable(viewer, lessonId)).thenReturn(lesson);
			when(lessonEntityService.findPublicationStatus(LessonPublicationStatusCode.PUBLISHED))
					.thenReturn(publishedStatus);
			when(currentTime.utcDateTime()).thenReturn(now);
			when(lessonEntityService.save(lesson)).thenReturn(lesson);
			when(lessonMapper.toDetailRecord(lesson)).thenReturn(mock(LessonDetailRecord.class));

			// When:
			service.changeLessonStatus(viewer, lessonId, LessonStatusAction.PUBLISH);

			// Then:
			assertThat(lesson.getPublicationStatus()).isSameAs(publishedStatus);
			assertThat(lesson.getPublishedAt()).isEqualTo(now);
			verify(lessonMutationSupport).requirePublishableContent(lesson);
		}

		@Test
		void shouldThrowWhenPublishingALessonThatIsNotReadyTest() {
			// Given:
			AppUser viewer = adminViewer();
			Long lessonId = 63L;
			Lesson lesson = readyLesson(lessonId, LessonPublicationStatusCode.PRIVATE);
			lesson.setStatus(status(LessonStatusCode.DRAFT));
			when(lessonMutationSupport.requireManageable(viewer, lessonId)).thenReturn(lesson);

			// When-Then:
			AppException thrown = assertThrows(AppException.class,
					() -> service.changeLessonStatus(viewer, lessonId, LessonStatusAction.PUBLISH));
			assertThat(thrown.getCode()).isEqualTo(ErrorReason.C002.name());
		}

		@Test
		void shouldWriteArchivedOnArchiveTest() {
			// Given:
			AppUser viewer = adminViewer();
			Long lessonId = 64L;
			Lesson lesson = readyLesson(lessonId, LessonPublicationStatusCode.PUBLISHED);
			LessonPublicationStatus archivedStatus = publicationStatus(LessonPublicationStatusCode.ARCHIVED);
			when(lessonMutationSupport.requireManageable(viewer, lessonId)).thenReturn(lesson);
			when(lessonEntityService.findPublicationStatus(LessonPublicationStatusCode.ARCHIVED))
					.thenReturn(archivedStatus);
			when(lessonEntityService.save(lesson)).thenReturn(lesson);
			when(lessonMapper.toDetailRecord(lesson)).thenReturn(mock(LessonDetailRecord.class));

			// When:
			service.changeLessonStatus(viewer, lessonId, LessonStatusAction.ARCHIVE);

			// Then:
			assertThat(lesson.getPublicationStatus()).isSameAs(archivedStatus);
		}

		@Test
		void shouldWritePrivateOnRestoreTest() {
			// Given: an archived lesson being restored
			AppUser viewer = adminViewer();
			Long lessonId = 65L;
			Lesson lesson = readyLesson(lessonId, LessonPublicationStatusCode.ARCHIVED);
			LessonPublicationStatus privateStatus = publicationStatus(LessonPublicationStatusCode.PRIVATE);
			when(lessonMutationSupport.requireManageable(viewer, lessonId)).thenReturn(lesson);
			when(lessonEntityService.findPublicationStatus(LessonPublicationStatusCode.PRIVATE))
					.thenReturn(privateStatus);
			when(lessonEntityService.save(lesson)).thenReturn(lesson);
			when(lessonMapper.toDetailRecord(lesson)).thenReturn(mock(LessonDetailRecord.class));

			// When:
			service.changeLessonStatus(viewer, lessonId, LessonStatusAction.RESTORE);

			// Then:
			assertThat(lesson.getPublicationStatus()).isSameAs(privateStatus);
		}
	}
}
