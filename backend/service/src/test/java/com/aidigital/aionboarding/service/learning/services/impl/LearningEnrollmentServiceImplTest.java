package com.aidigital.aionboarding.service.learning.services.impl;

import com.aidigital.aionboarding.domain.common.dictionary.LessonPublicationStatusCode;
import com.aidigital.aionboarding.domain.common.dictionary.LessonStatusCode;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonPublicationStatus;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonStatus;
import com.aidigital.aionboarding.domain.learning.entities.UserLesson;
import com.aidigital.aionboarding.domain.learning.entities.UserRoadmap;
import com.aidigital.aionboarding.domain.learning.repositories.MyLessonSummaryProjection;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.roadmap.entities.Roadmap;
import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapLesson;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.learning.models.LessonEnrollmentRecord;
import com.aidigital.aionboarding.service.learning.services.entity.LearningEnrollmentEntityService;
import com.aidigital.aionboarding.service.learning.support.LearningEnrollmentSupport;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonEntityService;
import com.aidigital.aionboarding.service.lessonactivity.models.LessonActivityCountsRecord;
import com.aidigital.aionboarding.service.lessonactivity.services.LessonActivityAssemblyService;
import com.aidigital.aionboarding.service.roadmap.services.entity.RoadmapEntityService;
import com.aidigital.aionboarding.service.user.services.entity.UserEntityService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningEnrollmentServiceImplTest {

	@Mock
	private LessonEntityService lessonEntityService;
	@Mock
	private RoadmapEntityService roadmapEntityService;
	@Mock
	private LearningEnrollmentEntityService learningEnrollmentEntityService;
	@Mock
	private UserEntityService userEntityService;
	@Mock
	private LearningEnrollmentSupport enrollmentSupport;
	@Mock
	private LessonActivityAssemblyService lessonActivityAssemblyService;
	@Mock
	private CurrentTime currentTime;

	@InjectMocks
	private LearningEnrollmentServiceImpl service;

	@Nested
	class GetMyLessons {

		@Test
		void shouldReturnEmptyPageWhenNoEnrollmentsTest() {
			// Given:
			AppUser viewer = viewer();
			Pageable pageable = PageRequest.of(0, 20);
			when(learningEnrollmentEntityService.findMyLessonsPage(viewer.internalId(), pageable))
					.thenReturn(new PageImpl<>(List.of(), pageable, 0));
			when(lessonActivityAssemblyService.countActivitiesByLessonIds(List.of())).thenReturn(java.util.Map.of());
			when(lessonEntityService.findIdsWithTeacherVideoIn(List.of())).thenReturn(java.util.Set.of());

			// When:
			Page<com.aidigital.aionboarding.service.learning.models.MyLessonSummaryRecord> result =
					service.getMyLessons(viewer, pageable);

			// Then:
			assertThat(result.getContent()).isEmpty();
		}

		@Test
		void shouldMapProjectionFieldsAndMergeActivityCountsAndTeacherVideoFlagTest() {
			// Given:
			AppUser viewer = viewer();
			Pageable pageable = PageRequest.of(0, 20);
			LocalDateTime enrolledAt = LocalDateTime.of(2026, 1, 1, 0, 0);
			MyLessonSummaryProjection summary = new MyLessonSummaryProjection(
					1L, "Title", "Description", "ready", "published",
					"cover-key", "cover.png", "image/png",
					"<p>preview</p>", "preview markdown",
					List.of("tag1", "tag2"), "Author", enrolledAt, enrolledAt,
					enrolledAt, null
			);
			when(learningEnrollmentEntityService.findMyLessonsPage(viewer.internalId(), pageable))
					.thenReturn(new PageImpl<>(List.of(summary), pageable, 1));
			when(lessonActivityAssemblyService.countActivitiesByLessonIds(List.of(1L)))
					.thenReturn(java.util.Map.of(1L, new LessonActivityCountsRecord(2, 3)));
			when(lessonEntityService.findIdsWithTeacherVideoIn(List.of(1L))).thenReturn(java.util.Set.of(1L));

			// When:
			Page<com.aidigital.aionboarding.service.learning.models.MyLessonSummaryRecord> result =
					service.getMyLessons(viewer, pageable);

			// Then:
			assertThat(result.getContent()).hasSize(1);
			var card = result.getContent().get(0);
			assertThat(card.id()).isEqualTo(1L);
			assertThat(card.title()).isEqualTo("Title");
			assertThat(card.contentHtmlPreview()).isEqualTo("<p>preview</p>");
			assertThat(card.hasTeacherVideo()).isTrue();
			assertThat(card.activityCounts()).isEqualTo(new LessonActivityCountsRecord(2, 3));
			assertThat(card.enrollment().enrolledAt()).isEqualTo(enrolledAt);
			assertThat(card.enrollment().isCompleted()).isFalse();
		}

		@Test
		void shouldDefaultToZeroActivityCountsAndNoTeacherVideoWhenMissingFromBatchLookupsTest() {
			// Given:
			AppUser viewer = viewer();
			Pageable pageable = PageRequest.of(0, 20);
			LocalDateTime enrolledAt = LocalDateTime.of(2026, 1, 1, 0, 0);
			LocalDateTime completedAt = LocalDateTime.of(2026, 1, 2, 0, 0);
			MyLessonSummaryProjection summary = new MyLessonSummaryProjection(
					5L, "Title", "Description", "ready", "published",
					"", "", "",
					"", "",
					List.of(), "Author", enrolledAt, enrolledAt,
					enrolledAt, completedAt
			);
			when(learningEnrollmentEntityService.findMyLessonsPage(viewer.internalId(), pageable))
					.thenReturn(new PageImpl<>(List.of(summary), pageable, 1));
			when(lessonActivityAssemblyService.countActivitiesByLessonIds(List.of(5L))).thenReturn(java.util.Map.of());
			when(lessonEntityService.findIdsWithTeacherVideoIn(List.of(5L))).thenReturn(java.util.Set.of());

			// When:
			Page<com.aidigital.aionboarding.service.learning.models.MyLessonSummaryRecord> result =
					service.getMyLessons(viewer, pageable);

			// Then:
			var card = result.getContent().get(0);
			assertThat(card.activityCounts()).isEqualTo(new LessonActivityCountsRecord(0, 0));
			assertThat(card.hasTeacherVideo()).isFalse();
			assertThat(card.enrollment().isCompleted()).isTrue();
		}
	}

	@Nested
	class GetLessonEnrollment {

		@Test
		void shouldReturnEnrollmentRecordWhenPresentTest() {
			// Given:
			AppUser viewer = viewer();
			Long lessonId = 10L;
			UserLesson enrollment = userLesson(1L, lessonId);
			LessonEnrollmentRecord record = new LessonEnrollmentRecord(lessonId, enrollment.getEnrolledAt(), null,
					false);
			when(lessonEntityService.getReference(lessonId)).thenReturn(new Lesson());
			when(enrollmentSupport.userLessonId(viewer.internalId(), lessonId))
					.thenReturn(new UserLesson.UserLessonId());
			when(learningEnrollmentEntityService.findUserLessonById(enrollmentSupport.userLessonId(viewer.internalId(),
					lessonId))).thenReturn(Optional.of(enrollment));
			when(enrollmentSupport.toLessonEnrollment(enrollment)).thenReturn(record);

			// When:
			LessonEnrollmentRecord result = service.getLessonEnrollment(viewer, lessonId);

			// Then:
			assertThat(result).isEqualTo(record);
		}

		@Test
		void shouldThrowWhenEnrollmentNotFoundTest() {
			// Given:
			AppUser viewer = viewer();
			Long lessonId = 10L;
			UserLesson.UserLessonId id = new UserLesson.UserLessonId();
			id.setUserId(viewer.internalId());
			id.setLessonId(lessonId);
			when(lessonEntityService.getReference(lessonId)).thenReturn(new Lesson());
			when(enrollmentSupport.userLessonId(viewer.internalId(), lessonId)).thenReturn(id);
			when(learningEnrollmentEntityService.findUserLessonById(id)).thenReturn(Optional.empty());

			// When-Then:
			assertThatThrownBy(() -> service.getLessonEnrollment(viewer, lessonId))
					.isInstanceOf(AppException.class)
					.hasMessageContaining("Enrollment not found");
		}
	}

	@Nested
	class FindLessonEnrollment {

		@Test
		void shouldReturnEmptyWhenUserLessonMissingTest() {
			// Given:
			AppUser viewer = viewer();
			Long lessonId = 10L;
			UserLesson.UserLessonId id = new UserLesson.UserLessonId();
			id.setUserId(viewer.internalId());
			id.setLessonId(lessonId);
			when(lessonEntityService.getReference(lessonId)).thenReturn(new Lesson());
			when(enrollmentSupport.userLessonId(viewer.internalId(), lessonId)).thenReturn(id);
			when(learningEnrollmentEntityService.findUserLessonById(id)).thenReturn(Optional.empty());

			// When:
			Optional<LessonEnrollmentRecord> result = service.findLessonEnrollment(viewer, lessonId);

			// Then:
			assertThat(result).isEmpty();
		}
	}

	@Nested
	class RequireEnrollableLesson {

		@Test
		void shouldReturnLessonWhenReadyAndPublishedTest() {
			// Given:
			Long lessonId = 1L;
			Lesson lesson = lessonWithStatus(LessonStatusCode.READY, LessonPublicationStatusCode.PUBLISHED);
			lesson.setId(lessonId);
			when(lessonEntityService.getReference(lessonId)).thenReturn(lesson);

			// When:
			Lesson result = service.requireEnrollableLesson(lessonId);

			// Then:
			assertThat(result).isSameAs(lesson);
		}

		@Test
		void shouldThrowWhenLessonNotReadyTest() {
			// Given:
			Long lessonId = 1L;
			Lesson lesson = lessonWithStatus(LessonStatusCode.DRAFT, LessonPublicationStatusCode.PUBLISHED);
			lesson.setId(lessonId);
			when(lessonEntityService.getReference(lessonId)).thenReturn(lesson);

			// When-Then:
			assertThatThrownBy(() -> service.requireEnrollableLesson(lessonId))
					.isInstanceOf(AppException.class)
					.hasMessageContaining("not ready");
		}

		@Test
		void shouldThrowWhenLessonNotPublishedTest() {
			// Given:
			Long lessonId = 1L;
			Lesson lesson = lessonWithStatus(LessonStatusCode.READY, LessonPublicationStatusCode.PRIVATE);
			lesson.setId(lessonId);
			when(lessonEntityService.getReference(lessonId)).thenReturn(lesson);

			// When-Then:
			assertThatThrownBy(() -> service.requireEnrollableLesson(lessonId))
					.isInstanceOf(AppException.class)
					.hasMessageContaining("not ready");
		}
	}

	@Nested
	class IsEnrollable {

		@Test
		void shouldReturnTrueForReadyAndPublishedLessonTest() {
			// Given:
			Lesson lesson = lessonWithStatus(LessonStatusCode.READY, LessonPublicationStatusCode.PUBLISHED);

			// When:
			boolean result = service.isEnrollable(lesson);

			// Then:
			assertThat(result).isTrue();
		}

		@Test
		void shouldReturnFalseForNonReadyLessonTest() {
			// Given:
			Lesson lesson = lessonWithStatus(LessonStatusCode.DRAFT, LessonPublicationStatusCode.PUBLISHED);

			// When:
			boolean result = service.isEnrollable(lesson);

			// Then:
			assertThat(result).isFalse();
		}

		@Test
		void shouldReturnFalseForNonPublishedLessonTest() {
			// Given:
			Lesson lesson = lessonWithStatus(LessonStatusCode.READY, LessonPublicationStatusCode.PRIVATE);

			// When:
			boolean result = service.isEnrollable(lesson);

			// Then:
			assertThat(result).isFalse();
		}
	}

	@Nested
	class EnrollUsersInLesson {

		@Test
		void shouldReturnEmptyListWhenUserIdsEmptyTest() {
			// Given:
			Lesson lesson = lessonWithStatus(LessonStatusCode.READY, LessonPublicationStatusCode.PUBLISHED);
			lesson.setId(1L);

			// When:
			List<UserLesson> result = service.enrollUsersInLesson(List.of(), lesson, LocalDateTime.now(), false);

			// Then:
			assertThat(result).isEmpty();
			verifyNoInteractions(learningEnrollmentEntityService);
		}

		@Test
		void shouldCreateNewEnrollmentsWhenNoneExistTest() {
			// Given:
			Long lessonId = 1L;
			Long userId = 10L;
			LocalDateTime enrolledAt = LocalDateTime.of(2026, 1, 1, 12, 0);
			Lesson lesson = lessonWithStatus(LessonStatusCode.READY, LessonPublicationStatusCode.PUBLISHED);
			lesson.setId(lessonId);
			UserLesson.UserLessonId id = new UserLesson.UserLessonId();
			id.setUserId(userId);
			id.setLessonId(lessonId);
			User user = userWithId(userId);
			when(enrollmentSupport.userLessonId(userId, lessonId)).thenReturn(id);
			when(userEntityService.getReference(userId)).thenReturn(user);
			when(learningEnrollmentEntityService.findUserLessonsByUserIdsAndLessonIds(List.of(userId), List.of(lessonId)))
					.thenReturn(List.of());

			// When:
			List<UserLesson> result = service.enrollUsersInLesson(List.of(userId), lesson, enrolledAt, false);

			// Then:
			assertThat(result).hasSize(1);
			assertThat(result.get(0).getId()).isEqualTo(id);
			assertThat(result.get(0).getLesson()).isEqualTo(lesson);
			ArgumentCaptor<List<UserLesson>> captor = ArgumentCaptor.forClass(List.class);
			verify(learningEnrollmentEntityService).saveAllUserLessons(captor.capture());
			assertThat(captor.getValue()).hasSize(1);
			assertThat(captor.getValue().get(0).getId()).isEqualTo(id);
			assertThat(captor.getValue().get(0).getEnrolledAt()).isEqualTo(enrolledAt);
		}

		@Test
		void shouldKeepExistingEnrollmentWithoutUpdateWhenUpdateExistingFalseTest() {
			// Given:
			Long lessonId = 1L;
			Long userId = 10L;
			Lesson lesson = lessonWithStatus(LessonStatusCode.READY, LessonPublicationStatusCode.PUBLISHED);
			lesson.setId(lessonId);
			UserLesson existing = userLesson(userId, lessonId);
			existing.setLesson(lesson);
			existing.setEnrolledAt(LocalDateTime.of(2020, 1, 1, 0, 0));
			when(learningEnrollmentEntityService.findUserLessonsByUserIdsAndLessonIds(List.of(userId), List.of(lessonId)))
					.thenReturn(List.of(existing));
			when(learningEnrollmentEntityService.saveAllUserLessons(List.of())).thenReturn(List.of());

			// When:
			List<UserLesson> result = service.enrollUsersInLesson(List.of(userId), lesson, LocalDateTime.now(), false);

			// Then:
			assertThat(result).containsExactly(existing);
			assertThat(existing.getEnrolledAt()).isEqualTo(LocalDateTime.of(2020, 1, 1, 0, 0));
			verify(learningEnrollmentEntityService).saveAllUserLessons(List.of());
		}

		@Test
		void shouldUpdateExistingEnrollmentWhenUpdateExistingTrueTest() {
			// Given:
			Long lessonId = 1L;
			Long userId = 10L;
			LocalDateTime newEnrolledAt = LocalDateTime.of(2026, 2, 1, 0, 0);
			Lesson lesson = lessonWithStatus(LessonStatusCode.READY, LessonPublicationStatusCode.PUBLISHED);
			lesson.setId(lessonId);
			UserLesson existing = userLesson(userId, lessonId);
			existing.setLesson(lesson);
			existing.setEnrolledAt(LocalDateTime.of(2020, 1, 1, 0, 0));
			when(learningEnrollmentEntityService.findUserLessonsByUserIdsAndLessonIds(List.of(userId), List.of(lessonId)))
					.thenReturn(List.of(existing));
			when(learningEnrollmentEntityService.saveAllUserLessons(List.of(existing))).thenReturn(List.of(existing));

			// When:
			List<UserLesson> result = service.enrollUsersInLesson(List.of(userId), lesson, newEnrolledAt, true);

			// Then:
			assertThat(result.get(0).getEnrolledAt()).isEqualTo(newEnrolledAt);
			verify(learningEnrollmentEntityService).saveAllUserLessons(List.of(existing));
		}

		@Test
		void shouldDeduplicateAndDropNullUserIdsWhenEnrollingTest() {
			// Given:
			Long lessonId = 1L;
			Long userId = 10L;
			LocalDateTime enrolledAt = LocalDateTime.of(2026, 1, 1, 12, 0);
			Lesson lesson = lessonWithStatus(LessonStatusCode.READY, LessonPublicationStatusCode.PUBLISHED);
			lesson.setId(lessonId);
			UserLesson.UserLessonId id = new UserLesson.UserLessonId();
			id.setUserId(userId);
			id.setLessonId(lessonId);
			User user = userWithId(userId);
			when(enrollmentSupport.userLessonId(userId, lessonId)).thenReturn(id);
			when(userEntityService.getReference(userId)).thenReturn(user);
			when(learningEnrollmentEntityService.findUserLessonsByUserIdsAndLessonIds(List.of(userId), List.of(lessonId)))
					.thenReturn(List.of());

			// When:
			List<UserLesson> result = service.enrollUsersInLesson(java.util.Arrays.asList(userId, null, userId), lesson,
					enrolledAt, false);

			// Then:
			assertThat(result).hasSize(1);
			verify(learningEnrollmentEntityService)
					.findUserLessonsByUserIdsAndLessonIds(List.of(userId), List.of(lessonId));
			ArgumentCaptor<List<UserLesson>> captor = ArgumentCaptor.forClass(List.class);
			verify(learningEnrollmentEntityService).saveAllUserLessons(captor.capture());
			assertThat(captor.getValue()).hasSize(1);
			assertThat(captor.getValue().get(0).getId()).isEqualTo(id);
		}
	}

	@Nested
	class EnrollUsersInRoadmap {

		@Test
		void shouldReturnEmptyListWhenUserIdsEmptyTest() {
			// Given:
			Long roadmapId = 1L;

			// When:
			List<UserRoadmap> result = service.enrollUsersInRoadmap(List.of(), roadmapId);

			// Then:
			assertThat(result).isEmpty();
			verifyNoInteractions(roadmapEntityService);
		}

		@Test
		void shouldSkipExistingRoadmapEnrollmentsAndCreateNewOnesTest() {
			// Given:
			LearningEnrollmentServiceImpl spy = spy(service);
			Long roadmapId = 1L;
			Long existingUserId = 10L;
			Long newUserId = 20L;
			Roadmap roadmap = new Roadmap();
			roadmap.setId(roadmapId);
			UserRoadmap existing = userRoadmap(existingUserId, roadmapId);
			existing.setRoadmap(roadmap);
			UserRoadmap.UserRoadmapId newId = new UserRoadmap.UserRoadmapId();
			newId.setUserId(newUserId);
			newId.setRoadmapId(roadmapId);
			User newUser = userWithId(newUserId);
			LocalDateTime enrolledAt = LocalDateTime.of(2026, 1, 1, 0, 0);
			when(roadmapEntityService.getReference(roadmapId)).thenReturn(roadmap);
			when(learningEnrollmentEntityService.findUserRoadmapsByUserIdsAndRoadmapId(List.of(existingUserId, newUserId),
					roadmapId)).thenReturn(List.of(existing));
			when(currentTime.utcDateTime()).thenReturn(enrolledAt);
			when(enrollmentSupport.userRoadmapId(newUserId, roadmapId)).thenReturn(newId);
			when(userEntityService.getReference(newUserId)).thenReturn(newUser);
			doNothing().when(spy).fanOutRoadmapLessons(List.of(existingUserId, newUserId), roadmapId, true);

			// When:
			List<UserRoadmap> result = spy.enrollUsersInRoadmap(List.of(existingUserId, newUserId), roadmapId);

			// Then:
			assertThat(result).hasSize(2);
			assertThat(result.get(0)).isSameAs(existing);
			assertThat(result.get(1).getId()).isEqualTo(newId);
			assertThat(result.get(1).getEnrolledAt()).isEqualTo(enrolledAt);
			ArgumentCaptor<List<UserRoadmap>> captor = ArgumentCaptor.forClass(List.class);
			verify(learningEnrollmentEntityService).saveAllUserRoadmaps(captor.capture());
			assertThat(captor.getValue()).hasSize(1);
			assertThat(captor.getValue().get(0).getId()).isEqualTo(newId);
		}
	}

	@Nested
	class FanOutRoadmapLessons {

		@Test
		void shouldReturnEarlyWhenUserIdsEmptyTest() {
			// Given:
			Long roadmapId = 1L;

			// When:
			service.fanOutRoadmapLessons(List.of(), roadmapId, true);

			// Then:
			verifyNoInteractions(roadmapEntityService);
		}

		@Test
		void shouldReturnEarlyWhenNoRoadmapLessonsAreEnrollableTest() {
			// Given:
			Long roadmapId = 1L;
			Long userId = 10L;
			Lesson draftLesson = lessonWithStatus(LessonStatusCode.DRAFT, LessonPublicationStatusCode.PUBLISHED);
			RoadmapLesson roadmapLesson = roadmapLesson(roadmapId, draftLesson, 1);
			when(roadmapEntityService.findByIdRoadmapIdOrderBySortOrderAsc(roadmapId))
					.thenReturn(List.of(roadmapLesson));

			// When:
			service.fanOutRoadmapLessons(List.of(userId), roadmapId, true);

			// Then:
			verify(roadmapEntityService).findByIdRoadmapIdOrderBySortOrderAsc(roadmapId);
			verifyNoInteractions(learningEnrollmentEntityService);
		}

		@Test
		void shouldCreateNewLessonEnrollmentsForEnrollableRoadmapLessonsTest() {
			// Given:
			Long roadmapId = 1L;
			Long userId = 10L;
			Long lessonId = 100L;
			LocalDateTime base = LocalDateTime.of(2026, 1, 1, 12, 0, 0);
			Lesson lesson = lessonWithStatus(LessonStatusCode.READY, LessonPublicationStatusCode.PUBLISHED);
			lesson.setId(lessonId);
			RoadmapLesson roadmapLesson = roadmapLesson(roadmapId, lesson, 5);
			User user = userWithId(userId);
			UserLesson.UserLessonId id = new UserLesson.UserLessonId();
			id.setUserId(userId);
			id.setLessonId(lessonId);
			when(roadmapEntityService.findByIdRoadmapIdOrderBySortOrderAsc(roadmapId))
					.thenReturn(List.of(roadmapLesson));
			when(learningEnrollmentEntityService.findUserLessonsByUserIdsAndLessonIds(List.of(userId), List.of(lessonId)))
					.thenReturn(List.of());
			when(currentTime.utcDateTime()).thenReturn(base);
			when(userEntityService.getReference(userId)).thenReturn(user);
			when(enrollmentSupport.userLessonId(userId, lessonId)).thenReturn(id);

			// When:
			service.fanOutRoadmapLessons(List.of(userId), roadmapId, true);

			// Then:
			ArgumentCaptor<List<UserLesson>> captor = ArgumentCaptor.forClass(List.class);
			verify(learningEnrollmentEntityService).saveAllUserLessons(captor.capture());
			assertThat(captor.getValue()).hasSize(1);
			assertThat(captor.getValue().get(0).getId()).isEqualTo(id);
			assertThat(captor.getValue().get(0).getEnrolledAt()).isEqualTo(base.minusNanos(5_000_000L));
		}

		@Test
		void shouldUpdateExistingEnrollmentWhenFlagSetTrueTest() {
			// Given:
			Long roadmapId = 1L;
			Long userId = 10L;
			Long lessonId = 100L;
			LocalDateTime base = LocalDateTime.of(2026, 1, 1, 12, 0, 0);
			Lesson lesson = lessonWithStatus(LessonStatusCode.READY, LessonPublicationStatusCode.PUBLISHED);
			lesson.setId(lessonId);
			RoadmapLesson roadmapLesson = roadmapLesson(roadmapId, lesson, 3);
			UserLesson existing = userLesson(userId, lessonId);
			existing.setLesson(lesson);
			existing.setEnrolledAt(LocalDateTime.of(2020, 1, 1, 0, 0));
			when(roadmapEntityService.findByIdRoadmapIdOrderBySortOrderAsc(roadmapId))
					.thenReturn(List.of(roadmapLesson));
			when(learningEnrollmentEntityService.findUserLessonsByUserIdsAndLessonIds(List.of(userId), List.of(lessonId)))
					.thenReturn(List.of(existing));
			when(currentTime.utcDateTime()).thenReturn(base);
			when(learningEnrollmentEntityService.saveAllUserLessons(List.of(existing))).thenReturn(List.of(existing));

			// When:
			service.fanOutRoadmapLessons(List.of(userId), roadmapId, true);

			// Then:
			assertThat(existing.getEnrolledAt()).isEqualTo(base.minusNanos(3_000_000L));
			verify(learningEnrollmentEntityService).saveAllUserLessons(List.of(existing));
		}

		@Test
		void shouldSkipExistingEnrollmentWhenFlagSetFalseTest() {
			// Given:
			Long roadmapId = 1L;
			Long userId = 10L;
			Long lessonId = 100L;
			LocalDateTime base = LocalDateTime.of(2026, 1, 1, 12, 0, 0);
			Lesson lesson = lessonWithStatus(LessonStatusCode.READY, LessonPublicationStatusCode.PUBLISHED);
			lesson.setId(lessonId);
			RoadmapLesson roadmapLesson = roadmapLesson(roadmapId, lesson, 3);
			UserLesson existing = userLesson(userId, lessonId);
			existing.setLesson(lesson);
			when(roadmapEntityService.findByIdRoadmapIdOrderBySortOrderAsc(roadmapId))
					.thenReturn(List.of(roadmapLesson));
			when(learningEnrollmentEntityService.findUserLessonsByUserIdsAndLessonIds(List.of(userId), List.of(lessonId)))
					.thenReturn(List.of(existing));
			when(currentTime.utcDateTime()).thenReturn(base);

			// When:
			service.fanOutRoadmapLessons(List.of(userId), roadmapId, false);

			// Then:
			verify(learningEnrollmentEntityService).saveAllUserLessons(List.of());
		}
	}

	@Nested
	class UnenrollUserFromLesson {

		@Test
		void shouldDeleteExistingLessonEnrollmentTest() {
			// Given:
			Long userId = 1L;
			Long lessonId = 10L;
			UserLesson.UserLessonId id = new UserLesson.UserLessonId();
			id.setUserId(userId);
			id.setLessonId(lessonId);
			UserLesson enrollment = userLesson(userId, lessonId);
			when(enrollmentSupport.userLessonId(userId, lessonId)).thenReturn(id);
			when(learningEnrollmentEntityService.findUserLessonById(id)).thenReturn(Optional.of(enrollment));

			// When:
			service.unenrollUserFromLesson(userId, lessonId);

			// Then:
			verify(learningEnrollmentEntityService).delete(enrollment);
		}

		@Test
		void shouldDoNothingWhenLessonEnrollmentMissingTest() {
			// Given:
			Long userId = 1L;
			Long lessonId = 10L;
			UserLesson.UserLessonId id = new UserLesson.UserLessonId();
			id.setUserId(userId);
			id.setLessonId(lessonId);
			UserLesson enrollment = userLesson(userId, lessonId);
			when(enrollmentSupport.userLessonId(userId, lessonId)).thenReturn(id);
			when(learningEnrollmentEntityService.findUserLessonById(id)).thenReturn(Optional.empty());

			// When:
			service.unenrollUserFromLesson(userId, lessonId);

			// Then:
			verify(learningEnrollmentEntityService, never()).delete(enrollment);
		}
	}

	@Nested
	class unenrollUserFromRoadmap {

		@Test
		void shouldBulkDeleteRoadmapAndLessonEnrollmentsForSingleUserTest() {
			// Given:
			Long userId = 1L;
			Long roadmapId = 10L;

			// When:
			service.unenrollUserFromRoadmap(userId, roadmapId);

			// Then:
			verify(learningEnrollmentEntityService).deleteUserRoadmapsByUserIdsAndRoadmapId(List.of(userId),
					roadmapId);
			verify(learningEnrollmentEntityService).deleteRoadmapDerivedLessonEnrollments(List.of(userId), roadmapId);
		}
	}

	@Nested
	class unenrollUsersFromRoadmap {

		@Test
		void shouldBulkDeleteRoadmapAndLessonEnrollmentsForMultipleUsersInOneStatementEachTest() {
			// Given:
			List<Long> userIds = List.of(1L, 2L, 3L);
			Long roadmapId = 10L;

			// When:
			service.unenrollUsersFromRoadmap(userIds, roadmapId);

			// Then:
			verify(learningEnrollmentEntityService).deleteUserRoadmapsByUserIdsAndRoadmapId(userIds, roadmapId);
			verify(learningEnrollmentEntityService).deleteRoadmapDerivedLessonEnrollments(userIds, roadmapId);
		}

		@Test
		void shouldDeduplicateAndDropNullUserIdsBeforeDeletingTest() {
			// Given:
			Long userId = 1L;
			Long roadmapId = 10L;
			List<Long> userIdsWithDuplicateAndNull = java.util.Arrays.asList(userId, null, userId);

			// When:
			service.unenrollUsersFromRoadmap(userIdsWithDuplicateAndNull, roadmapId);

			// Then:
			verify(learningEnrollmentEntityService).deleteUserRoadmapsByUserIdsAndRoadmapId(List.of(userId),
					roadmapId);
			verify(learningEnrollmentEntityService).deleteRoadmapDerivedLessonEnrollments(List.of(userId), roadmapId);
		}

		@Test
		void shouldNotDeleteWhenUserIdsIsEmptyTest() {
			// Given:
			Long roadmapId = 10L;

			// When:
			service.unenrollUsersFromRoadmap(List.of(), roadmapId);

			// Then:
			verify(learningEnrollmentEntityService, never()).deleteUserRoadmapsByUserIdsAndRoadmapId(List.of(),
					roadmapId);
			verify(learningEnrollmentEntityService, never()).deleteRoadmapDerivedLessonEnrollments(List.of(), roadmapId);
		}
	}

	@Nested
	class unenrollUsersFromLesson {

		@Test
		void shouldBulkDeleteLessonEnrollmentsForMultipleUsersInOneStatementTest() {
			// Given:
			List<Long> userIds = List.of(1L, 2L, 3L);
			Long lessonId = 10L;

			// When:
			service.unenrollUsersFromLesson(userIds, lessonId);

			// Then:
			verify(learningEnrollmentEntityService).deleteUserLessonsByUserIdsAndLessonId(userIds, lessonId);
		}

		@Test
		void shouldDeduplicateAndDropNullUserIdsBeforeDeletingTest() {
			// Given:
			Long userId = 1L;
			Long lessonId = 10L;
			List<Long> userIdsWithDuplicateAndNull = java.util.Arrays.asList(userId, null, userId);

			// When:
			service.unenrollUsersFromLesson(userIdsWithDuplicateAndNull, lessonId);

			// Then:
			verify(learningEnrollmentEntityService).deleteUserLessonsByUserIdsAndLessonId(List.of(userId), lessonId);
		}

		@Test
		void shouldNotDeleteWhenUserIdsIsEmptyTest() {
			// Given:
			Long lessonId = 10L;

			// When:
			service.unenrollUsersFromLesson(List.of(), lessonId);

			// Then:
			verify(learningEnrollmentEntityService, never()).deleteUserLessonsByUserIdsAndLessonId(List.of(), lessonId);
		}
	}

	@Nested
	class GetEnrolledLessonIds {

		@Test
		void shouldDelegateToEntityServiceBoundedByGivenLessonIdsTest() {
			// Given:
			Long userId = 1L;
			List<Long> pageLessonIds = List.of(10L, 20L);
			when(learningEnrollmentEntityService.findEnrolledLessonIds(userId, pageLessonIds))
					.thenReturn(java.util.Set.of(10L));

			// When:
			java.util.Set<Long> result = service.getEnrolledLessonIds(userId, pageLessonIds);

			// Then:
			assertThat(result).containsExactly(10L);
			verify(learningEnrollmentEntityService).findEnrolledLessonIds(userId, pageLessonIds);
		}
	}

	@Nested
	class NormalizedUserIds {

		@Test
		void shouldReturnEmptyListForNullOrEmptyInputTest() {
			// When:
			List<Long> nullResult = service.normalizedUserIds(null);
			List<Long> emptyResult = service.normalizedUserIds(List.of());

			// Then:
			assertThat(nullResult).isEmpty();
			assertThat(emptyResult).isEmpty();
		}

		@Test
		void shouldPreserveOrderAndDropDuplicatesAndNullsTest() {
			// When:
			List<Long> result = service.normalizedUserIds(java.util.Arrays.asList(3L, 1L, null, 3L, 2L, 1L));

			// Then:
			assertThat(result).containsExactly(3L, 1L, 2L);
		}
	}

	// -------------------------------------------------------------------------
	// Setup helpers
	// -------------------------------------------------------------------------

	private AppUser viewer() {
		return new AppUser(1L, "clerk-1", "learner@test.com", "Learner", "learner", "Learner", null, null, null);
	}

	private Lesson lessonWithStatus(String statusCode, String publicationStatusCode) {
		Lesson lesson = new Lesson();
		LessonStatus status = new LessonStatus();
		status.setCode(statusCode);
		LessonPublicationStatus publicationStatus = new LessonPublicationStatus();
		publicationStatus.setCode(publicationStatusCode);
		lesson.setStatus(status);
		lesson.setPublicationStatus(publicationStatus);
		return lesson;
	}

	private User userWithId(Long userId) {
		User user = new User();
		user.setId(userId);
		return user;
	}

	private UserLesson userLesson(Long userId, Long lessonId) {
		UserLesson enrollment = new UserLesson();
		UserLesson.UserLessonId id = new UserLesson.UserLessonId();
		id.setUserId(userId);
		id.setLessonId(lessonId);
		enrollment.setId(id);
		enrollment.setEnrolledAt(LocalDateTime.of(2025, 1, 1, 0, 0));
		return enrollment;
	}

	private UserRoadmap userRoadmap(Long userId, Long roadmapId) {
		UserRoadmap enrollment = new UserRoadmap();
		UserRoadmap.UserRoadmapId id = new UserRoadmap.UserRoadmapId();
		id.setUserId(userId);
		id.setRoadmapId(roadmapId);
		enrollment.setId(id);
		enrollment.setEnrolledAt(LocalDateTime.of(2025, 1, 1, 0, 0));
		return enrollment;
	}

	private RoadmapLesson roadmapLesson(Long roadmapId, Lesson lesson, Integer sortOrder) {
		RoadmapLesson roadmapLesson = new RoadmapLesson();
		RoadmapLesson.RoadmapLessonId id = new RoadmapLesson.RoadmapLessonId();
		id.setRoadmapId(roadmapId);
		id.setLessonId(lesson.getId());
		roadmapLesson.setId(id);
		roadmapLesson.setLesson(lesson);
		roadmapLesson.setSortOrder(sortOrder);
		return roadmapLesson;
	}
}
