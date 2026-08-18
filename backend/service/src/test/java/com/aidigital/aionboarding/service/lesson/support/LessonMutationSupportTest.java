package com.aidigital.aionboarding.service.lesson.support;

import com.aidigital.aionboarding.domain.common.dictionary.LessonPublicationStatusCode;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonPublicationStatus;
import com.aidigital.aionboarding.domain.roadmap.entities.Roadmap;
import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapLesson;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.lesson.models.UpdateLessonContentInput;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonEntityService;
import com.aidigital.aionboarding.service.lessonactivity.support.LessonActivityAccessPolicy;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import com.aidigital.aionboarding.service.roadmap.services.entity.RoadmapEntityService;
import com.aidigital.aionboarding.service.storage.StorageService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
class LessonMutationSupportTest {

	@Mock
	private PermissionService permissionService;
	@Mock
	private LessonEntityService lessonEntityService;
	@Mock
	private LessonActivityAccessPolicy lessonActivityAccessPolicy;
	@Mock
	private StorageService storageService;
	@Mock
	private LessonHtmlSanitizer lessonHtmlSanitizer;
	@Mock
	private RoadmapEntityService roadmapEntityService;

	@InjectMocks
	private LessonMutationSupport support;

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
	class CanView {

		@Test
		void unenrolledLearnerOnPublishedLesson_returnsTrueWithoutThrowingTest() {
			// Given
			AppUser viewer = learnerViewer();
			Lesson lesson = publishedLesson(1L);
			when(permissionService.userHasPermission(viewer, PermissionKeys.LESSONS_MANAGE)).thenReturn(false);

			// Execution
			boolean result = support.canView(viewer, lesson);

			// Verification - must return boolean, never throw
			assertThat(result).isTrue();
			verify(lessonActivityAccessPolicy, never()).requireEnrollment(any(), anyLong());
		}
	}

	@Nested
	class RequireLearnerAccess {

		@Test
		void unenrolledLearner_throwsC001Test() {
			// Given
			AppUser viewer = learnerViewer();
			Lesson lesson = publishedLesson(10L);
			when(permissionService.userHasPermission(viewer, PermissionKeys.LESSONS_MANAGE)).thenReturn(false);
			doThrow(new AppException(com.aidigital.aionboarding.service.common.error.ErrorReason.C001,
					"Lesson is not in My Lessons."))
					.when(lessonActivityAccessPolicy).requireEnrollment(viewer, 10L);

			// Execution
			AppException thrown = org.junit.jupiter.api.Assertions.assertThrows(AppException.class,
					() -> support.requireLearnerAccess(viewer, lesson));

			// Verification
			assertThat(thrown.getCode()).isEqualTo(com.aidigital.aionboarding.service.common.error.ErrorReason.C001.name());
			verify(lessonActivityAccessPolicy).requireEnrollment(viewer, 10L);
		}

		@Test
		void adminViewer_bypassesEnrollmentCheckTest() {
			// Given
			AppUser viewer = adminViewer();
			Lesson lesson = publishedLesson(20L);

			// Execution
			support.requireLearnerAccess(viewer, lesson);

			// Verification
			verify(lessonActivityAccessPolicy, never()).requireEnrollment(any(), anyLong());
		}

		@Test
		void lessonManageHolder_bypassesEnrollmentCheckTest() {
			// Given
			AppUser viewer = learnerViewer();
			Lesson lesson = publishedLesson(30L);
			when(permissionService.userHasPermission(viewer, PermissionKeys.LESSONS_MANAGE)).thenReturn(true);

			// Execution
			support.requireLearnerAccess(viewer, lesson);

			// Verification
			verify(lessonActivityAccessPolicy, never()).requireEnrollment(any(), anyLong());
		}

		@Test
		void enrolledLearner_doesNotThrowTest() {
			// Given
			AppUser viewer = learnerViewer();
			Lesson lesson = publishedLesson(40L);
			when(permissionService.userHasPermission(viewer, PermissionKeys.LESSONS_MANAGE)).thenReturn(false);
			// requireEnrollment does NOT throw - viewer is enrolled

			// Execution
			support.requireLearnerAccess(viewer, lesson);

			// Verification
			verify(lessonActivityAccessPolicy).requireEnrollment(viewer, 40L);
		}
	}

	@Nested
	class RequireManageable {

		@Test
		void shouldReturnLessonWhenViewerCanManageTest() {
			// Given:
			AppUser viewer = adminViewer();
			Long lessonId = 50L;
			Lesson lesson = new Lesson();
			lesson.setId(lessonId);
			when(lessonEntityService.getReference(lessonId)).thenReturn(lesson);
			when(permissionService.canManageExistingLesson(viewer, null)).thenReturn(true);

			// When:
			Lesson result = support.requireManageable(viewer, lessonId);

			// Then:
			assertThat(result).isSameAs(lesson);
		}

