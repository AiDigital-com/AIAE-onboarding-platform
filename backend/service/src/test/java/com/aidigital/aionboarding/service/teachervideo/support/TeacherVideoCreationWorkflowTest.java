package com.aidigital.aionboarding.service.teachervideo.support;

import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.external.heygen.HeyGenClient;
import com.aidigital.aionboarding.external.heygen.HeyGenExternalException;
import com.aidigital.aionboarding.external.heygen.model.HeyGenTeacherVideoResult;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.lesson.models.LessonDetailRecord;
import com.aidigital.aionboarding.service.lesson.models.TeacherVideoRecord;
import com.aidigital.aionboarding.service.lesson.models.TeacherVideoResultRecord;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonEntityService;
import com.aidigital.aionboarding.service.lesson.support.LessonRecordAssembler;
import com.aidigital.aionboarding.service.material.models.PreparedMaterialsResult;
import com.aidigital.aionboarding.service.material.services.MaterialPreparationService;
import com.aidigital.aionboarding.service.teachervideo.prompt.TeacherVideoPromptBuilder;
import org.instancio.Instancio;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherVideoCreationWorkflowTest {

	@Mock
	private LessonEntityService lessonEntityService;
	@Mock
	private MaterialPreparationService materialPreparationService;
	@Mock
	private LessonRecordAssembler lessonMapper;
	@Mock
	private HeyGenClient heyGenClient;
	@Mock
	private TeacherVideoPromptBuilder teacherVideoPromptBuilder;
	@Mock
	private TeacherVideoMetadataSupport teacherVideoMetadataSupport;
	@Mock
	private CurrentTime currentTime;

	@InjectMocks
	private TeacherVideoCreationWorkflow workflow;

	@Nested
	class Create {

		@Test
		void shouldThrowWhenActiveGenerationAlreadyInProgressTest() {
			// Given:
			Long lessonId = 34L;
			Lesson lesson = Instancio.of(Lesson.class).set(field(Lesson::getId), lessonId).create();
			Map<String, Object> activeTeacherVideoMap = Map.of("videoId", "video-1", "status", "processing");
			Map<String, Object> metadata = new HashMap<>(Map.of("teacherVideo", activeTeacherVideoMap));
			TeacherVideoRecord activeRecord = mock(TeacherVideoRecord.class);

			when(teacherVideoMetadataSupport.mutableMetadata(lesson)).thenReturn(metadata);
			when(lessonMapper.toTeacherVideoRecord(activeTeacherVideoMap)).thenReturn(activeRecord);
			when(teacherVideoMetadataSupport.hasActiveTeacherVideo(activeRecord)).thenReturn(true);

			// When-Then:
			AppException thrown = assertThrows(AppException.class, () -> workflow.create(lesson, lessonId));
			assertThat(thrown.getCode()).isEqualTo(ErrorReason.C006.name());
			verifyNoInteractions(materialPreparationService);
			verifyNoInteractions(heyGenClient);
		}
	}

	@Nested
	class RequestTeacherVideo {

		@Test
		void shouldPersistMetadataAndReturnResultOnSuccessTest() {
			// Given:
			Lesson lesson = Instancio.of(Lesson.class).set(field(Lesson::getId), 7L).create();
			Map<String, Object> metadata = new HashMap<>();
			Map<String, Object> lessonMap = new HashMap<>();
			when(teacherVideoPromptBuilder.buildTeacherVideoPrompt(lessonMap)).thenReturn("generated prompt");
			HeyGenTeacherVideoResult heyGenResult = new HeyGenTeacherVideoResult(
					"heygen", "generated prompt", "avatar-1", "voice-1", "session-1", "video-1", "pending");
			when(heyGenClient.createTeacherVideo("generated prompt")).thenReturn(heyGenResult);
			when(currentTime.instantString()).thenReturn("2026-07-15T10:00:00Z");
			when(teacherVideoPromptBuilder.durationLimitSeconds()).thenReturn(60);
			Map<String, Object> teacherVideoMap = Map.of("videoId", "video-1");
			when(lessonMapper.toTeacherVideoMap(any(TeacherVideoRecord.class))).thenReturn(teacherVideoMap);
			LocalDateTime updatedAt = LocalDateTime.parse("2026-07-15T10:05:00");
			when(currentTime.utcDateTime()).thenReturn(updatedAt);
			Lesson savedLesson = Instancio.of(Lesson.class).set(field(Lesson::getId), 7L).create();
			when(lessonEntityService.save(lesson)).thenReturn(savedLesson);
			TeacherVideoRecord normalized = mock(TeacherVideoRecord.class);
			when(lessonMapper.normalizeTeacherVideoRecord(any(TeacherVideoRecord.class), eq("2026-07-15T10:00:00Z")))
					.thenReturn(normalized);
			LessonDetailRecord detailRecord = mock(LessonDetailRecord.class);
			when(lessonMapper.toDetailRecord(savedLesson)).thenReturn(detailRecord);

			// When:
			TeacherVideoResultRecord result = workflow.requestTeacherVideo(lesson, metadata, lessonMap);

			// Then:
			ArgumentCaptor<TeacherVideoRecord> captor = ArgumentCaptor.forClass(TeacherVideoRecord.class);
			verify(lessonMapper).toTeacherVideoMap(captor.capture());
			TeacherVideoRecord captured = captor.getValue();
			assertThat(captured.provider()).isEqualTo("heygen");
			assertThat(captured.prompt()).isEqualTo("generated prompt");
			assertThat(captured.avatarId()).isEqualTo("avatar-1");
			assertThat(captured.voiceId()).isEqualTo("voice-1");
			assertThat(captured.sessionId()).isEqualTo("session-1");
			assertThat(captured.videoId()).isEqualTo("video-1");
			assertThat(captured.status()).isEqualTo("pending");
			assertThat(captured.durationLimitSeconds()).isEqualTo(60);
			assertThat(captured.checkedAt()).isEqualTo("2026-07-15T10:00:00Z");
			assertThat(captured.videoUrl()).isEmpty();
			assertThat(captured.thumbnailUrl()).isEmpty();
			assertThat(captured.duration()).isNull();
			assertThat(captured.completedAt()).isNull();
			assertThat(captured.failedAt()).isNull();
			assertThat(metadata).containsEntry("teacherVideo", teacherVideoMap);
			assertThat(lesson.getGenerationMetadata()).isSameAs(metadata);
			assertThat(lesson.getUpdatedAt()).isEqualTo(updatedAt);
			assertThat(result.teacherVideo()).isSameAs(normalized);
			assertThat(result.lesson()).isSameAs(detailRecord);
		}

		@Test
		void shouldThrowAppExceptionWhenHeyGenClientFailsTest() {
			// Given:
			Lesson lesson = Instancio.of(Lesson.class).set(field(Lesson::getId), 8L).create();
			Map<String, Object> metadata = new HashMap<>();
			Map<String, Object> lessonMap = new HashMap<>();
			when(teacherVideoPromptBuilder.buildTeacherVideoPrompt(lessonMap)).thenReturn("prompt");
			when(heyGenClient.createTeacherVideo("prompt")).thenThrow(new HeyGenExternalException("HeyGen unavailable"));

			// When-Then:
			AppException thrown = assertThrows(AppException.class,
					() -> workflow.requestTeacherVideo(lesson, metadata, lessonMap));
			assertThat(thrown.getCode()).isEqualTo(ErrorReason.C003.name());
			verifyNoInteractions(lessonEntityService);
		}
	}

	@Nested
	class PrepareLessonForPrompt {

		@Test
		void shouldMergePreparedMaterialsIntoMetadataAndLessonMapTest() {
			// Given:
			Long lessonId = 11L;
			Lesson lesson = Instancio.of(Lesson.class).set(field(Lesson::getId), lessonId).create();
			Map<String, Object> metadata = new HashMap<>();
			Map<String, Object> lessonMap = new HashMap<>(Map.of("title", "Lesson title"));
			when(lessonMapper.toDetailMap(lesson)).thenReturn(lessonMap);
			PreparedMaterialsResult preparedMaterialsResult = mock(PreparedMaterialsResult.class);
			Map<String, Object> preparedMap = Map.of("materials", List.of());
			when(preparedMaterialsResult.toLegacyMap()).thenReturn(preparedMap);
			when(materialPreparationService.prepareForLesson(lessonId)).thenReturn(preparedMaterialsResult);

			// When:
			Map<String, Object> result = workflow.prepareLessonForPrompt(lesson, lessonId, metadata);

			// Then:
			assertThat(result).isSameAs(lessonMap);
			assertThat(metadata).containsEntry("preparedMaterials", preparedMap);
			assertThat(result.get("generationMetadata")).isSameAs(metadata);
		}
	}

	@Nested
	class RejectIfActiveGeneration {

		@Test
		void shouldNotThrowWhenNoActiveTeacherVideoExistsTest() {
			// Given:
			Map<String, Object> metadata = new HashMap<>();
			when(lessonMapper.toTeacherVideoRecord(Map.of())).thenReturn(null);
			when(teacherVideoMetadataSupport.hasActiveTeacherVideo(null)).thenReturn(false);

			// When-Then:
			assertDoesNotThrow(() -> workflow.rejectIfActiveGeneration(metadata));
		}

		@Test
		void shouldThrowWhenTeacherVideoIsActiveTest() {
			// Given:
			Map<String, Object> existingTeacherVideoMap = Map.of("videoId", "video-9", "status", "processing");
			Map<String, Object> metadata = new HashMap<>(Map.of("teacherVideo", existingTeacherVideoMap));
			TeacherVideoRecord existingRecord = mock(TeacherVideoRecord.class);
			when(lessonMapper.toTeacherVideoRecord(existingTeacherVideoMap)).thenReturn(existingRecord);
			when(teacherVideoMetadataSupport.hasActiveTeacherVideo(existingRecord)).thenReturn(true);

			// When-Then:
			AppException thrown = assertThrows(AppException.class, () -> workflow.rejectIfActiveGeneration(metadata));
			assertThat(thrown.getCode()).isEqualTo(ErrorReason.C006.name());
		}
	}

	@Nested
	class CastMap {

		@Test
		void shouldReturnTheSameMapWhenValueIsAMapTest() {
			// Given:
			Map<String, Object> source = Map.of("videoId", "video-1");

			// When:
			Map<String, Object> result = workflow.castMap(source);

			// Then:
			assertThat(result).isEqualTo(source);
		}

		@Test
		void shouldReturnEmptyMapWhenValueIsNotAMapTest() {
			// Given:
			Object value = "not-a-map";

			// When:
			Map<String, Object> result = workflow.castMap(value);

			// Then:
			assertThat(result).isEmpty();
		}

		@Test
		void shouldReturnEmptyMapWhenValueIsNullTest() {
			// Given:
			Object value = null;

			// When:
			Map<String, Object> result = workflow.castMap(value);

			// Then:
			assertThat(result).isEmpty();
		}
	}
}
