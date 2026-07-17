package com.aidigital.aionboarding.controllers;

import com.aidigital.aionboarding.api.v1.model.ActivityProgressResponseV1;
import com.aidigital.aionboarding.api.v1.model.ActivityResponseV1;
import com.aidigital.aionboarding.api.v1.model.AddLessonAssetRequestV1;
import com.aidigital.aionboarding.api.v1.model.AskLessonRequestV1;
import com.aidigital.aionboarding.api.v1.model.AskLessonResponseV1;
import com.aidigital.aionboarding.api.v1.model.AssignmentRequestV1;
import com.aidigital.aionboarding.api.v1.model.AssignmentResponseV1;
import com.aidigital.aionboarding.api.v1.model.ChangeLessonStatusRequestV1;
import com.aidigital.aionboarding.api.v1.model.ChatMessageV1;
import com.aidigital.aionboarding.api.v1.model.CountResponseV1;
import com.aidigital.aionboarding.api.v1.model.CreateLessonRequestV1;
import com.aidigital.aionboarding.api.v1.model.EnrollmentResponseV1;
import com.aidigital.aionboarding.api.v1.model.GenerateActivityRequestV1;
import com.aidigital.aionboarding.api.v1.model.LearningAssigneesResponseV1;
import com.aidigital.aionboarding.api.v1.model.LessonActivitiesResponseV1;
import com.aidigital.aionboarding.api.v1.model.LessonAssetResponseV1;
import com.aidigital.aionboarding.api.v1.model.LessonAssistantConversationResponseV1;
import com.aidigital.aionboarding.api.v1.model.LessonDetailResponseV1;
import com.aidigital.aionboarding.api.v1.model.LessonGenerationStatusResponseV1;
import com.aidigital.aionboarding.api.v1.model.LessonPublishActionV1;
import com.aidigital.aionboarding.api.v1.model.LessonResponseV1;
import com.aidigital.aionboarding.api.v1.model.LessonSummaryV1;
import com.aidigital.aionboarding.api.v1.model.LessonsListResponseV1;
import com.aidigital.aionboarding.api.v1.model.OkIdResponseV1;
import com.aidigital.aionboarding.api.v1.model.OkResponseV1;
import com.aidigital.aionboarding.api.v1.model.ReviseLessonRequestV1;
import com.aidigital.aionboarding.api.v1.model.SearchLessonsV1;
import com.aidigital.aionboarding.api.v1.model.SetLessonCompletionRequestV1;
import com.aidigital.aionboarding.api.v1.model.SubmitActivityProgressRequestV1;
import com.aidigital.aionboarding.api.v1.model.TeacherVideoResponseV1;
import com.aidigital.aionboarding.api.v1.model.UpdateActivityRequestV1;
import com.aidigital.aionboarding.api.v1.model.UpdateLessonContentRequestV1;
import com.aidigital.aionboarding.api.v1.model.UploadUrlRequestV1;
import com.aidigital.aionboarding.api.v1.model.UploadUrlResponseV1;
import com.aidigital.aionboarding.api.v1.model.UploadedFileResponseV1;
import com.aidigital.aionboarding.mappers.common.LessonAssistantPresetApiMapper;
import com.aidigital.aionboarding.mappers.learning.LearningApiMapper;
import com.aidigital.aionboarding.mappers.lesson.LessonApiMapper;
import com.aidigital.aionboarding.mappers.lessonactivity.LessonActivityApiMapper;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.learning.models.LearningAssigneeRecord;
import com.aidigital.aionboarding.service.learning.models.LessonAssignmentResultRecord;
import com.aidigital.aionboarding.service.learning.models.LessonEnrollmentResultRecord;
import com.aidigital.aionboarding.service.learning.services.LearningEnrollmentService;
import com.aidigital.aionboarding.service.learning.services.LearningService;
import com.aidigital.aionboarding.service.lesson.enums.LessonAssistantPreset;
import com.aidigital.aionboarding.service.lesson.enums.LessonStatusAction;
import com.aidigital.aionboarding.service.lesson.models.AskLessonResultRecord;
import com.aidigital.aionboarding.service.lesson.models.ChatTurn;
import com.aidigital.aionboarding.service.lesson.models.CreateLessonAssetInput;
import com.aidigital.aionboarding.service.lesson.models.CreateLessonInput;
import com.aidigital.aionboarding.service.lesson.models.LessonAssetDeleteResultRecord;
import com.aidigital.aionboarding.service.lesson.models.LessonAssetResultRecord;
import com.aidigital.aionboarding.service.lesson.models.LessonAssistantConversationRecord;
import com.aidigital.aionboarding.service.lesson.models.LessonDetailRecord;
import com.aidigital.aionboarding.service.lesson.models.LessonDetailResultRecord;
import com.aidigital.aionboarding.service.lesson.models.LessonListQuery;
import com.aidigital.aionboarding.service.lesson.models.LessonSummaryRecord;
import com.aidigital.aionboarding.service.lesson.models.ReviseLessonInput;
import com.aidigital.aionboarding.service.lesson.models.RevisionResultRecord;
import com.aidigital.aionboarding.service.lesson.models.TeacherVideoResultRecord;
import com.aidigital.aionboarding.service.lesson.models.UpdateLessonContentInput;
import com.aidigital.aionboarding.service.lesson.services.LessonAssistantService;
import com.aidigital.aionboarding.service.lesson.services.LessonRevisionService;
import com.aidigital.aionboarding.service.lesson.services.LessonService;
import com.aidigital.aionboarding.service.lessonactivity.models.GenerateActivityResultRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.LessonActivityRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.SubmitActivityProgressResultRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.UpdateActivityInput;
import com.aidigital.aionboarding.service.lessonactivity.models.UpdateActivityResultRecord;
import com.aidigital.aionboarding.service.lessonactivity.services.LessonActivityService;
import com.aidigital.aionboarding.service.material.services.UploadValidator;
import com.aidigital.aionboarding.service.storage.StorageService;
import com.aidigital.aionboarding.service.storage.enums.UploadPurpose;
import com.aidigital.aionboarding.service.teachervideo.services.TeacherVideoService;
import com.aidigital.aionboarding.support.ApiResponses;
import org.instancio.Instancio;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.Select.field;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonsControllerTest {

	@Mock
	private CurrentUserSupport currentUser;
	@Mock
	private LessonService lessonService;
	@Mock
	private LessonRevisionService lessonRevisionService;
	@Mock
	private LessonAssistantService lessonAssistantService;
	@Mock
	private TeacherVideoService teacherVideoService;
	@Mock
	private LessonActivityService lessonActivityService;
	@Mock
	private LearningService learningService;
	@Mock
	private LearningEnrollmentService learningEnrollmentService;
	@Mock
	private StorageService storageService;
	@Mock
	private UploadValidator uploadValidator;
	@Mock
	private LessonApiMapper lessonApiMapper;
	@Mock
	private LessonAssistantPresetApiMapper lessonAssistantPresetApiMapper;
	@Mock
	private LessonActivityApiMapper lessonActivityApiMapper;
	@Mock
	private LearningApiMapper learningApiMapper;
	@Mock
	private ApiResponses apiResponses;

	@InjectMocks
	private LessonsController controller;

	@Test
	void shouldCountLessonsAndWrapTheTotalTest() {
		// Given:
		AppUser viewer = viewer();
		SearchLessonsV1 request = Instancio.create(SearchLessonsV1.class);
		LessonListQuery query = Instancio.create(LessonListQuery.class);
		CountResponseV1 expectedBody = Instancio.create(CountResponseV1.class);
		when(currentUser.requireUser()).thenReturn(viewer);
		when(lessonApiMapper.toLessonListQuery(request)).thenReturn(query);
		when(lessonService.countLessons(viewer, query)).thenReturn(9L);
		when(lessonApiMapper.toCountResponseV1(9L)).thenReturn(expectedBody);

		// When:
		ResponseEntity<CountResponseV1> response = controller.countLessons(request);

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
	}

	@Test
	void shouldGetLessonGenerationStatusAndWrapTheStatusTest() {
		// Given:
		AppUser viewer = viewer();
		LessonGenerationStatusResponseV1 expectedBody = Instancio.create(LessonGenerationStatusResponseV1.class);
		when(currentUser.requireUser()).thenReturn(viewer);
		when(lessonService.getLessonGenerationStatus(viewer, 5L)).thenReturn("generating");
		when(lessonApiMapper.toLessonGenerationStatusResponseV1("generating")).thenReturn(expectedBody);

		// When:
		ResponseEntity<LessonGenerationStatusResponseV1> response = controller.getLessonGenerationStatus(5L);

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
	}

	@Test
	void shouldCreateLessonUploadUrlWithPresignedUploadTest() {
		// Given:
		AppUser viewer = viewer();
		UploadUrlRequestV1 request = Instancio.of(UploadUrlRequestV1.class)
				.set(field("fileName"), "clip.mp4")
				.set(field("contentType"), "video/mp4")
				.set(field("size"), 2048L)
				.create();
		StorageService.PresignedUpload presigned = Instancio.of(StorageService.PresignedUpload.class)
				.set(field("uploadUrl"), "https://bucket/presigned")
				.set(field("storageKey"), "uploads/abc/clip.mp4")
				.create();
		UploadUrlResponseV1 expectedBody = Instancio.create(UploadUrlResponseV1.class);
		when(currentUser.requireUser()).thenReturn(viewer);
		when(storageService.presignPut(viewer, UploadPurpose.LESSON_ASSET, "clip.mp4", "video/mp4", 2048L)).thenReturn(presigned);
		when(apiResponses.uploadUrl("https://bucket/presigned", "uploads/abc/clip.mp4")).thenReturn(expectedBody);

		// When:
		ResponseEntity<UploadUrlResponseV1> response = controller.createLessonUploadUrl(request);

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
	}

	@Test
	void shouldUploadLessonFileWithoutBufferingIntoByteArrayTest() throws Exception {
		// Given:
		AppUser viewer = viewer();
		MockMultipartFile file = new MockMultipartFile("file", "diagram.png", "image/png", "content".getBytes());
		UploadValidator.UploadValidationRecord uploadMeta = Instancio.of(UploadValidator.UploadValidationRecord.class)
				.set(field("originalName"), "diagram.png")
				.set(field("mimeType"), "image/png")
				.set(field("sizeBytes"), 7L)
				.create();
		UploadedFileResponseV1 expectedBody = Instancio.create(UploadedFileResponseV1.class);
		when(currentUser.requireUser()).thenReturn(viewer);
		when(uploadValidator.validate(file)).thenReturn(uploadMeta);
		when(storageService.putObjectStreaming(
				eq(viewer), eq(UploadPurpose.LESSON_ASSET), any(InputStream.class), eq(7L), eq("diagram.png"), eq(
						"image/png")))
				.thenReturn("uploads/def/diagram.png");
		when(lessonApiMapper.toUploadedFileResponseV1("uploads/def/diagram.png", "diagram.png", "image/png", 7L))
				.thenReturn(expectedBody);

		// When:
		ResponseEntity<UploadedFileResponseV1> response = controller.uploadLessonFile(file);

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
	}

	@Nested
	class SearchLessons {

		@Test
		void shouldReturnLessonListWithEnrollmentInfoTest() {
			// Given:
			AppUser viewer = viewer();
			SearchLessonsV1 request = Instancio.create(SearchLessonsV1.class);
			LessonListQuery query = Instancio.create(LessonListQuery.class);
			LessonSummaryV1 summary = Instancio.of(LessonSummaryV1.class)
					.set(field("id"), 10L)
					.create();
			LessonsListResponseV1 responseV1 = Instancio.of(LessonsListResponseV1.class)
					.set(field("lessons"), List.of(summary))
					.create();
			when(currentUser.requireUser()).thenReturn(viewer);
			when(lessonApiMapper.toLessonListQuery(request)).thenReturn(query);
			when(lessonApiMapper.page(request)).thenReturn(0);
			when(lessonApiMapper.size(request)).thenReturn(20);
			when(lessonService.getAllLessons(viewer, query, 0, 20)).thenReturn(mock(Page.class));
			when(lessonApiMapper.toLessonsListResponseV1(any())).thenReturn(responseV1);
			when(learningEnrollmentService.getEnrolledLessonIds(1L, List.of(10L))).thenReturn(java.util.Set.of(10L));

			// When:
			ResponseEntity<LessonsListResponseV1> response = controller.searchLessons(request);

			// Then:
			assertThat(response.getBody()).isSameAs(responseV1);
		}
	}

	@Nested
	class GetLesson {

		@Test
		void shouldAssembleDetailResponseTest() {
			// Given:
			AppUser viewer = viewer();
			LessonDetailRecord lessonDetail = Instancio.create(LessonDetailRecord.class);
			List<LessonActivityRecord> activities = List.of();
			LessonDetailResponseV1 expected = Instancio.create(LessonDetailResponseV1.class);
			when(currentUser.requireUser()).thenReturn(viewer);
			when(lessonService.getLesson(viewer, 1L)).thenReturn(lessonDetail);
			when(lessonActivityService.getLessonActivities(viewer, 1L)).thenReturn(activities);
			when(learningEnrollmentService.findLessonEnrollment(viewer, 1L)).thenReturn(Optional.empty());
			when(lessonApiMapper.toLessonDetailResponseV1(any(LessonDetailResultRecord.class))).thenReturn(expected);

			// When:
			ResponseEntity<LessonDetailResponseV1> response = controller.getLesson(1L);

			// Then:
			assertThat(response.getBody()).isSameAs(expected);
		}
	}

	@Nested
	class CreateLesson {

		@Test
		void shouldReturnCreatedTest() {
			// Given:
			AppUser viewer = viewer();
			CreateLessonRequestV1 request = Instancio.create(CreateLessonRequestV1.class);
			CreateLessonInput input = Instancio.create(CreateLessonInput.class);
			LessonSummaryRecord created = Instancio.create(LessonSummaryRecord.class);
			LessonResponseV1 expected = Instancio.create(LessonResponseV1.class);
			when(currentUser.requireUser()).thenReturn(viewer);
			when(lessonApiMapper.toCreateLessonInput(request)).thenReturn(input);
			when(lessonService.createLesson(viewer, input)).thenReturn(created);
			when(lessonApiMapper.toLessonResponseV1(created)).thenReturn(expected);

			// When:
			ResponseEntity<LessonResponseV1> response = controller.createLesson(request);

			// Then:
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
			assertThat(response.getBody()).isSameAs(expected);
		}
	}

	@Nested
	class UpdateLessonContent {

		@Test
		void shouldDelegateAndReturnOkTest() {
			// Given:
			AppUser viewer = viewer();
			UpdateLessonContentRequestV1 request = Instancio.create(UpdateLessonContentRequestV1.class);
			UpdateLessonContentInput input = Instancio.create(UpdateLessonContentInput.class);
			LessonDetailRecord updated = Instancio.create(LessonDetailRecord.class);
			LessonResponseV1 expected = Instancio.create(LessonResponseV1.class);
			when(currentUser.requireUser()).thenReturn(viewer);
			when(lessonApiMapper.toUpdateLessonContentInput(request)).thenReturn(input);
			when(lessonService.updateLessonContent(viewer, 1L, input)).thenReturn(updated);
			when(lessonApiMapper.toLessonResponseV1(updated)).thenReturn(expected);

			// When:
			ResponseEntity<LessonResponseV1> response = controller.updateLessonContent(1L, request);

			// Then:
			assertThat(response.getBody()).isSameAs(expected);
		}
	}

	@Nested
	class ChangeLessonStatus {

		@Test
		void shouldMapActionAndDelegateTest() {
			// Given:
			AppUser viewer = viewer();
			LessonPublishActionV1 actionV1 = Instancio.create(LessonPublishActionV1.class);
			ChangeLessonStatusRequestV1 request = Instancio.of(ChangeLessonStatusRequestV1.class)
					.set(field("action"), actionV1)
					.create();
			LessonDetailRecord changed = Instancio.create(LessonDetailRecord.class);
			LessonResponseV1 expected = Instancio.create(LessonResponseV1.class);
			when(currentUser.requireUser()).thenReturn(viewer);
			when(lessonService.changeLessonStatus(eq(viewer), eq(1L), any(LessonStatusAction.class))).thenReturn(changed);
			when(lessonApiMapper.toLessonResponseV1(changed)).thenReturn(expected);

			// When:
			ResponseEntity<LessonResponseV1> response = controller.changeLessonStatus(1L, request);

			// Then:
			assertThat(response.getBody()).isSameAs(expected);
		}
	}

	@Nested
	class DeleteLesson {

		@Test
		void shouldReturnOkIdTest() {
			// Given:
			AppUser viewer = viewer();
			OkIdResponseV1 expected = Instancio.create(OkIdResponseV1.class);
			when(currentUser.requireUser()).thenReturn(viewer);
			when(apiResponses.okId(1L)).thenReturn(expected);

			// When:
			ResponseEntity<OkIdResponseV1> response = controller.deleteLesson(1L);

			// Then:
			assertThat(response.getBody()).isSameAs(expected);
		}
	}

	@Nested
	class ReviseLesson {

		@Test
		void shouldDelegateToRevisionServiceTest() {
			// Given:
			AppUser viewer = viewer();
			ReviseLessonRequestV1 request = Instancio.create(ReviseLessonRequestV1.class);
			ReviseLessonInput input = Instancio.create(ReviseLessonInput.class);
			RevisionResultRecord result = Instancio.create(RevisionResultRecord.class);
			LessonResponseV1 expected = Instancio.create(LessonResponseV1.class);
			when(currentUser.requireUser()).thenReturn(viewer);
			when(lessonApiMapper.toReviseLessonInput(request)).thenReturn(input);
			when(lessonRevisionService.reviseLesson(viewer, 1L, input)).thenReturn(result);
			when(lessonApiMapper.toLessonResponseV1(result)).thenReturn(expected);

			// When:
			ResponseEntity<LessonResponseV1> response = controller.reviseLesson(1L, request);

			// Then:
			assertThat(response.getBody()).isSameAs(expected);
		}
	}

	@Nested
	class GenerateActivity {

		@Test
		void shouldPassTypeAndCountTest() {
			// Given:
			AppUser viewer = viewer();
			GenerateActivityRequestV1 request = Instancio.of(GenerateActivityRequestV1.class)
					.set(field("type"), null)
					.set(field("count"), null)
					.create();
			GenerateActivityResultRecord result = Instancio.create(GenerateActivityResultRecord.class);
			ActivityResponseV1 expected = Instancio.create(ActivityResponseV1.class);
			when(currentUser.requireUser()).thenReturn(viewer);
			when(lessonActivityService.generateActivity(viewer, 1L, null, null)).thenReturn(result);
			when(lessonActivityApiMapper.toActivityResponseV1(result)).thenReturn(expected);

			// When:
			ResponseEntity<ActivityResponseV1> response = controller.generateActivity(1L, request);

			// Then:
			assertThat(response.getBody()).isSameAs(expected);
		}
	}

	@Nested
	class UpdateActivity {

		@Test
		void shouldDelegateToActivityServiceTest() {
			// Given:
			AppUser viewer = viewer();
			UpdateActivityRequestV1 request = Instancio.create(UpdateActivityRequestV1.class);
			UpdateActivityInput input = Instancio.create(UpdateActivityInput.class);
			UpdateActivityResultRecord result = Instancio.create(UpdateActivityResultRecord.class);
			ActivityResponseV1 expected = Instancio.create(ActivityResponseV1.class);
			when(currentUser.requireUser()).thenReturn(viewer);
			when(lessonActivityApiMapper.toUpdateActivityInput(request)).thenReturn(input);
			when(lessonActivityService.updateActivity(viewer, 1L, 2L, input)).thenReturn(result);
			when(lessonActivityApiMapper.toActivityResponseV1(result)).thenReturn(expected);

			// When:
			ResponseEntity<ActivityResponseV1> response = controller.updateActivity(1L, 2L, request);

			// Then:
			assertThat(response.getBody()).isSameAs(expected);
		}
	}

	@Nested
	class DeleteLessonActivity {

		@Test
		void shouldDelegateAndReturnActivitiesTest() {
			// Given:
			AppUser viewer = viewer();
			List<LessonActivityRecord> result = List.of();
			LessonActivitiesResponseV1 expected = Instancio.create(LessonActivitiesResponseV1.class);
			when(currentUser.requireUser()).thenReturn(viewer);
			when(lessonActivityService.deleteActivity(viewer, 1L, 2L)).thenReturn(result);
			when(lessonActivityApiMapper.toLessonActivitiesResponseV1(result)).thenReturn(expected);

			// When:
			ResponseEntity<LessonActivitiesResponseV1> response = controller.deleteLessonActivity(1L, 2L);

			// Then:
			assertThat(response.getBody()).isSameAs(expected);
		}
	}

	@Nested
	class SubmitActivityProgress {

		@Test
		void shouldDelegateAndReturnProgressTest() {
			// Given:
			AppUser viewer = viewer();
			SubmitActivityProgressRequestV1 request = Instancio.create(SubmitActivityProgressRequestV1.class);
			SubmitActivityProgressResultRecord result = Instancio.create(SubmitActivityProgressResultRecord.class);
			ActivityProgressResponseV1 expected = Instancio.create(ActivityProgressResponseV1.class);
			when(currentUser.requireUser()).thenReturn(viewer);
			when(lessonActivityService.submitActivityProgress(eq(viewer), eq(1L), eq(2L), any())).thenReturn(result);
			when(lessonActivityApiMapper.toActivityProgressResponseV1(result)).thenReturn(expected);

			// When:
			ResponseEntity<ActivityProgressResponseV1> response = controller.submitActivityProgress(1L, 2L, request);

			// Then:
			assertThat(response.getBody()).isSameAs(expected);
		}
	}

	@Nested
	class ResetActivityProgress {

		@Test
		void shouldDelegateAndReturnOkTest() {
			// Given:
			AppUser viewer = viewer();
			OkResponseV1 expected = Instancio.create(OkResponseV1.class);
			when(currentUser.requireUser()).thenReturn(viewer);
			when(apiResponses.ok()).thenReturn(expected);

			// When:
			ResponseEntity<OkResponseV1> response = controller.resetActivityProgress(1L, 2L);

			// Then:
			assertThat(response.getBody()).isSameAs(expected);
		}
	}

	@Nested
	class AskLessonAssistant {

		@Test
		void shouldDelegateWithMappedPresetTest() {
			// Given:
			AppUser viewer = viewer();
			AskLessonRequestV1 request = Instancio.of(AskLessonRequestV1.class)
					.set(field("question"), "What is this?")
					.create();
			AskLessonResultRecord result = Instancio.create(AskLessonResultRecord.class);
			AskLessonResponseV1 expected = Instancio.create(AskLessonResponseV1.class);
			when(currentUser.requireUser()).thenReturn(viewer);
			when(lessonAssistantPresetApiMapper.mapAssistantPreset(any())).thenReturn(LessonAssistantPreset.REGULAR);
			when(lessonAssistantService.ask(eq(viewer), eq(1L), eq("What is this?"), any(), any())).thenReturn(result);
			when(lessonApiMapper.toAskLessonResponseV1(result)).thenReturn(expected);

			// When:
			ResponseEntity<AskLessonResponseV1> response = controller.askLessonAssistant(1L, request);

			// Then:
			assertThat(response.getBody()).isSameAs(expected);
		}
	}

	@Nested
	class GetConversation {

		@Test
		void shouldReturnConversationTest() {
			// Given:
			AppUser viewer = viewer();
			LessonAssistantConversationRecord conversation = Instancio.create(LessonAssistantConversationRecord.class);
			LessonAssistantConversationResponseV1 expected =
					Instancio.create(LessonAssistantConversationResponseV1.class);
			when(currentUser.requireUser()).thenReturn(viewer);
			when(lessonAssistantService.getConversation(viewer, 1L)).thenReturn(conversation);
			when(lessonApiMapper.toLessonAssistantConversationResponseV1(conversation)).thenReturn(expected);

			// When:
			ResponseEntity<LessonAssistantConversationResponseV1> response =
					controller.getLessonAssistantConversation(1L);

			// Then:
			assertThat(response.getBody()).isSameAs(expected);
		}
	}

	@Nested
	class ClearConversation {

		@Test
		void shouldDelegateAndReturnOkTest() {
			// Given:
			AppUser viewer = viewer();
			OkResponseV1 expected = Instancio.create(OkResponseV1.class);
			when(currentUser.requireUser()).thenReturn(viewer);
			when(apiResponses.ok()).thenReturn(expected);

			// When:
			ResponseEntity<OkResponseV1> response = controller.clearLessonAssistantConversation(1L);

			// Then:
			assertThat(response.getBody()).isSameAs(expected);
		}
	}

	@Nested
	class AddLessonAsset {

		@Test
		void shouldReturnCreatedWithAssetTest() {
			// Given:
			AppUser viewer = viewer();
			AddLessonAssetRequestV1 request = Instancio.create(AddLessonAssetRequestV1.class);
			CreateLessonAssetInput input = Instancio.create(CreateLessonAssetInput.class);
			LessonAssetResultRecord result = Instancio.create(LessonAssetResultRecord.class);
			LessonAssetResponseV1 expected = Instancio.create(LessonAssetResponseV1.class);
			when(currentUser.requireUser()).thenReturn(viewer);
			when(lessonApiMapper.toCreateLessonAssetInput(request)).thenReturn(input);
			when(lessonService.createAsset(viewer, 1L, input)).thenReturn(result);
			when(lessonApiMapper.toLessonAssetResponseV1(result)).thenReturn(expected);

			// When:
			ResponseEntity<LessonAssetResponseV1> response = controller.addLessonAsset(1L, request);

			// Then:
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		}
	}

	@Nested
	class DeleteLessonAsset {

		@Test
		void shouldDelegateAndReturnLessonTest() {
			// Given:
			AppUser viewer = viewer();
			LessonAssetDeleteResultRecord result = Instancio.create(LessonAssetDeleteResultRecord.class);
			LessonResponseV1 expected = Instancio.create(LessonResponseV1.class);
			when(currentUser.requireUser()).thenReturn(viewer);
			when(lessonService.deleteAsset(viewer, 1L, 2L)).thenReturn(result);
			when(lessonApiMapper.toLessonResponseV1(result)).thenReturn(expected);

			// When:
			ResponseEntity<LessonResponseV1> response = controller.deleteLessonAsset(1L, 2L);

			// Then:
			assertThat(response.getBody()).isSameAs(expected);
		}
	}

	@Nested
	class AssignLesson {

		@Test
		void shouldDelegateAndReturnAssignmentTest() {
			// Given:
			AppUser viewer = viewer();
			AssignmentRequestV1 request = Instancio.of(AssignmentRequestV1.class)
					.set(field("userIds"), List.of(10L, 20L))
					.create();
			LessonAssignmentResultRecord result = Instancio.create(LessonAssignmentResultRecord.class);
			AssignmentResponseV1 expected = Instancio.create(AssignmentResponseV1.class);
			when(currentUser.requireUser()).thenReturn(viewer);
			when(learningService.assignLesson(viewer, 1L, List.of(10L, 20L))).thenReturn(result);
			when(learningApiMapper.toLessonAssignmentResponseV1(result)).thenReturn(expected);

			// When:
			ResponseEntity<AssignmentResponseV1> response = controller.assignLesson(1L, request);

			// Then:
			assertThat(response.getBody()).isSameAs(expected);
		}
	}

	@Nested
	class ListLessonAssignees {

		@Test
		void shouldDelegateAndReturnAssigneesTest() {
			// Given:
			AppUser viewer = viewer();
			List<LearningAssigneeRecord> result = List.of();
			LearningAssigneesResponseV1 expected = Instancio.create(LearningAssigneesResponseV1.class);
			when(currentUser.requireUser()).thenReturn(viewer);
			when(learningService.listLessonAssignees(viewer, 1L)).thenReturn(result);
			when(learningApiMapper.toLearningAssigneesResponseV1(result)).thenReturn(expected);

			// When:
			ResponseEntity<LearningAssigneesResponseV1> response = controller.listLessonAssignees(1L);

			// Then:
			assertThat(response.getBody()).isSameAs(expected);
		}
	}

	@Nested
	class RevokeLessonAssignments {

		@Test
		void shouldDelegateAndReturnOkTest() {
			// Given:
			AppUser viewer = viewer();
			AssignmentRequestV1 request = Instancio.of(AssignmentRequestV1.class)
					.set(field("userIds"), List.of(10L))
					.create();
			OkResponseV1 expected = Instancio.create(OkResponseV1.class);
			when(currentUser.requireUser()).thenReturn(viewer);
			when(apiResponses.ok()).thenReturn(expected);

			// When:
			ResponseEntity<OkResponseV1> response = controller.revokeLessonAssignments(1L, request);

			// Then:
			assertThat(response.getBody()).isSameAs(expected);
		}
	}

	@Nested
	class EnrollLesson {

		@Test
		void shouldDelegateAndReturnEnrollmentTest() {
			// Given:
			AppUser viewer = viewer();
			LessonEnrollmentResultRecord result = Instancio.create(LessonEnrollmentResultRecord.class);
			EnrollmentResponseV1 expected = Instancio.create(EnrollmentResponseV1.class);
			when(currentUser.requireUser()).thenReturn(viewer);
			when(learningService.enrollLesson(viewer, 1L)).thenReturn(result);
			when(learningApiMapper.toLessonEnrollmentResponseV1(result)).thenReturn(expected);

			// When:
			ResponseEntity<EnrollmentResponseV1> response = controller.enrollLesson(1L);

			// Then:
			assertThat(response.getBody()).isSameAs(expected);
		}
	}

	@Nested
	class UnenrollLesson {

		@Test
		void shouldDelegateAndReturnOkTest() {
			// Given:
			AppUser viewer = viewer();
			OkResponseV1 expected = Instancio.create(OkResponseV1.class);
			when(currentUser.requireUser()).thenReturn(viewer);
			when(apiResponses.ok()).thenReturn(expected);

			// When:
			ResponseEntity<OkResponseV1> response = controller.unenrollLesson(1L);

			// Then:
			assertThat(response.getBody()).isSameAs(expected);
		}
	}

	@Nested
	class SetLessonCompletion {

		@Test
		void shouldDelegateWithCompletedFlagTest() {
			// Given:
			AppUser viewer = viewer();
			SetLessonCompletionRequestV1 request = Instancio.of(SetLessonCompletionRequestV1.class)
					.set(field("completed"), true)
					.create();
			LessonEnrollmentResultRecord result = Instancio.create(LessonEnrollmentResultRecord.class);
			EnrollmentResponseV1 expected = Instancio.create(EnrollmentResponseV1.class);
			when(currentUser.requireUser()).thenReturn(viewer);
			when(learningService.setLessonCompletion(viewer, 1L, true)).thenReturn(result);
			when(learningApiMapper.toLessonEnrollmentResponseV1(result)).thenReturn(expected);

			// When:
			ResponseEntity<EnrollmentResponseV1> response = controller.setLessonCompletion(1L, request);

			// Then:
			assertThat(response.getBody()).isSameAs(expected);
		}
	}

	@Nested
	class TeacherVideo {

		@Test
		void shouldCreateTeacherVideoAndReturnCreatedTest() {
			// Given:
			AppUser viewer = viewer();
			TeacherVideoResultRecord result = Instancio.create(TeacherVideoResultRecord.class);
			TeacherVideoResponseV1 expected = Instancio.create(TeacherVideoResponseV1.class);
			when(currentUser.requireUser()).thenReturn(viewer);
			when(teacherVideoService.create(viewer, 1L)).thenReturn(result);
			when(lessonApiMapper.toTeacherVideoResponseV1(result)).thenReturn(expected);

			// When:
			ResponseEntity<TeacherVideoResponseV1> response = controller.createTeacherVideo(1L, null);

			// Then:
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		}

		@Test
		void shouldGetTeacherVideoStatusTest() {
			// Given:
			AppUser viewer = viewer();
			TeacherVideoResultRecord result = Instancio.create(TeacherVideoResultRecord.class);
			TeacherVideoResponseV1 expected = Instancio.create(TeacherVideoResponseV1.class);
			when(currentUser.requireUser()).thenReturn(viewer);
			when(teacherVideoService.getStatus(viewer, 1L)).thenReturn(result);
			when(lessonApiMapper.toTeacherVideoResponseV1(result)).thenReturn(expected);

			// When:
			ResponseEntity<TeacherVideoResponseV1> response = controller.getTeacherVideo(1L);

			// Then:
			assertThat(response.getBody()).isSameAs(expected);
		}

		@Test
		void shouldDeleteTeacherVideoAndReturnOkTest() {
			// Given:
			AppUser viewer = viewer();
			OkResponseV1 expected = Instancio.create(OkResponseV1.class);
			when(currentUser.requireUser()).thenReturn(viewer);
			when(apiResponses.ok()).thenReturn(expected);

			// When:
			ResponseEntity<OkResponseV1> response = controller.deleteTeacherVideo(1L);

			// Then:
			assertThat(response.getBody()).isSameAs(expected);
		}
	}

	@Nested
	class ToChatHistory {

		@Test
		void shouldReturnEmptyForNullTest() {
			// When:
			List<ChatTurn> result = controller.toChatHistory(null);

			// Then:
			assertThat(result).isEmpty();
		}

		@Test
		void shouldMapMessagesTest() {
			// Given:
			ChatMessageV1 msg = Instancio.of(ChatMessageV1.class)
					.set(field("content"), "Hello")
					.create();

			// When:
			List<ChatTurn> result = controller.toChatHistory(List.of(msg));

			// Then:
			assertThat(result).hasSize(1);
			assertThat(result.get(0).content()).isEqualTo("Hello");
		}

		@Test
		void shouldSkipNullMessagesTest() {
			// When:
			List<ChatTurn> result = controller.toChatHistory(java.util.Arrays.asList(null, null));

			// Then:
			assertThat(result).isEmpty();
		}
	}

	AppUser viewer() {
		return Instancio.of(AppUser.class)
				.set(field("internalId"), 1L)
				.set(field("clerkUserId"), "clerk-1")
				.set(field("email"), "v@t.com")
				.set(field("fullName"), "V")
				.set(field("roleCode"), "member")
				.set(field("name"), "V")
				.set(field("position"), null)
				.set(field("avatarStorageKey"), null)
				.set(field("avatarColor"), null)
				.create();
	}
}
