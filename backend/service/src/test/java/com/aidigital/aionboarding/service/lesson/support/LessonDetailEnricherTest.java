package com.aidigital.aionboarding.service.lesson.support;

import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.lesson.models.LessonDetailRecord;
import com.aidigital.aionboarding.service.lesson.models.LessonRoadmapContextRecord;
import com.aidigital.aionboarding.service.lesson.models.TeacherVideoRecord;
import com.aidigital.aionboarding.service.lesson.services.LessonRoadmapContextService;
import com.aidigital.aionboarding.service.teachervideo.services.TeacherVideoRefreshService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonDetailEnricherTest {

	@Mock
	private LessonRecordAssembler lessonMapper;
	@Mock
	private TeacherVideoRefreshService teacherVideoRefreshService;
	@Mock
	private LessonRoadmapContextService lessonRoadmapContextService;

	private LessonDetailEnricher enricher;

	@BeforeEach
	void setUp() {
		enricher = new LessonDetailEnricher(
				lessonMapper,
				teacherVideoRefreshService,
				lessonRoadmapContextService
		);
	}

	@Test
	void toEnrichedDetailRecordShouldRefreshTeacherVideoBeforeAssemblyTest() {
		AppUser viewer = viewer();
		Lesson lesson = mock(Lesson.class);
		Lesson refreshedLesson = mock(Lesson.class);
		TeacherVideoRecord teacherVideo = teacherVideo("vid-123");
		LessonDetailRecord detail = detail(null);
		Map<String, Object> teacherVideoMap = Map.of("videoId", "vid-123");

		when(lesson.getGenerationMetadata()).thenReturn(Map.of("teacherVideo", teacherVideoMap));
		when(lessonMapper.toTeacherVideoRecord(teacherVideoMap)).thenReturn(teacherVideo);
		when(teacherVideoRefreshService.refreshTeacherVideoIfNeeded(lesson, teacherVideo, false))
				.thenReturn(new TeacherVideoRefreshService.RefreshResult(refreshedLesson, teacherVideo));
		when(lessonMapper.toDetailRecord(refreshedLesson)).thenReturn(detail);
		when(lessonRoadmapContextService.buildContext(viewer, refreshedLesson)).thenReturn(null);

		LessonDetailRecord result = enricher.toEnrichedDetailRecord(viewer, lesson);

		assertThat(result).isSameAs(detail);
		verify(lessonMapper).toDetailRecord(refreshedLesson);
	}

	@Test
	void toEnrichedDetailRecordShouldAttachRoadmapContextWhenAvailableTest() {
		AppUser viewer = viewer();
		Lesson lesson = mock(Lesson.class);
		LessonDetailRecord detail = detail(null);
		LessonRoadmapContextRecord roadmapContext =
				new LessonRoadmapContextRecord(10L, "Roadmap", 2, 4, 1L, 3L);

		when(lesson.getGenerationMetadata()).thenReturn(null);
		when(lessonMapper.toDetailRecord(lesson)).thenReturn(detail);
		when(lessonRoadmapContextService.buildContext(viewer, lesson)).thenReturn(roadmapContext);

		LessonDetailRecord result = enricher.toEnrichedDetailRecord(viewer, lesson);

		assertThat(result.roadmapContext()).isEqualTo(roadmapContext);
		assertThat(result.id()).isEqualTo(detail.id());
		assertThat(result.title()).isEqualTo(detail.title());
		verify(teacherVideoRefreshService, never()).refreshTeacherVideoIfNeeded(
				org.mockito.ArgumentMatchers.isA(Lesson.class),
				org.mockito.ArgumentMatchers.isA(TeacherVideoRecord.class),
				org.mockito.ArgumentMatchers.eq(false)
		);
	}

	private AppUser viewer() {
		return new AppUser(1L, "clerk-1", "learner@test.com", "Learner", "learner", "Learner", null, null, null);
	}

	private TeacherVideoRecord teacherVideo(String videoId) {
		return new TeacherVideoRecord(
				"heygen", "", "", "", "", videoId, "completed",
				"", null, "", "", "", null, "", ""
		);
	}

	private LessonDetailRecord detail(LessonRoadmapContextRecord roadmapContext) {
		return new LessonDetailRecord(
				1L,
				"Lesson",
				"Description",
				"READY",
				"PUBLISHED",
				true,
				false,
				"",
				"standard",
				"neutral",
				"article",
				"markdown",
				"content",
				"<p>content</p>",
				null,
				null,
				null,
				List.of("tag"),
				Map.of(),
				List.of(),
				null,
				"Teacher",
				2L,
				LocalDateTime.parse("2026-07-03T09:00:00"),
				LocalDateTime.parse("2026-07-03T09:01:00"),
				LocalDateTime.parse("2026-07-03T09:02:00"),
				List.of(3L),
				List.of(),
				List.of(),
				roadmapContext
		);
	}
}