		@Test
		void shouldThrowWhenViewerCannotManageTest() {
			// Given:
			AppUser viewer = learnerViewer();
			Long lessonId = 51L;
			Lesson lesson = new Lesson();
			lesson.setId(lessonId);
			when(lessonEntityService.getReference(lessonId)).thenReturn(lesson);
			when(permissionService.canManageExistingLesson(viewer, null)).thenReturn(false);

			// When-Then:
			assertThatThrownBy(() -> support.requireManageable(viewer, lessonId))
					.isInstanceOf(AppException.class);
		}
	}

	@Nested
	class ApplyContentUpdate {

		@Test
		void shouldConfirmUploadWhenCoverImageStorageKeyChangesTest() {
			// Given:
			AppUser viewer = adminViewer();
			Lesson lesson = new Lesson();
			lesson.setCoverImageStorageKey("old-key");
			UpdateLessonContentInput input = new UpdateLessonContentInput(
					Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
					Optional.of("new-key"), Optional.empty(), Optional.empty()
			);

			// When:
			support.applyContentUpdate(viewer, lesson, input);

			// Then:
			assertThat(lesson.getCoverImageStorageKey()).isEqualTo("new-key");
			verify(storageService).confirmUpload(viewer, "new-key");
		}

		@Test
		void shouldNotReconfirmAnUnchangedCoverImageStorageKeyTest() {
			// Given:
			AppUser viewer = adminViewer();
			Lesson lesson = new Lesson();
			lesson.setCoverImageStorageKey("same-key");
			UpdateLessonContentInput input = new UpdateLessonContentInput(
					Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
					Optional.of("same-key"), Optional.empty(), Optional.empty()
			);

			// When:
			support.applyContentUpdate(viewer, lesson, input);

			// Then:
			verifyNoInteractions(storageService);
		}

		@Test
		void shouldPersistTheSanitizerOutputRatherThanRawSubmittedHtmlTest() {
			// Given: a manually-authored update containing a script tag the sanitizer would strip.
			AppUser viewer = adminViewer();
			Lesson lesson = new Lesson();
			when(lessonHtmlSanitizer.sanitize("<p>New content</p><script>evil()</script>"))
					.thenReturn("<p>New content</p>");
			UpdateLessonContentInput input = new UpdateLessonContentInput(
					Optional.empty(), Optional.empty(), Optional.of("<p>New content</p><script>evil()</script>"),
					Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()
			);

			// When:
			support.applyContentUpdate(viewer, lesson, input);

			// Then:
			assertThat(lesson.getContentHtml()).isEqualTo("<p>New content</p>");
		}

		@Test
		void shouldUpdateTagsWhenPresentTest() {
			// Given:
			AppUser viewer = adminViewer();
			Lesson lesson = new Lesson();
			UpdateLessonContentInput input = new UpdateLessonContentInput(
					Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(List.of("tag1", "tag2")),
					Optional.empty(), Optional.empty(), Optional.empty()
			);

			// When:
			support.applyContentUpdate(viewer, lesson, input);

			// Then:
			assertThat(lesson.getTags()).containsExactly("tag1", "tag2");
		}
	}

	@Nested
	class RequireNoRoadmapUsage {

		@Test
		void shouldNotThrowWhenLessonIsNotUsedByAnyRoadmapTest() {
			// Given:
			when(roadmapEntityService.findByIdLessonId(1L)).thenReturn(List.of());

			// When-Then:
			support.requireNoRoadmapUsage(1L);
		}

		@Test
		void shouldThrowWithRoadmapTitlesWhenLessonIsUsedTest() {
			// Given:
			Roadmap roadmap = new Roadmap();
			roadmap.setTitle("Onboarding basics");
			RoadmapLesson usage = new RoadmapLesson();
			usage.setRoadmap(roadmap);
			when(roadmapEntityService.findByIdLessonId(1L)).thenReturn(List.of(usage));

			// When-Then:
			assertThatThrownBy(() -> support.requireNoRoadmapUsage(1L))
					.isInstanceOf(AppException.class)
					.hasMessageContaining("Onboarding basics");
		}
	}

	@Nested
	class RequirePublishableContent {

		@Test
		void shouldThrowWhenTitleIsBlankTest() {
			// Given:
			Lesson lesson = new Lesson();
			lesson.setTitle("");
			lesson.setContentHtml("<p>content</p>");

			// When-Then:
			assertThatThrownBy(() -> support.requirePublishableContent(lesson))
					.isInstanceOf(AppException.class);
		}

		@Test
		void shouldThrowWhenContentIsEmptyTest() {
			// Given:
			Lesson lesson = new Lesson();
			lesson.setTitle("A title");
			lesson.setContentHtml("");
			lesson.setContentMarkdown("");

			// When-Then:
			assertThatThrownBy(() -> support.requirePublishableContent(lesson))
					.isInstanceOf(AppException.class);
		}

		@Test
		void shouldNotThrowWhenTitleAndContentArePresentTest() {
			// Given:
			Lesson lesson = new Lesson();
			lesson.setTitle("A title");
			lesson.setContentHtml("<p>content</p>");

			// When-Then:
			support.requirePublishableContent(lesson);
		}
	}
}
