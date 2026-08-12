package com.aidigital.aionboarding.service.roadmap.support;

import com.aidigital.aionboarding.domain.learning.entities.UserLesson;
import com.aidigital.aionboarding.domain.learning.entities.UserRoadmap;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.roadmap.entities.Roadmap;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.common.time.CurrentTimeImpl;
import com.aidigital.aionboarding.service.learning.services.entity.LearningEnrollmentEntityService;
import com.aidigital.aionboarding.service.learning.support.LearningEnrollmentSupport;
import com.aidigital.aionboarding.service.user.services.entity.UserEntityService;
import org.instancio.Instancio;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.Select.field;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoadmapEnrollmentFanOutSupportTest {

	@Mock
	private LearningEnrollmentEntityService learningEnrollmentEntityService;
	@Mock
	private UserEntityService userEntityService;
	@Mock
	private LearningEnrollmentSupport learningEnrollmentSupport;
	@Spy
	private CurrentTime currentTime = new CurrentTimeImpl();

	@InjectMocks
	private RoadmapEnrollmentFanOutSupport support;

	@Nested
	class FanOutLessonsToEnrolledUsers {

		@Test
		void shouldDoNothingWhenNoEnrolleesExistTest() {
			// Given:
			Long roadmapId = 10L;
			when(learningEnrollmentEntityService.findUserRoadmapsByRoadmapId(roadmapId)).thenReturn(List.of());

			// When:
			support.fanOutLessonsToEnrolledUsers(roadmapId, List.of(new Lesson()));

			// Then:
			verifyNoInteractions(learningEnrollmentSupport);
			verify(learningEnrollmentEntityService, never()).saveAllUserLessons(org.mockito.ArgumentMatchers.any());
		}

		@Test
		void shouldDoNothingWhenLessonListIsEmptyTest() {
			// Given:
			Long roadmapId = 11L;
			UserRoadmap enrollment = Instancio.create(UserRoadmap.class);
			when(learningEnrollmentEntityService.findUserRoadmapsByRoadmapId(roadmapId)).thenReturn(List.of(enrollment));

			// When:
			support.fanOutLessonsToEnrolledUsers(roadmapId, List.of());

			// Then:
			verifyNoInteractions(learningEnrollmentSupport);
		}

		@Test
		void shouldEnrollOnlyUsersMissingTheLessonTest() {
			// Given:
			Long roadmapId = 12L;
			Long lessonId = 100L;
			Lesson lesson = Instancio.of(Lesson.class).set(field(Lesson::getId), lessonId).create();

			UserRoadmap.UserRoadmapId enrolledUserId = new UserRoadmap.UserRoadmapId();
			enrolledUserId.setUserId(20L);
			enrolledUserId.setRoadmapId(roadmapId);
			UserRoadmap alreadyEnrolled = new UserRoadmap();
			alreadyEnrolled.setId(enrolledUserId);

			UserRoadmap.UserRoadmapId missingUserId = new UserRoadmap.UserRoadmapId();
			missingUserId.setUserId(21L);
			missingUserId.setRoadmapId(roadmapId);
			UserRoadmap missingEnrollment = new UserRoadmap();
			missingEnrollment.setId(missingUserId);

			when(learningEnrollmentEntityService.findUserRoadmapsByRoadmapId(roadmapId))
					.thenReturn(List.of(alreadyEnrolled, missingEnrollment));

			UserLesson.UserLessonId existingKey = new UserLesson.UserLessonId();
			existingKey.setUserId(20L);
			existingKey.setLessonId(lessonId);
			UserLesson existingUserLesson = new UserLesson();
			existingUserLesson.setId(existingKey);
			when(learningEnrollmentEntityService.findUserLessonsByUserIdsAndLessonIds(List.of(20L, 21L), List.of(lessonId)))
					.thenReturn(List.of(existingUserLesson));

			UserLesson.UserLessonId newKey = new UserLesson.UserLessonId();
			newKey.setUserId(21L);
			newKey.setLessonId(lessonId);
			when(learningEnrollmentSupport.userLessonId(21L, lessonId)).thenReturn(newKey);

			// When:
			support.fanOutLessonsToEnrolledUsers(roadmapId, List.of(lesson));

			// Then: only user 21 (missing) gets a new row; user 20 (already enrolled) is skipped
			ArgumentCaptor<List<UserLesson>> captor = ArgumentCaptor.forClass(List.class);
			verify(learningEnrollmentEntityService).saveAllUserLessons(captor.capture());
			assertThat(captor.getValue()).hasSize(1);
			assertThat(captor.getValue().get(0).getId()).isEqualTo(newKey);
		}
	}

	@Nested
	class GetViewerEnrollmentsByRoadmapId {

		@Test
		void shouldReturnEmptyMapWhenViewerIsNullTest() {
			// When:
			Map<Long, UserRoadmap> result = support.getViewerEnrollmentsByRoadmapId(null, List.of(new Roadmap()));

			// Then:
			assertThat(result).isEmpty();
		}

		@Test
		void shouldReturnEmptyMapWhenRoadmapListIsEmptyTest() {
			// Given:
			AppUser viewer = new AppUser(1L, "clerk-1", "a@test.com", "A", "admin", "A", null, null, null);

			// When:
			Map<Long, UserRoadmap> result = support.getViewerEnrollmentsByRoadmapId(viewer, List.of());

			// Then:
			assertThat(result).isEmpty();
		}

		@Test
		void shouldKeyEnrollmentsByRoadmapIdTest() {
			// Given:
			AppUser viewer = new AppUser(1L, "clerk-1", "a@test.com", "A", "admin", "A", null, null, null);
			Roadmap roadmap = Instancio.of(Roadmap.class).set(field(Roadmap::getId), 30L).create();
			UserRoadmap.UserRoadmapId enrollmentId = new UserRoadmap.UserRoadmapId();
			enrollmentId.setUserId(1L);
			enrollmentId.setRoadmapId(30L);
			UserRoadmap enrollment = new UserRoadmap();
			enrollment.setId(enrollmentId);
			when(learningEnrollmentEntityService.findUserRoadmapsByUserIdAndRoadmapIds(1L, List.of(30L)))
					.thenReturn(List.of(enrollment));

			// When:
			Map<Long, UserRoadmap> result = support.getViewerEnrollmentsByRoadmapId(viewer, List.of(roadmap));

			// Then:
			assertThat(result).containsEntry(30L, enrollment);
		}
	}
}
