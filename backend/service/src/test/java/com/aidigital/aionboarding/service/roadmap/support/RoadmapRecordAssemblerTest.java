package com.aidigital.aionboarding.service.roadmap.support;

import com.aidigital.aionboarding.domain.learning.entities.UserLesson;
import com.aidigital.aionboarding.domain.learning.entities.UserRoadmap;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.roadmap.entities.Roadmap;
import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapLesson;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.learning.services.entity.LearningEnrollmentEntityService;
import com.aidigital.aionboarding.service.mappers.roadmap.RoadmapMapper;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapLessonRecord;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoadmapRecordAssemblerTest {

	@Mock
	private RoadmapMapper roadmapMapper;
	@Mock
	private LearningEnrollmentEntityService learningEnrollmentEntityService;

	@InjectMocks
	private RoadmapRecordAssembler assembler;

	@Test
	void shouldAssembleRecordWithPrecomputedCompletionMapTest() {
		// Given:
		Roadmap roadmap = new Roadmap();
		roadmap.setId(1L);
		roadmap.setTitle("Roadmap");
		roadmap.setDescription("Desc");
		roadmap.setTags(List.of("tag"));
		RoadmapLesson roadmapLesson = roadmapLesson(1L, 10L, 0);
		RoadmapRecord base = new RoadmapRecord(
				1L, "Roadmap", "Desc", List.of("tag"),
				List.of(), List.of(), false, null, false,
				"creator", LocalDateTime.parse("2026-07-15T10:00:00"),
				LocalDateTime.parse("2026-07-15T11:00:00")
		);
		when(roadmapMapper.toRecord(eq(roadmap))).thenReturn(base);

		// When:
		RoadmapRecord result = assembler.toRecord(roadmap, Set.of(1L), null, List.of(roadmapLesson),
				Map.of(10L, true));

		// Then:
		assertThat(result.id()).isEqualTo(1L);
		assertThat(result.lessonIds()).containsExactly(10L);
		assertThat(result.lessons()).hasSize(1);
		assertThat(result.lessons().get(0).isCompleted()).isTrue();
		assertThat(result.viewerCanManage()).isTrue();
		assertThat(result.isEnrolled()).isFalse();
	}

	@Test
	void shouldAssembleRecordWithViewerCompletionMapTest() {
		// Given:
		AppUser viewer = new AppUser(1L, "clerk-1", "viewer@test.com", "Viewer", "member", "Viewer", null, null,
				null);
		Roadmap roadmap = new Roadmap();
		roadmap.setId(2L);
		roadmap.setTitle("Roadmap");
		roadmap.setDescription("Desc");
		roadmap.setTags(List.of());
		RoadmapLesson roadmapLesson = roadmapLesson(2L, 20L, 1);
		RoadmapRecord base = new RoadmapRecord(
				2L, "Roadmap", "Desc", List.of(),
				List.of(), List.of(), false, null, false,
				"creator", LocalDateTime.parse("2026-07-15T10:00:00"),
				LocalDateTime.parse("2026-07-15T11:00:00")
		);
		UserLesson enrollment = new UserLesson();
		UserLesson.UserLessonId id = new UserLesson.UserLessonId();
		id.setLessonId(20L);
		enrollment.setId(id);
		enrollment.setCompletedAt(LocalDateTime.parse("2026-07-15T12:00:00"));
		when(roadmapMapper.toRecord(eq(roadmap))).thenReturn(base);
		when(learningEnrollmentEntityService.findByUserIdAndLessonIdIn(eq(1L), eq(List.of(20L))))
				.thenReturn(List.of(enrollment));

		// When:
		RoadmapRecord result = assembler.toRecord(roadmap, viewer, null, null, List.of(roadmapLesson));

		// Then:
		assertThat(result.lessons()).hasSize(1);
		assertThat(result.lessons().get(0).isCompleted()).isTrue();
	}

	@Test
	void shouldReturnEmptyCompletionMapWhenViewerIdNullTest() {
		// Given:
		AppUser viewer = new AppUser(null, "clerk-1", "viewer@test.com", "Viewer", "member", "Viewer", null, null,
				null);

		// When:
		Map<Long, Boolean> result = assembler.lessonCompletionMap(viewer, List.of(1L));

		// Then:
		assertThat(result).isEmpty();
	}

	@Test
	void shouldReturnEmptyCompletionMapWhenLessonIdsEmptyTest() {
		// Given:
		AppUser viewer = new AppUser(1L, "clerk-1", "viewer@test.com", "Viewer", "member", "Viewer", null, null,
				null);

		// When:
		Map<Long, Boolean> result = assembler.lessonCompletionMap(viewer, List.of());

		// Then:
		assertThat(result).isEmpty();
	}

	@Test
	void shouldBuildLessonRecordWithStatusAndCompletionTest() {
		// Given:
		RoadmapLesson roadmapLesson = roadmapLesson(3L, 30L, 0);
		roadmapLesson.getLesson().setStatus(null);

		// When:
		RoadmapLessonRecord result = assembler.toLessonRecord(roadmapLesson, Map.of(30L, false));

		// Then:
		assertThat(result.id()).isEqualTo(30L);
		assertThat(result.status()).isNull();
		assertThat(result.isCompleted()).isFalse();
	}

	@Test
	void shouldBuildLessonRecordWithNullCompletionWhenNotInMapTest() {
		// Given:
		RoadmapLesson roadmapLesson = roadmapLesson(4L, 40L, 2);

		// When:
		RoadmapLessonRecord result = assembler.toLessonRecord(roadmapLesson, Map.of());

		// Then:
		assertThat(result.isCompleted()).isNull();
	}

	private RoadmapLesson roadmapLesson(Long roadmapId, Long lessonId, Integer sortOrder) {
		RoadmapLesson rl = new RoadmapLesson();
		RoadmapLesson.RoadmapLessonId id = new RoadmapLesson.RoadmapLessonId();
		id.setRoadmapId(roadmapId);
		id.setLessonId(lessonId);
		rl.setId(id);
		rl.setSortOrder(sortOrder);
		Lesson lesson = new Lesson();
		lesson.setId(lessonId);
		lesson.setTitle("Lesson " + lessonId);
		lesson.setDescription("Description");
		com.aidigital.aionboarding.domain.common.dictionary.entities.LessonStatus status =
				new com.aidigital.aionboarding.domain.common.dictionary.entities.LessonStatus();
		status.setCode("ready");
		lesson.setStatus(status);
		lesson.setCreatedAt(LocalDateTime.parse("2026-07-15T10:00:00"));
		rl.setLesson(lesson);
		return rl;
	}
}
