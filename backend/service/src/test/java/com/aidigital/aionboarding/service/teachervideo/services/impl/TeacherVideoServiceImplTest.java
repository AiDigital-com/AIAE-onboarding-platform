package com.aidigital.aionboarding.service.teachervideo.services.impl;

import com.aidigital.aionboarding.domain.common.dictionary.LessonStatusCode;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonStatus;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.lesson.models.LessonDetailRecord;
import com.aidigital.aionboarding.service.lesson.models.TeacherVideoDeleteResultRecord;
import com.aidigital.aionboarding.service.lesson.models.TeacherVideoRecord;
import com.aidigital.aionboarding.service.lesson.models.TeacherVideoResultRecord;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonEntityService;
import com.aidigital.aionboarding.service.lesson.support.LessonRecordAssembler;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import com.aidigital.aionboarding.service.teachervideo.services.TeacherVideoRefreshService;
import com.aidigital.aionboarding.service.teachervideo.support.TeacherVideoCreationWorkflow;
import com.aidigital.aionboarding.service.teachervideo.support.TeacherVideoMetadataSupport;
import org.instancio.Instancio;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherVideoServiceImplTest {

	@Mock
	private LessonEntityService lessonEntityService;
	@Mock
	private PermissionService permissionService;
	@Mock
	private LessonRecordAssembler lessonMapper;
	@Mock
	private TeacherVideoRefreshService teacherVideoRefreshService;
	@Mock
	private TeacherVideoMetadataSupport teacherVideoMetadataSupport;
	@Mock
	private TeacherVideoCreationWorkflow teacherVideoCreationWorkflow;
	@Mock
	private CurrentTime currentTime;

	@InjectMocks
	private TeacherVideoServiceImpl service;

	@Nested
	class Create {

		@Test
		void shouldReturnTeacherVideoResultOnHappyPathTest() {
			// Given:
			AppUser admin = new AppUser(1L, "clerk-1", "admin@test.com", "Admin", "admin", "Admin", null, null, null);
			Long lessonId = 30L;
			Lesson lesson = Instancio.of(Lesson.class).set(field(Lesson::getId), lessonId).create();
			TeacherVideoResultRecord expected = mock(TeacherVideoResultRecord.class);

			TeacherVideoServiceImpl spyService = spy(service);
			doReturn(lesson).when(spyService).requireReadyLessonWithContent(admin, lessonId);
			when(teacherVideoCreationWorkflow.create(lesson, lessonId)).thenReturn(expected);

			// When:
			TeacherVideoResultRecord result = spyService.create(admin, lessonId);

			// Then:
			assertThat(result).isSameAs(expected);
			verify(permissionService).requirePermission(admin, PermissionKeys.LESSONS_MANAGE);
			verify(teacherVideoCreationWorkflow).create(lesson, lessonId);
		}

		@Test
		void shouldPropagateExceptionWhenPermissionDeniedTest() {
			// Given:
			AppUser viewer = new AppUser(2L, "clerk-2", "viewer@test.com", "Viewer", "member", "Viewer", null, null, null);
			Long lessonId = 31L;
			doThrow(new AppException(ErrorReason.C004)).when(permissionService)
					.requirePermission(viewer, PermissionKeys.LESSONS_MANAGE);

			// When-Then:
			AppException thrown = assertThrows(AppException.class, () -> service.create(viewer, lessonId));
			assertThat(thrown.getCode()).isEqualTo(ErrorReason.C004.name());
			verifyNoInteractions(lessonEntityService);
		}

		@Test
		void shouldThrowWhenViewerIsNotAdminTest() {
			// Given:
			AppUser member = new AppUser(3L, "clerk-3", "member@test.com", "Member", "member", "Member", null, null, null);
			Long lessonId = 32L;

			// When-Then:
			AppException thrown = assertThrows(AppException.class, () -> service.create(member, lessonId));
			assertThat(thrown.getCode()).isEqualTo(ErrorReason.C004.name());
			verify(permissionService).requirePermission(member, PermissionKeys.LESSONS_MANAGE);
			verifyNoInteractions(lessonEntityService);
		}

		@Test
		void shouldPropagateExceptionFromRequireReadyLessonWithContentTest() {
			// Given:
			AppUser admin = new AppUser(4L, "clerk-4", "admin@test.com", "Admin", "admin", "Admin", null, null, null);
			Long lessonId = 33L;
			TeacherVideoServiceImpl spyService = spy(service);
			doThrow(new AppException(ErrorReason.C002, "Only ready lessons can have teacher videos."))
					.when(spyService).requireReadyLessonWithContent(admin, lessonId);

			// When-Then:
			AppException thrown = assertThrows(AppException.class, () -> spyService.create(admin, lessonId));
			assertThat(thrown.getCode()).isEqualTo(ErrorReason.C002.name());
			verify(spyService).requireReadyLessonWithContent(admin, lessonId);
			// The creation workflow never ran, so none of its steps were touched.
			verifyNoInteractions(teacherVideoCreationWorkflow);
		}
	}

	// -------------------------------------------------------------------------
	// getStatus()
	// -------------------------------------------------------------------------

	@Test
	void getStatusShouldReturnRefreshedTeacherVideoWhenPresentTest() {
		// Given:
		AppUser admin = new AppUser(1L, "clerk-1", "admin@test.com", "Admin", "admin", "Admin", null, null, null);
		Long lessonId = 40L;
		Map<String, Object> teacherVideoMap = Map.of("videoId", "video-1", "status", "completed");
		Lesson lesson = Instancio.of(Lesson.class)
				.set(field(Lesson::getId), lessonId)
				.set(field(Lesson::getGenerationMetadata), Map.of("teacherVideo", teacherVideoMap))
				.create();
		TeacherVideoRecord teacherVideo = mock(TeacherVideoRecord.class);
		when(teacherVideo.videoId()).thenReturn("video-1");
		when(lessonMapper.toTeacherVideoRecord(teacherVideoMap)).thenReturn(teacherVideo);
		when(lessonEntityService.getReference(lessonId)).thenReturn(lesson);

		TeacherVideoRefreshService.RefreshResult refreshResult =
				new TeacherVideoRefreshService.RefreshResult(lesson, teacherVideo);
		when(teacherVideoRefreshService.refreshTeacherVideoIfNeeded(lesson, teacherVideo, true))
				.thenReturn(refreshResult);
		when(currentTime.instantString()).thenReturn("2026-07-15T11:00:00Z");
		TeacherVideoRecord normalized = mock(TeacherVideoRecord.class);
		when(lessonMapper.normalizeTeacherVideoRecord(teacherVideo, "2026-07-15T11:00:00Z")).thenReturn(normalized);
		LessonDetailRecord detailRecord = mock(LessonDetailRecord.class);
		when(lessonMapper.toDetailRecord(lesson)).thenReturn(detailRecord);

		// When:
		TeacherVideoResultRecord result = service.getStatus(admin, lessonId);

		// Then:
		assertThat(result.teacherVideo()).isSameAs(normalized);
		assertThat(result.lesson()).isSameAs(detailRecord);
		verify(permissionService).requirePermission(admin, PermissionKeys.LESSONS_MANAGE);
	}

	@Test
	void getStatusShouldPropagateExceptionWhenPermissionDeniedTest() {
		// Given:
		AppUser viewer = new AppUser(2L, "clerk-2", "viewer@test.com", "Viewer", "member", "Viewer", null, null, null);
		Long lessonId = 41L;
		doThrow(new AppException(ErrorReason.C004)).when(permissionService)
				.requirePermission(viewer, PermissionKeys.LESSONS_MANAGE);

		// When-Then:
		AppException thrown = assertThrows(AppException.class, () -> service.getStatus(viewer, lessonId));
		assertThat(thrown.getCode()).isEqualTo(ErrorReason.C004.name());
		verifyNoInteractions(lessonEntityService);
	}

	@Test
	void getStatusShouldThrowWhenViewerIsNotAdminTest() {
		// Given:
		AppUser member = new AppUser(3L, "clerk-3", "member@test.com", "Member", "member", "Member", null, null, null);
		Long lessonId = 42L;

		// When-Then:
		AppException thrown = assertThrows(AppException.class, () -> service.getStatus(member, lessonId));
		assertThat(thrown.getCode()).isEqualTo(ErrorReason.C004.name());
		verifyNoInteractions(lessonEntityService);
	}

	@Test
	void getStatusShouldThrowWhenNoTeacherVideoHasBeenRequestedTest() {
		// Given:
		AppUser admin = new AppUser(1L, "clerk-1", "admin@test.com", "Admin", "admin", "Admin", null, null, null);
		Long lessonId = 43L;
		Lesson lesson = Instancio.of(Lesson.class)
				.set(field(Lesson::getId), lessonId)
				.set(field(Lesson::getGenerationMetadata), (Map<String, Object>) null)
				.create();
		when(lessonEntityService.getReference(lessonId)).thenReturn(lesson);
		when(lessonMapper.toTeacherVideoRecord(Map.of())).thenReturn(null);

		// When-Then:
		AppException thrown = assertThrows(AppException.class, () -> service.getStatus(admin, lessonId));
		assertThat(thrown.getCode()).isEqualTo(ErrorReason.C001.name());
		verifyNoInteractions(teacherVideoRefreshService);
	}

	@Test
	void getStatusShouldThrowWhenTeacherVideoIdIsBlankTest() {
		// Given:
		AppUser admin = new AppUser(1L, "clerk-1", "admin@test.com", "Admin", "admin", "Admin", null, null, null);
		Long lessonId = 44L;
		Map<String, Object> teacherVideoMap = Map.of("status", "pending");
		Lesson lesson = Instancio.of(Lesson.class)
				.set(field(Lesson::getId), lessonId)
				.set(field(Lesson::getGenerationMetadata), Map.of("teacherVideo", teacherVideoMap))
				.create();
		when(lessonEntityService.getReference(lessonId)).thenReturn(lesson);
		TeacherVideoRecord teacherVideo = mock(TeacherVideoRecord.class);
		when(teacherVideo.videoId()).thenReturn("");
		when(lessonMapper.toTeacherVideoRecord(teacherVideoMap)).thenReturn(teacherVideo);

		// When-Then:
		AppException thrown = assertThrows(AppException.class, () -> service.getStatus(admin, lessonId));
		assertThat(thrown.getCode()).isEqualTo(ErrorReason.C001.name());
		verifyNoInteractions(teacherVideoRefreshService);
	}

	// -------------------------------------------------------------------------
	// delete()
	// -------------------------------------------------------------------------

	@Test
	void deleteShouldRemoveTeacherVideoMetadataAndReturnUpdatedLessonTest() {
		// Given:
		AppUser admin = new AppUser(1L, "clerk-1", "admin@test.com", "Admin", "admin", "Admin", null, null, null);
		Long lessonId = 50L;
		Map<String, Object> existingMetadata = new HashMap<>(Map.of(
				"teacherVideo", Map.of("videoId", "video-1"),
				"preparedMaterials", Map.of("materials", List.of())
		));
		Lesson lesson = Instancio.of(Lesson.class)
				.set(field(Lesson::getId), lessonId)
				.set(field(Lesson::getGenerationMetadata), existingMetadata)
				.create();
		when(lessonEntityService.getReference(lessonId)).thenReturn(lesson);
		Map<String, Object> mutableCopy = new HashMap<>(existingMetadata);
		when(teacherVideoMetadataSupport.mutableMetadata(lesson)).thenReturn(mutableCopy);
		LocalDateTime updatedAt = LocalDateTime.parse("2026-07-15T12:00:00");
		when(currentTime.utcDateTime()).thenReturn(updatedAt);
		Lesson savedLesson = Instancio.of(Lesson.class).set(field(Lesson::getId), lessonId).create();
		when(lessonEntityService.save(lesson)).thenReturn(savedLesson);
		LessonDetailRecord detailRecord = mock(LessonDetailRecord.class);
		when(lessonMapper.toDetailRecord(savedLesson)).thenReturn(detailRecord);

		// When:
		TeacherVideoDeleteResultRecord result = service.delete(admin, lessonId);

		// Then:
		assertThat(result.lesson()).isSameAs(detailRecord);
		assertThat(mutableCopy).doesNotContainKey("teacherVideo");
		assertThat(mutableCopy).containsKey("preparedMaterials");
		assertThat(lesson.getGenerationMetadata()).isSameAs(mutableCopy);
		assertThat(lesson.getUpdatedAt()).isEqualTo(updatedAt);
		verify(permissionService).requirePermission(admin, PermissionKeys.LESSONS_MANAGE);
	}

	@Test
	void deleteShouldPropagateExceptionWhenPermissionDeniedTest() {
		// Given:
		AppUser viewer = new AppUser(2L, "clerk-2", "viewer@test.com", "Viewer", "member", "Viewer", null, null, null);
		Long lessonId = 51L;
		doThrow(new AppException(ErrorReason.C004)).when(permissionService)
				.requirePermission(viewer, PermissionKeys.LESSONS_MANAGE);

		// When-Then:
		AppException thrown = assertThrows(AppException.class, () -> service.delete(viewer, lessonId));
		assertThat(thrown.getCode()).isEqualTo(ErrorReason.C004.name());
		verifyNoInteractions(lessonEntityService);
	}

	@Test
	void deleteShouldThrowWhenViewerIsNotAdminTest() {
		// Given:
		AppUser member = new AppUser(3L, "clerk-3", "member@test.com", "Member", "member", "Member", null, null, null);
		Long lessonId = 52L;

		// When-Then:
		AppException thrown = assertThrows(AppException.class, () -> service.delete(member, lessonId));
		assertThat(thrown.getCode()).isEqualTo(ErrorReason.C004.name());
		verifyNoInteractions(lessonEntityService);
	}

	// -------------------------------------------------------------------------
	// requireReadyLessonWithContent()
	// -------------------------------------------------------------------------

	@Test
	void requireReadyLessonWithContentShouldReturnLessonWhenReadyManageableAndHasContentTest() {
		// Given:
		AppUser viewer = new AppUser(1L, "clerk-1", "viewer@test.com", "Viewer", "member", "Viewer", null, null, null);
		Long lessonId = 20L;
		User owner = new User();
		owner.setId(5L);
		LessonStatus readyStatus = new LessonStatus();
		readyStatus.setCode(LessonStatusCode.READY);
		Lesson lesson = Instancio.of(Lesson.class)
				.set(field(Lesson::getId), lessonId)
				.set(field(Lesson::getStatus), readyStatus)
				.set(field(Lesson::getContentHtml), "<p>content</p>")
				.set(field(Lesson::getContentMarkdown), "")
				.set(field(Lesson::getCreatedByUser), owner)
				.create();
		when(lessonEntityService.getReference(lessonId)).thenReturn(lesson);
		when(permissionService.canManageExistingLesson(viewer, 5L)).thenReturn(true);

		// When:
		Lesson result = service.requireReadyLessonWithContent(viewer, lessonId);

		// Then:
		assertThat(result).isSameAs(lesson);
	}

	@Test
	void requireReadyLessonWithContentShouldThrowWhenViewerCannotManageLessonTest() {
		// Given:
		AppUser viewer = new AppUser(1L, "clerk-1", "viewer@test.com", "Viewer", "member", "Viewer", null, null, null);
		Long lessonId = 21L;
		User owner = new User();
		owner.setId(9L);
		Lesson lesson = Instancio.of(Lesson.class)
				.set(field(Lesson::getId), lessonId)
				.set(field(Lesson::getCreatedByUser), owner)
				.create();
		when(lessonEntityService.getReference(lessonId)).thenReturn(lesson);
		when(permissionService.canManageExistingLesson(viewer, 9L)).thenReturn(false);

		// When-Then:
		AppException thrown = assertThrows(AppException.class,
				() -> service.requireReadyLessonWithContent(viewer, lessonId));
		assertThat(thrown.getCode()).isEqualTo(ErrorReason.C004.name());
	}

	@Test
	void requireReadyLessonWithContentShouldThrowWhenLessonIsNotReadyTest() {
		// Given:
		AppUser viewer = new AppUser(1L, "clerk-1", "viewer@test.com", "Viewer", "member", "Viewer", null, null, null);
		Long lessonId = 22L;
		LessonStatus draftStatus = new LessonStatus();
		draftStatus.setCode(LessonStatusCode.DRAFT);
		Lesson lesson = Instancio.of(Lesson.class)
				.set(field(Lesson::getId), lessonId)
				.set(field(Lesson::getStatus), draftStatus)
				.set(field(Lesson::getCreatedByUser), (User) null)
				.create();
		when(lessonEntityService.getReference(lessonId)).thenReturn(lesson);
		when(permissionService.canManageExistingLesson(viewer, null)).thenReturn(true);

		// When-Then:
		AppException thrown = assertThrows(AppException.class,
				() -> service.requireReadyLessonWithContent(viewer, lessonId));
		assertThat(thrown.getCode()).isEqualTo(ErrorReason.C002.name());
	}

	@Test
	void requireReadyLessonWithContentShouldThrowWhenContentIsEmptyTest() {
		// Given:
		AppUser viewer = new AppUser(1L, "clerk-1", "viewer@test.com", "Viewer", "member", "Viewer", null, null, null);
		Long lessonId = 23L;
		LessonStatus readyStatus = new LessonStatus();
		readyStatus.setCode(LessonStatusCode.READY);
		Lesson lesson = Instancio.of(Lesson.class)
				.set(field(Lesson::getId), lessonId)
				.set(field(Lesson::getStatus), readyStatus)
				.set(field(Lesson::getContentHtml), "")
				.set(field(Lesson::getContentMarkdown), "   ")
				.set(field(Lesson::getCreatedByUser), (User) null)
				.create();
		when(lessonEntityService.getReference(lessonId)).thenReturn(lesson);
		when(permissionService.canManageExistingLesson(viewer, null)).thenReturn(true);

		// When-Then:
		AppException thrown = assertThrows(AppException.class,
				() -> service.requireReadyLessonWithContent(viewer, lessonId));
		assertThat(thrown.getCode()).isEqualTo(ErrorReason.C002.name());
	}

	// -------------------------------------------------------------------------
	// requireAdmin()
	// -------------------------------------------------------------------------

	@Test
	void requireAdminShouldNotThrowWhenViewerIsAdminTest() {
		// Given:
		AppUser admin = new AppUser(1L, "clerk-1", "admin@test.com", "Admin", "admin", "Admin", null, null, null);

		// When-Then:
		assertDoesNotThrow(() -> service.requireAdmin(admin));
	}

	@Test
	void requireAdminShouldThrowWhenViewerIsNotAdminTest() {
		// Given:
		AppUser member = new AppUser(2L, "clerk-2", "member@test.com", "Member", "member", "Member", null, null, null);

		// When-Then:
		AppException thrown = assertThrows(AppException.class, () -> service.requireAdmin(member));
		assertThat(thrown.getCode()).isEqualTo(ErrorReason.C004.name());
	}

	// -------------------------------------------------------------------------
	// castMap()
	// -------------------------------------------------------------------------

	@Test
	void castMapShouldReturnTheSameMapWhenValueIsAMapTest() {
		// Given:
		Map<String, Object> source = Map.of("videoId", "video-1");

		// When:
		Map<String, Object> result = service.castMap(source);

		// Then:
		assertThat(result).isEqualTo(source);
	}

	@Test
	void castMapShouldReturnEmptyMapWhenValueIsNotAMapTest() {
		// Given:
		Object value = "not-a-map";

		// When:
		Map<String, Object> result = service.castMap(value);

		// Then:
		assertThat(result).isEmpty();
	}

	@Test
	void castMapShouldReturnEmptyMapWhenValueIsNullTest() {
		// Given:
		Object value = null;

		// When:
		Map<String, Object> result = service.castMap(value);

		// Then:
		assertThat(result).isEmpty();
	}
}
