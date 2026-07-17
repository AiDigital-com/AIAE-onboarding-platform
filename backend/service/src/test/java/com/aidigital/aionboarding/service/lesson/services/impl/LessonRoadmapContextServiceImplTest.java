package com.aidigital.aionboarding.service.lesson.services.impl;

import com.aidigital.aionboarding.domain.learning.entities.UserRoadmap;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.roadmap.entities.Roadmap;
import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapLesson;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.learning.services.entity.LearningEnrollmentEntityService;
import com.aidigital.aionboarding.service.lesson.models.LessonRoadmapContextRecord;
import com.aidigital.aionboarding.service.roadmap.services.entity.RoadmapEntityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonRoadmapContextServiceImplTest {

	@Mock
	private LearningEnrollmentEntityService learningEnrollmentEntityService;
	@Mock
	private RoadmapEntityService roadmapEntityService;

	private LessonRoadmapContextServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new LessonRoadmapContextServiceImpl(learningEnrollmentEntityService, roadmapEntityService);
	}

	@Nested
	class buildContext {

		@Test
		void buildContext_noEnrollments_returnsNull() {
			// Given
			AppUser viewer = learnerViewer();
			Long lessonId = 10L;
			Lesson lesson = lessonWithId(lessonId);

			when(learningEnrollmentEntityService.findUserRoadmapsByUserId(viewer.internalId())).thenReturn(List.of());

			// Execution
			LessonRoadmapContextRecord result = service.buildContext(viewer, lesson);

			// Verification
			assertThat(result).isNull();
		}

		@Test
		void buildContext_enrolledButNoneContainLesson_returnsNull() {
			// Given
			AppUser viewer = learnerViewer();
			Long lessonId = 10L;
			Lesson lesson = lessonWithId(lessonId);
			Long roadmapId = 100L;

			UserRoadmap userRoadmap = userRoadmapWithIds(roadmapId);
			RoadmapLesson rl1 = roadmapLessonWithIds(roadmapId, 50L, 1);
			RoadmapLesson rl2 = roadmapLessonWithIds(roadmapId, 51L, 2);

			when(learningEnrollmentEntityService.findUserRoadmapsByUserId(viewer.internalId())).thenReturn(List.of(userRoadmap));
			when(roadmapEntityService.findAllByRoadmapIdsWithLessons(List.of(roadmapId)))
					.thenReturn(List.of(rl1, rl2));

			// Execution
			LessonRoadmapContextRecord result = service.buildContext(viewer, lesson);

			// Verification
			assertThat(result).isNull();
		}

		@Test
		void buildContext_adminNotEnrolledAnywhere_returnsNull() {
			// Given: an admin previewing a lesson they are not personally enrolled to learn must not
			// be navigated into a roadmap they have no assignment to
			AppUser admin = adminViewer();
			Long lessonId = 10L;
			Lesson lesson = lessonWithId(lessonId);

			when(learningEnrollmentEntityService.findUserRoadmapsByUserId(admin.internalId())).thenReturn(List.of());

			// Execution
			LessonRoadmapContextRecord result = service.buildContext(admin, lesson);

			// Verification
			assertThat(result).isNull();
			verify(roadmapEntityService, never()).findByIdLessonId(anyLong());
		}

		@Test
		void buildContext_lessonIsFirstInRoadmap_previousLessonIdIsNull() {
			// Given
			AppUser viewer = learnerViewer();
			Long lessonId = 10L;
			Lesson lesson = lessonWithId(lessonId);
			Long roadmapId = 100L;

			UserRoadmap userRoadmap = userRoadmapWithIds(roadmapId);
			RoadmapLesson rl1 = roadmapLessonWithIds(roadmapId, lessonId, 1); // this lesson is first
			RoadmapLesson rl2 = roadmapLessonWithIds(roadmapId, 20L, 2);

			Roadmap roadmap = roadmapWithTitle("My Roadmap");

			when(learningEnrollmentEntityService.findUserRoadmapsByUserId(viewer.internalId())).thenReturn(List.of(userRoadmap));
			when(roadmapEntityService.findAllByRoadmapIdsWithLessons(List.of(roadmapId)))
					.thenReturn(List.of(rl1, rl2));
			when(roadmapEntityService.findById(roadmapId)).thenReturn(Optional.of(roadmap));

			// Execution
			LessonRoadmapContextRecord result = service.buildContext(viewer, lesson);

			// Verification
			assertThat(result).isNotNull();
			assertThat(result.previousLessonId()).isNull();
			assertThat(result.nextLessonId()).isEqualTo(20L);
			assertThat(result.positionInRoadmap()).isEqualTo(1);
		}

		@Test
		void buildContext_lessonIsLastInRoadmap_nextLessonIdIsNull() {
			// Given
			AppUser viewer = learnerViewer();
			Long lessonId = 10L;
			Lesson lesson = lessonWithId(lessonId);
			Long roadmapId = 100L;

			UserRoadmap userRoadmap = userRoadmapWithIds(roadmapId);
			RoadmapLesson rl1 = roadmapLessonWithIds(roadmapId, 5L, 1);
			RoadmapLesson rl2 = roadmapLessonWithIds(roadmapId, lessonId, 2); // this lesson is last

			Roadmap roadmap = roadmapWithTitle("My Roadmap");

			when(learningEnrollmentEntityService.findUserRoadmapsByUserId(viewer.internalId())).thenReturn(List.of(userRoadmap));
			when(roadmapEntityService.findAllByRoadmapIdsWithLessons(List.of(roadmapId)))
					.thenReturn(List.of(rl1, rl2));
			when(roadmapEntityService.findById(roadmapId)).thenReturn(Optional.of(roadmap));

			// Execution
			LessonRoadmapContextRecord result = service.buildContext(viewer, lesson);

			// Verification
			assertThat(result).isNotNull();
			assertThat(result.nextLessonId()).isNull();
			assertThat(result.previousLessonId()).isEqualTo(5L);
			assertThat(result.positionInRoadmap()).isEqualTo(2);
		}

		@Test
		void buildContext_lessonIsMiddleInRoadmap_bothNavIdsSet() {
			// Given
			AppUser viewer = learnerViewer();
			Long lessonId = 10L;
			Lesson lesson = lessonWithId(lessonId);
			Long roadmapId = 100L;

			UserRoadmap userRoadmap = userRoadmapWithIds(roadmapId);
			RoadmapLesson rl1 = roadmapLessonWithIds(roadmapId, 5L, 1);
			RoadmapLesson rl2 = roadmapLessonWithIds(roadmapId, lessonId, 2); // middle
			RoadmapLesson rl3 = roadmapLessonWithIds(roadmapId, 20L, 3);

			Roadmap roadmap = roadmapWithTitle("Full Roadmap");

			when(learningEnrollmentEntityService.findUserRoadmapsByUserId(viewer.internalId())).thenReturn(List.of(userRoadmap));
			when(roadmapEntityService.findAllByRoadmapIdsWithLessons(List.of(roadmapId)))
					.thenReturn(List.of(rl1, rl2, rl3));
			when(roadmapEntityService.findById(roadmapId)).thenReturn(Optional.of(roadmap));

			// Execution
			LessonRoadmapContextRecord result = service.buildContext(viewer, lesson);

			// Verification
			assertThat(result).isNotNull();
			assertThat(result.previousLessonId()).isEqualTo(5L);
			assertThat(result.nextLessonId()).isEqualTo(20L);
			assertThat(result.positionInRoadmap()).isEqualTo(2);
			assertThat(result.totalLessons()).isEqualTo(3);
		}

		@Test
		void buildContext_roadmapTitleLoadedFromRepository() {
			// Given
			AppUser viewer = learnerViewer();
			Long lessonId = 10L;
			Lesson lesson = lessonWithId(lessonId);
			Long roadmapId = 100L;

			UserRoadmap userRoadmap = userRoadmapWithIds(roadmapId);
			RoadmapLesson rl1 = roadmapLessonWithIds(roadmapId, lessonId, 1);

			Roadmap roadmap = roadmapWithTitle("Roadmap Title From DB");

			when(learningEnrollmentEntityService.findUserRoadmapsByUserId(viewer.internalId())).thenReturn(List.of(userRoadmap));
			when(roadmapEntityService.findAllByRoadmapIdsWithLessons(List.of(roadmapId)))
					.thenReturn(List.of(rl1));
			when(roadmapEntityService.findById(roadmapId)).thenReturn(Optional.of(roadmap));

			// Execution
			LessonRoadmapContextRecord result = service.buildContext(viewer, lesson);

			// Verification
			assertThat(result).isNotNull();
			assertThat(result.roadmapTitle()).isEqualTo("Roadmap Title From DB");
			assertThat(result.roadmapId()).isEqualTo(roadmapId);
		}

		@Test
		void buildContext_positionInRoadmapIsOneBased() {
			// Given
			AppUser viewer = learnerViewer();
			Long lessonId = 10L;
			Lesson lesson = lessonWithId(lessonId);
			Long roadmapId = 100L;

			UserRoadmap userRoadmap = userRoadmapWithIds(roadmapId);
			RoadmapLesson rl1 = roadmapLessonWithIds(roadmapId, 1L, 1);
			RoadmapLesson rl2 = roadmapLessonWithIds(roadmapId, 2L, 2);
			RoadmapLesson rl3 = roadmapLessonWithIds(roadmapId, lessonId, 3); // index 2 → position 3

			Roadmap roadmap = roadmapWithTitle("Roadmap");

			when(learningEnrollmentEntityService.findUserRoadmapsByUserId(viewer.internalId())).thenReturn(List.of(userRoadmap));
			when(roadmapEntityService.findAllByRoadmapIdsWithLessons(List.of(roadmapId)))
					.thenReturn(List.of(rl1, rl2, rl3));
			when(roadmapEntityService.findById(roadmapId)).thenReturn(Optional.of(roadmap));

			// Execution
			LessonRoadmapContextRecord result = service.buildContext(viewer, lesson);

			// Verification
			assertThat(result).isNotNull();
			assertThat(result.positionInRoadmap()).isEqualTo(3); // 1-based: index 2 + 1
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

	private Lesson lessonWithId(Long id) {
		Lesson lesson = mock(Lesson.class);
		lenient().when(lesson.getId()).thenReturn(id);
		return lesson;
	}

	private UserRoadmap userRoadmapWithIds(Long roadmapId) {
		UserRoadmap userRoadmap = mock(UserRoadmap.class);
		UserRoadmap.UserRoadmapId id = mock(UserRoadmap.UserRoadmapId.class);
		when(id.getRoadmapId()).thenReturn(roadmapId);
		when(userRoadmap.getId()).thenReturn(id);
		return userRoadmap;
	}

	private RoadmapLesson roadmapLessonWithIds(Long roadmapId, Long lessonId, int sortOrder) {
		RoadmapLesson rl = mock(RoadmapLesson.class);
		RoadmapLesson.RoadmapLessonId id = mock(RoadmapLesson.RoadmapLessonId.class);
		lenient().when(id.getRoadmapId()).thenReturn(roadmapId);
		lenient().when(id.getLessonId()).thenReturn(lessonId);
		lenient().when(rl.getId()).thenReturn(id);
		return rl;
	}

	private Roadmap roadmapWithTitle(String title) {
		Roadmap roadmap = mock(Roadmap.class);
		lenient().when(roadmap.getTitle()).thenReturn(title);
		return roadmap;
	}
}
