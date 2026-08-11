package com.aidigital.aionboarding.service.teachervideo.services.impl;

import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.external.heygen.HeyGenClient;
import com.aidigital.aionboarding.external.heygen.HeyGenExternalException;
import com.aidigital.aionboarding.external.heygen.model.HeyGenVideoStatus;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.mapping.TextValueNormalizer;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.lesson.models.TeacherVideoRecord;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonEntityService;
import com.aidigital.aionboarding.service.lesson.support.LessonRecordAssembler;
import com.aidigital.aionboarding.service.teachervideo.services.TeacherVideoRefreshService.RefreshResult;
import com.aidigital.aionboarding.service.teachervideo.support.TeacherVideoMetadataSupport;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.instancio.Select.field;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherVideoRefreshServiceImplTest {

	@Mock
	private LessonEntityService lessonEntityService;
	@Mock
	private LessonRecordAssembler lessonMapper;
	@Mock
	private HeyGenClient heyGenClient;
	@Spy
	private TextValueNormalizer textValueNormalizer = new TextValueNormalizer();
	@Spy
	private CurrentTime currentTime = new CurrentTime();
	@Spy
	private TeacherVideoMetadataSupport teacherVideoMetadataSupport =
			new TeacherVideoMetadataSupport(new TextValueNormalizer());

	@InjectMocks
	private TeacherVideoRefreshServiceImpl service;

	@Test
	void refreshTeacherVideoIfNeededShouldReturnInputUnchangedWhenNoRefreshNeededTest() {
		// Given:
		Lesson lesson = Instancio.of(Lesson.class).set(field(Lesson::getId), 1L).create();
		TeacherVideoRecord teacherVideo = Instancio.of(TeacherVideoRecord.class)
				.set(field(TeacherVideoRecord::videoId), "")
				.create();

		// When:
		RefreshResult result = service.refreshTeacherVideoIfNeeded(lesson, teacherVideo, false);

		// Then:
		assertThat(result.lesson()).isSameAs(lesson);
		assertThat(result.teacherVideo()).isSameAs(teacherVideo);
		verify(lessonEntityService, never()).save(any());
	}

	@Test
	void refreshTeacherVideoIfNeededShouldSaveViaLessonEntityServiceOnForcedRefreshTest() {
		// Given:
		Lesson lesson = Instancio.of(Lesson.class)
				.set(field(Lesson::getId), 2L)
				.set(field(Lesson::getVersion), 5L)
				.set(field(Lesson::getGenerationMetadata), (Map<String, Object>) null)
				.create();
		TeacherVideoRecord teacherVideo = Instancio.of(TeacherVideoRecord.class)
				.set(field(TeacherVideoRecord::videoId), "video-42")
				.create();
		HeyGenVideoStatus status = Instancio.of(HeyGenVideoStatus.class)
				.set(field(HeyGenVideoStatus::status), "completed")
				.create();
		Lesson savedLesson = Instancio.of(Lesson.class).set(field(Lesson::getId), 2L).create();
		when(lessonEntityService.claimForTeacherVideoRefresh(2L, 5L)).thenReturn(true);
		when(heyGenClient.getVideoStatus("video-42")).thenReturn(status);
		when(lessonMapper.toTeacherVideoMap(any(TeacherVideoRecord.class))).thenReturn(Map.of());
		when(lessonEntityService.save(lesson)).thenReturn(savedLesson);

		// When:
		RefreshResult result = service.refreshTeacherVideoIfNeeded(lesson, teacherVideo, true);

		// Then:
		assertThat(result.lesson()).isSameAs(savedLesson);
		assertThat(result.teacherVideo().status()).isEqualTo("completed");
		assertThat(lesson.getVersion()).isEqualTo(6L);
		verify(lessonEntityService).save(lesson);
	}

	@Test
	void refreshTeacherVideoIfNeededShouldThrowAppExceptionWhenHeyGenFailsTest() {
		// Given:
		Lesson lesson = Instancio.of(Lesson.class)
				.set(field(Lesson::getId), 3L)
				.set(field(Lesson::getVersion), 1L)
				.create();
		TeacherVideoRecord teacherVideo = Instancio.of(TeacherVideoRecord.class)
				.set(field(TeacherVideoRecord::videoId), "video-99")
				.create();
		when(lessonEntityService.claimForTeacherVideoRefresh(3L, 1L)).thenReturn(true);
		when(heyGenClient.getVideoStatus("video-99")).thenThrow(new HeyGenExternalException("boom"));

		// When-Then:
		assertThatThrownBy(() -> service.refreshTeacherVideoIfNeeded(lesson, teacherVideo, true))
				.isInstanceOf(AppException.class);
		verify(lessonEntityService, never()).save(any());
	}

	@Test
	void refreshTeacherVideoIfNeededShouldReturnInputUnchangedWhenAnotherCallerAlreadyWonTheClaimTest() {
		// Given: another node/thread already advanced the version before this call attempted
		// to claim it, so the conditional update matches zero rows.
		Lesson lesson = Instancio.of(Lesson.class)
				.set(field(Lesson::getId), 4L)
				.set(field(Lesson::getVersion), 7L)
				.create();
		TeacherVideoRecord teacherVideo = Instancio.of(TeacherVideoRecord.class)
				.set(field(TeacherVideoRecord::videoId), "video-7")
				.set(field(TeacherVideoRecord::videoUrl), "")
				.create();
		when(lessonEntityService.claimForTeacherVideoRefresh(4L, 7L)).thenReturn(false);

		// When:
		RefreshResult result = service.refreshTeacherVideoIfNeeded(lesson, teacherVideo, false);

		// Then:
		assertThat(result.lesson()).isSameAs(lesson);
		assertThat(result.teacherVideo()).isSameAs(teacherVideo);
		assertThat(lesson.getVersion()).isEqualTo(7L);
		verify(heyGenClient, never()).getVideoStatus(any());
		verify(lessonEntityService, never()).save(any());
	}
}
