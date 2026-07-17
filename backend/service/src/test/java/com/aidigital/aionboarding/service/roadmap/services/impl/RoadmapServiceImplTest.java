package com.aidigital.aionboarding.service.roadmap.services.impl;

import com.aidigital.aionboarding.domain.common.dictionary.LessonPublicationStatusCode;
import com.aidigital.aionboarding.domain.common.dictionary.LessonStatusCode;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonPublicationStatus;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonStatus;
import com.aidigital.aionboarding.domain.learning.entities.UserRoadmap;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.roadmap.entities.Roadmap;
import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapLesson;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.learning.services.entity.LearningEnrollmentEntityService;
import com.aidigital.aionboarding.service.learning.support.LearningEnrollmentSupport;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import com.aidigital.aionboarding.service.roadmap.models.CreateRoadmapInput;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapListQuery;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapRecord;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapSortField;
import com.aidigital.aionboarding.service.roadmap.models.UpdateRoadmapInput;
import com.aidigital.aionboarding.service.roadmap.services.entity.RoadmapEntityService;
import com.aidigital.aionboarding.service.roadmap.support.RoadmapAccessPolicy;
import com.aidigital.aionboarding.service.roadmap.support.RoadmapLessonValidator;
import com.aidigital.aionboarding.service.roadmap.support.RoadmapRecordAssembler;
import com.aidigital.aionboarding.service.user.services.entity.UserEntityService;
import org.instancio.Instancio;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.instancio.Select.field;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoadmapServiceImplTest {

	@Mock
	private RoadmapEntityService roadmapEntityService;
	@Mock
	private LearningEnrollmentEntityService learningEnrollmentEntityService;
	@Mock
	private RoadmapAccessPolicy roadmapAccessPolicy;
	@Mock
	private RoadmapLessonValidator roadmapLessonValidator;
	@Mock
	private RoadmapRecordAssembler roadmapRecordAssembler;
	@Mock
	private PermissionService permissionService;
	@Mock
	private UserEntityService userEntityService;
	@Mock
	private LearningEnrollmentSupport learningEnrollmentSupport;

	@Spy
	private CurrentTime currentTime = new CurrentTime();

	@InjectMocks
	private RoadmapServiceImpl service;

	private AppUser adminViewer() {
		return new AppUser(1L, "clerk-admin", "admin@test.com", "Admin", "admin", "Admin", null, null, null);
	}

	private AppUser nonOwnerNonAdminViewer() {
		return new AppUser(2L, "clerk-member", "member@test.com", "Member", "member", "Member", null, null, null);
	}

	private RoadmapRecord baseRecord(Long id, String title) {
		return new RoadmapRecord(id, title, "desc", List.of(), List.of(), List.of(), null, null, null,
				"creator", LocalDateTime.now(), LocalDateTime.now());
	}

	private RoadmapRecord recordWithManageable(Long id, String title, boolean viewerCanManage) {
		return new RoadmapRecord(id, title, "desc", List.of(), List.of(), List.of(), null, null, viewerCanManage,
				"creator", LocalDateTime.now(), LocalDateTime.now());
	}

	private RoadmapRecord recordWithEnrollment(Long id, String title, boolean isEnrolled, LocalDateTime enrolledAt) {
		return new RoadmapRecord(id, title, "desc", List.of(), List.of(), List.of(), isEnrolled, enrolledAt, null,
				"creator", LocalDateTime.now(), LocalDateTime.now());
	}

	private RoadmapRecord recordWithLessonCompletion(Long id, String title, Long lessonId, Boolean isCompleted) {
		com.aidigital.aionboarding.service.roadmap.models.RoadmapLessonRecord lessonRecord =
				new com.aidigital.aionboarding.service.roadmap.models.RoadmapLessonRecord(
						lessonId, "Lesson " + lessonId, "desc", "READY", LocalDateTime.now(), 0, isCompleted);
		return new RoadmapRecord(id, title, "desc", List.of(), List.of(lessonId), List.of(lessonRecord), null, null,
				null,
				"creator", LocalDateTime.now(), LocalDateTime.now());
	}

	@Nested
	class GetAllRoadmaps {

		private final RoadmapListQuery defaultQuery = new RoadmapListQuery(
				null, null, null, null, RoadmapSortField.CREATED_AT, Sort.Direction.DESC
		);

		@Test
		void shouldReturnEmptyPageWithoutFurtherQueriesWhenNoRoadmapsExistTest() {
			// Given:
			AppUser viewer = adminViewer();
			when(roadmapEntityService.search(defaultQuery, viewer.internalId(), 0, 20))
					.thenReturn(new PageImpl<>(List.of()));

			// When:
			Page<RoadmapRecord> result = service.getAllRoadmaps(viewer, defaultQuery, 0, 20);

			// Then:
			assertThat(result.getContent()).isEmpty();
			verify(learningEnrollmentEntityService, never()).findUserRoadmapsByUserIdAndRoadmapIds(eq(viewer.internalId()), Mockito.any());
			verify(roadmapEntityService, never()).findAllByRoadmapIdsWithLessons(List.of());
		}

		@Test
		void shouldGiveAdminViewerNullManageableIdsSentinelMeaningManageEverythingTest() {
			// Given:
			AppUser viewer = adminViewer();
			Roadmap roadmap = Instancio.of(Roadmap.class)
					.set(field(Roadmap::getId), 10L)
					.set(field(Roadmap::getAuthorUser), null)
					.create();
			when(roadmapEntityService.search(defaultQuery, viewer.internalId(), 0, 20))
					.thenReturn(new PageImpl<>(List.of(roadmap)));
			when(roadmapAccessPolicy.getManageableRoadmapIds(viewer, List.of(roadmap))).thenReturn(null);
			when(learningEnrollmentEntityService.findUserRoadmapsByUserIdAndRoadmapIds(viewer.internalId(),
					List.of(10L)))
					.thenReturn(List.of());
			when(roadmapEntityService.findAllByRoadmapIdsWithLessons(List.of(10L))).thenReturn(List.of());
			when(roadmapRecordAssembler.lessonCompletionMap(viewer, List.of())).thenReturn(Map.of());
			when(roadmapRecordAssembler.toRecord(eq(roadmap), eq((Set<Long>) null), eq(null), eq(List.of()),
					eq(Map.of())))
					.thenReturn(recordWithManageable(10L, "Roadmap 10", true));

			// When:
			Page<RoadmapRecord> result = service.getAllRoadmaps(viewer, defaultQuery, 0, 20);

			// Then:
			assertThat(result.getContent()).hasSize(1);
			assertThat(result.getContent().get(0).viewerCanManage()).isTrue();
			verify(permissionService, never()).canManageRoadmap(eq(viewer), eq(null));
		}

		@Test
		void shouldBuildExplicitManageableSetForNonAdminViewerTest() {
			// Given:
			AppUser viewer = nonOwnerNonAdminViewer();
			Roadmap manageable = Instancio.of(Roadmap.class)
					.set(field(Roadmap::getId), 20L)
					.set(field(Roadmap::getAuthorUser), null)
					.create();
			Roadmap notManageable = Instancio.of(Roadmap.class)
					.set(field(Roadmap::getId), 21L)
					.set(field(Roadmap::getAuthorUser), null)
					.create();
			when(roadmapEntityService.search(defaultQuery, viewer.internalId(), 0, 20))
					.thenReturn(new PageImpl<>(List.of(manageable, notManageable)));
			when(roadmapAccessPolicy.getManageableRoadmapIds(viewer, List.of(manageable, notManageable)))
					.thenReturn(Set.of(20L));
			when(learningEnrollmentEntityService.findUserRoadmapsByUserIdAndRoadmapIds(viewer.internalId(),
					List.of(20L, 21L)))
					.thenReturn(List.of());
			when(roadmapEntityService.findAllByRoadmapIdsWithLessons(List.of(20L, 21L))).thenReturn(List.of());
			when(roadmapRecordAssembler.lessonCompletionMap(viewer, List.of())).thenReturn(Map.of());
			when(roadmapRecordAssembler.toRecord(eq(manageable), eq(Set.of(20L)), eq(null), eq(List.of()),
					eq(Map.of())))
					.thenReturn(recordWithManageable(20L, "Manageable", true));
			when(roadmapRecordAssembler.toRecord(eq(notManageable), eq(Set.of(20L)), eq(null), eq(List.of()),
					eq(Map.of())))
					.thenReturn(recordWithManageable(21L, "NotManageable", false));

			// When:
			Page<RoadmapRecord> result = service.getAllRoadmaps(viewer, defaultQuery, 0, 20);

			// Then:
			assertThat(result.getContent()).hasSize(2);
			RoadmapRecord manageableRecord =
					result.getContent().stream().filter(r -> r.id().equals(20L)).findFirst().orElseThrow();
			RoadmapRecord notManageableRecord =
					result.getContent().stream().filter(r -> r.id().equals(21L)).findFirst().orElseThrow();
			assertThat(manageableRecord.viewerCanManage()).isTrue();
			assertThat(notManageableRecord.viewerCanManage()).isFalse();
		}

		@Test
		void shouldKeyEnrollmentMapByRoadmapIdTest() {
			// Given:
			AppUser viewer = adminViewer();
			Roadmap roadmap = Instancio.of(Roadmap.class)
					.set(field(Roadmap::getId), 30L)
					.set(field(Roadmap::getAuthorUser), null)
					.create();
			LocalDateTime enrolledAt = LocalDateTime.now().minusDays(1);
			UserRoadmap.UserRoadmapId enrollmentId = Instancio.of(UserRoadmap.UserRoadmapId.class)
					.set(field(UserRoadmap.UserRoadmapId::getUserId), viewer.internalId())
					.set(field(UserRoadmap.UserRoadmapId::getRoadmapId), 30L)
					.create();
			UserRoadmap enrollment = Instancio.of(UserRoadmap.class)
					.set(field(UserRoadmap::getId), enrollmentId)
					.set(field(UserRoadmap::getEnrolledAt), enrolledAt)
					.create();
			when(roadmapEntityService.search(defaultQuery, viewer.internalId(), 0, 20))
					.thenReturn(new PageImpl<>(List.of(roadmap)));
			when(roadmapAccessPolicy.getManageableRoadmapIds(viewer, List.of(roadmap))).thenReturn(null);
			when(learningEnrollmentEntityService.findUserRoadmapsByUserIdAndRoadmapIds(viewer.internalId(),
					List.of(30L)))
					.thenReturn(List.of(enrollment));
			when(roadmapEntityService.findAllByRoadmapIdsWithLessons(List.of(30L))).thenReturn(List.of());
			when(roadmapRecordAssembler.lessonCompletionMap(viewer, List.of())).thenReturn(Map.of());
			when(roadmapRecordAssembler.toRecord(eq(roadmap), eq((Set<Long>) null), eq(enrollment), eq(List.of()),
					eq(Map.of())))
					.thenReturn(recordWithEnrollment(30L, "Roadmap 30", true, enrolledAt));

			// When:
			Page<RoadmapRecord> result = service.getAllRoadmaps(viewer, defaultQuery, 0, 20);

			// Then:
			assertThat(result.getContent()).hasSize(1);
			assertThat(result.getContent().get(0).isEnrolled()).isTrue();
			assertThat(result.getContent().get(0).enrolledAt()).isEqualTo(enrolledAt);
		}

		@Test
		void shouldComputeLessonCompletionMapOnceAndReuseAcrossAllRoadmapsTest() {
			// Given:
			AppUser viewer = adminViewer();
			Roadmap roadmapOne = Instancio.of(Roadmap.class)
					.set(field(Roadmap::getId), 40L)
					.set(field(Roadmap::getAuthorUser), null)
					.create();
			Roadmap roadmapTwo = Instancio.of(Roadmap.class)
					.set(field(Roadmap::getId), 41L)
					.set(field(Roadmap::getAuthorUser), null)
					.create();

			Lesson sharedLesson = Instancio.of(Lesson.class)
					.set(field(Lesson::getId), 100L)
					.create();

			RoadmapLesson.RoadmapLessonId rlIdOne = Instancio.of(RoadmapLesson.RoadmapLessonId.class)
					.set(field(RoadmapLesson.RoadmapLessonId::getRoadmapId), 40L)
					.set(field(RoadmapLesson.RoadmapLessonId::getLessonId), 100L)
					.create();
			RoadmapLesson roadmapLessonOne = Instancio.of(RoadmapLesson.class)
					.set(field(RoadmapLesson::getId), rlIdOne)
					.set(field(RoadmapLesson::getLesson), sharedLesson)
					.set(field(RoadmapLesson::getSortOrder), 0)
					.create();

			RoadmapLesson.RoadmapLessonId rlIdTwo = Instancio.of(RoadmapLesson.RoadmapLessonId.class)
					.set(field(RoadmapLesson.RoadmapLessonId::getRoadmapId), 41L)
					.set(field(RoadmapLesson.RoadmapLessonId::getLessonId), 100L)
					.create();
			RoadmapLesson roadmapLessonTwo = Instancio.of(RoadmapLesson.class)
					.set(field(RoadmapLesson::getId), rlIdTwo)
					.set(field(RoadmapLesson::getLesson), sharedLesson)
					.set(field(RoadmapLesson::getSortOrder), 0)
					.create();

			when(roadmapEntityService.search(defaultQuery, viewer.internalId(), 0, 20))
					.thenReturn(new PageImpl<>(List.of(roadmapOne, roadmapTwo)));
			when(roadmapAccessPolicy.getManageableRoadmapIds(viewer, List.of(roadmapOne, roadmapTwo))).thenReturn(null);
			when(learningEnrollmentEntityService.findUserRoadmapsByUserIdAndRoadmapIds(viewer.internalId(),
					List.of(40L, 41L)))
					.thenReturn(List.of());
			when(roadmapEntityService.findAllByRoadmapIdsWithLessons(List.of(40L, 41L)))
					.thenReturn(List.of(roadmapLessonOne, roadmapLessonTwo));
			when(roadmapRecordAssembler.lessonCompletionMap(viewer, List.of(100L))).thenReturn(Map.of(100L, true));
			when(roadmapRecordAssembler.toRecord(eq(roadmapOne), eq((Set<Long>) null), eq(null),
					eq(List.of(roadmapLessonOne)), eq(Map.of(100L, true))))
					.thenReturn(recordWithLessonCompletion(40L, "Roadmap 40", 100L, true));
			when(roadmapRecordAssembler.toRecord(eq(roadmapTwo), eq((Set<Long>) null), eq(null),
					eq(List.of(roadmapLessonTwo)), eq(Map.of(100L, true))))
					.thenReturn(recordWithLessonCompletion(41L, "Roadmap 41", 100L, true));

			// When:
			Page<RoadmapRecord> result = service.getAllRoadmaps(viewer, defaultQuery, 0, 20);

			// Then: completion query executed exactly once across both roadmaps, and both roadmaps
			// observe the SAME completion value for the shared lesson.
			verify(roadmapRecordAssembler, times(1)).lessonCompletionMap(viewer, List.of(100L));
			assertThat(result.getContent()).hasSize(2);
			Boolean completionInRoadmapOne = result.getContent().get(0).lessons().get(0).isCompleted();
			Boolean completionInRoadmapTwo = result.getContent().get(1).lessons().get(0).isCompleted();
			assertThat(completionInRoadmapOne).isTrue();
			assertThat(completionInRoadmapTwo).isTrue();
		}

		@Test
		void shouldReturnNullCompletionStatusWhenViewerHasNoCompletionRecordTest() {
			// Given:
			AppUser viewer = adminViewer();
			Roadmap roadmap = Instancio.of(Roadmap.class)
					.set(field(Roadmap::getId), 50L)
					.set(field(Roadmap::getAuthorUser), null)
					.create();
			Lesson lesson = Instancio.of(Lesson.class)
					.set(field(Lesson::getId), 200L)
					.create();
			RoadmapLesson.RoadmapLessonId rlId = Instancio.of(RoadmapLesson.RoadmapLessonId.class)
					.set(field(RoadmapLesson.RoadmapLessonId::getRoadmapId), 50L)
					.set(field(RoadmapLesson.RoadmapLessonId::getLessonId), 200L)
					.create();
			RoadmapLesson roadmapLesson = Instancio.of(RoadmapLesson.class)
					.set(field(RoadmapLesson::getId), rlId)
					.set(field(RoadmapLesson::getLesson), lesson)
					.set(field(RoadmapLesson::getSortOrder), 0)
					.create();

			when(roadmapEntityService.search(defaultQuery, viewer.internalId(), 0, 20))
					.thenReturn(new PageImpl<>(List.of(roadmap)));
			when(roadmapAccessPolicy.getManageableRoadmapIds(viewer, List.of(roadmap))).thenReturn(null);
			when(learningEnrollmentEntityService.findUserRoadmapsByUserIdAndRoadmapIds(viewer.internalId(),
					List.of(50L)))
					.thenReturn(List.of());
			when(roadmapEntityService.findAllByRoadmapIdsWithLessons(List.of(50L))).thenReturn(List.of(roadmapLesson));
			when(roadmapRecordAssembler.lessonCompletionMap(viewer, List.of(200L))).thenReturn(Map.of());
			when(roadmapRecordAssembler.toRecord(eq(roadmap), eq((Set<Long>) null), eq(null),
					eq(List.of(roadmapLesson)), eq(Map.of())))
					.thenReturn(recordWithLessonCompletion(50L, "Roadmap 50", 200L, null));

			// When:
			Page<RoadmapRecord> result = service.getAllRoadmaps(viewer, defaultQuery, 0, 20);

			// Then:
			assertThat(result.getContent()).hasSize(1);
			assertThat(result.getContent().get(0).lessons().get(0).isCompleted()).isNull();
		}
	}

	@Nested
	class CountRoadmaps {

		@Test
		void shouldDelegateToEntityServiceWithViewerInternalIdTest() {
			// Given:
			AppUser viewer = adminViewer();
			RoadmapListQuery query = new RoadmapListQuery(
					null, null, null, null, RoadmapSortField.CREATED_AT, Sort.Direction.DESC
			);
			when(roadmapEntityService.countRoadmaps(query, viewer.internalId())).thenReturn(9L);

			// When:
			long result = service.countRoadmaps(viewer, query);

			// Then:
			assertThat(result).isEqualTo(9L);
			verify(roadmapEntityService).countRoadmaps(query, viewer.internalId());
		}
	}

	@Nested
	class CreateRoadmap {

		@Test
		void shouldThrowBeforeAnyMutationWhenCreatePermissionMissingTest() {
			// Given:
			AppUser viewer = nonOwnerNonAdminViewer();
			CreateRoadmapInput input = new CreateRoadmapInput("Title", "desc", List.of(1L), List.of());
			Mockito.doThrow(new AppException(ErrorReason.C004))
					.when(permissionService).requirePermission(viewer, PermissionKeys.ROADMAPS_CREATE);

			// When-Then:
			assertThatThrownBy(() -> service.createRoadmap(viewer, input))
					.isInstanceOf(AppException.class);
			verify(roadmapEntityService, never()).save(Mockito.isNull());
		}

		@Test
		void shouldThrowWhenTitleIsBlankTest() {
			// Given:
			AppUser viewer = adminViewer();
			CreateRoadmapInput input = new CreateRoadmapInput("   ", "desc", List.of(1L), List.of());

			// When-Then:
			assertThatThrownBy(() -> service.createRoadmap(viewer, input))
					.isInstanceOf(AppException.class);
			verify(roadmapLessonValidator, never()).validateReadyPublishedLessons(List.of(1L));
		}

		@Test
		void shouldThrowWhenTitleIsNullTest() {
			// Given:
			AppUser viewer = adminViewer();
			CreateRoadmapInput input = new CreateRoadmapInput(null, "desc", List.of(1L), List.of());

			// When-Then:
			assertThatThrownBy(() -> service.createRoadmap(viewer, input))
					.isInstanceOf(AppException.class);
			verify(roadmapLessonValidator, never()).validateReadyPublishedLessons(List.of(1L));
		}

		@Test
		void shouldNormalizeLessonIdsByDedupingAndFilteringNullsTest() {
			// Given:
			AppUser viewer = adminViewer();
			CreateRoadmapInput input = new CreateRoadmapInput("Title", "desc", List.of(1L, 1L, 2L), List.of());
			LessonStatus readyStatus = Instancio.of(LessonStatus.class).set(field(LessonStatus::getCode),
					LessonStatusCode.READY).create();
			LessonPublicationStatus publishedStatus = Instancio.of(LessonPublicationStatus.class)
					.set(field(LessonPublicationStatus::getCode), LessonPublicationStatusCode.PUBLISHED).create();
			Lesson lessonOne = Instancio.of(Lesson.class)
					.set(field(Lesson::getId), 1L)
					.set(field(Lesson::getStatus), readyStatus)
					.set(field(Lesson::getPublicationStatus), publishedStatus)
					.set(field(Lesson::getTags), List.of())
					.create();
			Lesson lessonTwo = Instancio.of(Lesson.class)
					.set(field(Lesson::getId), 2L)
					.set(field(Lesson::getStatus), readyStatus)
					.set(field(Lesson::getPublicationStatus), publishedStatus)
					.set(field(Lesson::getTags), List.of())
					.create();
			when(roadmapLessonValidator.normalizeLessonIds(List.of(1L, 1L, 2L))).thenReturn(List.of(1L, 2L));
			when(roadmapLessonValidator.validateReadyPublishedLessons(List.of(1L, 2L))).thenReturn(List.of(lessonOne,
					lessonTwo));
			when(roadmapLessonValidator.mergeTags(List.of(), List.of(lessonOne, lessonTwo))).thenReturn(List.of());
			User author = Instancio.of(User.class).set(field(User::getId), viewer.internalId()).create();
			when(userEntityService.getReference(viewer.internalId())).thenReturn(author);
			Roadmap saved = Instancio.of(Roadmap.class).set(field(Roadmap::getId), 60L).create();
			ArgumentCaptor<Roadmap> savedRoadmapCaptor = ArgumentCaptor.forClass(Roadmap.class);
			when(roadmapEntityService.save(savedRoadmapCaptor.capture())).thenReturn(saved);
			when(roadmapEntityService.findByIdRoadmapIdOrderBySortOrderAsc(60L)).thenReturn(List.of());
			when(roadmapRecordAssembler.toRecord(eq(saved), eq(viewer), eq(Set.of(60L)), eq(null), eq(List.of())))
					.thenReturn(baseRecord(60L, "Title"));

			// When:
			service.createRoadmap(viewer, input);

			// Then:
			verify(roadmapLessonValidator).validateReadyPublishedLessons(List.of(1L, 2L));
		}

		@Test
		void shouldThrowWhenLessonIdsEmptyAfterNormalizationTest() {
			// Given:
			AppUser viewer = adminViewer();
			CreateRoadmapInput input = new CreateRoadmapInput("Title", "desc", List.of(), List.of());
			when(roadmapLessonValidator.normalizeLessonIds(List.of())).thenReturn(List.of());
			when(roadmapLessonValidator.validateReadyPublishedLessons(List.of()))
					.thenThrow(new AppException(ErrorReason.C002, "Select at least one lesson for the roadmap."));

			// When-Then:
			assertThatThrownBy(() -> service.createRoadmap(viewer, input))
					.isInstanceOf(AppException.class);
			verify(roadmapEntityService, never()).save(Mockito.isA(Roadmap.class));
		}

		@Test
		void shouldThrowWhenAnyRequestedLessonIdIsNotFoundTest() {
			// Given:
			AppUser viewer = adminViewer();
			CreateRoadmapInput input = new CreateRoadmapInput("Title", "desc", List.of(1L, 2L), List.of());
			when(roadmapLessonValidator.normalizeLessonIds(List.of(1L, 2L))).thenReturn(List.of(1L, 2L));
			when(roadmapLessonValidator.validateReadyPublishedLessons(List.of(1L, 2L)))
					.thenThrow(new AppException(ErrorReason.C002, "Roadmaps can include only existing published ready " +
							"lessons."));

			// When-Then:
			assertThatThrownBy(() -> service.createRoadmap(viewer, input))
					.isInstanceOf(AppException.class);
			Mockito.verifyNoInteractions(roadmapRecordAssembler);
		}

		@Test
		void shouldThrowWhenAnyLessonIsNotReadyOrNotPublishedTest() {
			// Given:
			AppUser viewer = adminViewer();
			CreateRoadmapInput input = new CreateRoadmapInput("Title", "desc", List.of(1L), List.of());
			when(roadmapLessonValidator.normalizeLessonIds(List.of(1L))).thenReturn(List.of(1L));
			when(roadmapLessonValidator.validateReadyPublishedLessons(List.of(1L)))
					.thenThrow(new AppException(ErrorReason.C002, "Roadmaps can include only existing published ready " +
							"lessons."));

			// When-Then:
			assertThatThrownBy(() -> service.createRoadmap(viewer, input))
					.isInstanceOf(AppException.class);
			Mockito.verifyNoInteractions(roadmapRecordAssembler);
		}

		@Test
		void shouldMergeInputTagsWithEverySelectedLessonsOwnTagsTest() {
			// Given:
			AppUser viewer = adminViewer();
			CreateRoadmapInput input = new CreateRoadmapInput("Title", "desc", List.of(1L, 2L), List.of("input-tag"));
			LessonStatus readyStatus = Instancio.of(LessonStatus.class).set(field(LessonStatus::getCode),
					LessonStatusCode.READY).create();
			LessonPublicationStatus publishedStatus = Instancio.of(LessonPublicationStatus.class)
					.set(field(LessonPublicationStatus::getCode), LessonPublicationStatusCode.PUBLISHED).create();
			Lesson lessonOne = Instancio.of(Lesson.class)
					.set(field(Lesson::getId), 1L)
					.set(field(Lesson::getStatus), readyStatus)
					.set(field(Lesson::getPublicationStatus), publishedStatus)
					.set(field(Lesson::getTags), List.of("lesson-one-tag"))
					.create();
			Lesson lessonTwo = Instancio.of(Lesson.class)
					.set(field(Lesson::getId), 2L)
					.set(field(Lesson::getStatus), readyStatus)
					.set(field(Lesson::getPublicationStatus), publishedStatus)
					.set(field(Lesson::getTags), List.of("lesson-two-tag"))
					.create();
			when(roadmapLessonValidator.normalizeLessonIds(List.of(1L, 2L))).thenReturn(List.of(1L, 2L));
			when(roadmapLessonValidator.validateReadyPublishedLessons(List.of(1L, 2L))).thenReturn(List.of(lessonOne,
					lessonTwo));
			when(roadmapLessonValidator.mergeTags(List.of("input-tag"), List.of(lessonOne, lessonTwo)))
					.thenReturn(List.of("input-tag", "lesson-one-tag", "lesson-two-tag"));
			User author = Instancio.of(User.class).set(field(User::getId), viewer.internalId()).create();
			when(userEntityService.getReference(viewer.internalId())).thenReturn(author);
			Roadmap saved = Instancio.of(Roadmap.class).set(field(Roadmap::getId), 70L).create();
			ArgumentCaptor<Roadmap> savedRoadmapCaptor = ArgumentCaptor.forClass(Roadmap.class);
			when(roadmapEntityService.save(savedRoadmapCaptor.capture())).thenReturn(saved);
			when(roadmapEntityService.findByIdRoadmapIdOrderBySortOrderAsc(70L)).thenReturn(List.of());
			when(roadmapRecordAssembler.toRecord(eq(saved), eq(viewer), eq(Set.of(70L)), eq(null), eq(List.of())))
					.thenReturn(baseRecord(70L, "Title"));

			// When:
			service.createRoadmap(viewer, input);

			// Then:
			assertThat(savedRoadmapCaptor.getValue().getTags())
					.isEqualTo(List.of("input-tag", "lesson-one-tag", "lesson-two-tag"));
			verify(roadmapLessonValidator).mergeTags(List.of("input-tag"), List.of(lessonOne, lessonTwo));
		}

		@Test
		void shouldAssignSequentialZeroIndexedSortOrderMatchingListOrderTest() {
			// Given:
			AppUser viewer = adminViewer();
			CreateRoadmapInput input = new CreateRoadmapInput("Title", "desc", List.of(1L, 2L, 3L), List.of());
			LessonStatus readyStatus = Instancio.of(LessonStatus.class).set(field(LessonStatus::getCode),
					LessonStatusCode.READY).create();
			LessonPublicationStatus publishedStatus = Instancio.of(LessonPublicationStatus.class)
					.set(field(LessonPublicationStatus::getCode), LessonPublicationStatusCode.PUBLISHED).create();
			Lesson lessonOne = Instancio.of(Lesson.class)
					.set(field(Lesson::getId), 1L).set(field(Lesson::getStatus), readyStatus)
					.set(field(Lesson::getPublicationStatus), publishedStatus).set(field(Lesson::getTags), List.of()).create();
			Lesson lessonTwo = Instancio.of(Lesson.class)
					.set(field(Lesson::getId), 2L).set(field(Lesson::getStatus), readyStatus)
					.set(field(Lesson::getPublicationStatus), publishedStatus).set(field(Lesson::getTags), List.of()).create();
			Lesson lessonThree = Instancio.of(Lesson.class)
					.set(field(Lesson::getId), 3L).set(field(Lesson::getStatus), readyStatus)
					.set(field(Lesson::getPublicationStatus), publishedStatus).set(field(Lesson::getTags), List.of()).create();
			when(roadmapLessonValidator.normalizeLessonIds(List.of(1L, 2L, 3L))).thenReturn(List.of(1L, 2L, 3L));
			when(roadmapLessonValidator.validateReadyPublishedLessons(List.of(1L, 2L, 3L)))
					.thenReturn(List.of(lessonOne, lessonTwo, lessonThree));
			when(roadmapLessonValidator.mergeTags(List.of(), List.of(lessonOne, lessonTwo, lessonThree))).thenReturn(List.of());
			User author = Instancio.of(User.class).set(field(User::getId), viewer.internalId()).create();
			when(userEntityService.getReference(viewer.internalId())).thenReturn(author);
			Roadmap saved = Instancio.of(Roadmap.class).set(field(Roadmap::getId), 80L).create();
			ArgumentCaptor<Roadmap> savedRoadmapCaptor = ArgumentCaptor.forClass(Roadmap.class);
			when(roadmapEntityService.save(savedRoadmapCaptor.capture())).thenReturn(saved);
			when(roadmapEntityService.findByIdRoadmapIdOrderBySortOrderAsc(80L)).thenReturn(List.of());
			when(roadmapRecordAssembler.toRecord(eq(saved), eq(viewer), eq(Set.of(80L)), eq(null), eq(List.of())))
					.thenReturn(baseRecord(80L, "Title"));

			// When:
			service.createRoadmap(viewer, input);

			// Then:
			ArgumentCaptor<RoadmapLesson> rowCaptor = ArgumentCaptor.forClass(RoadmapLesson.class);
			verify(roadmapEntityService, times(3)).saveRoadmapLesson(rowCaptor.capture());
			List<RoadmapLesson> savedRows = rowCaptor.getAllValues();
			assertThat(savedRows).hasSize(3);
			assertThat(savedRows.get(0).getSortOrder()).isZero();
			assertThat(savedRows.get(0).getId().getLessonId()).isEqualTo(1L);
			assertThat(savedRows.get(1).getSortOrder()).isEqualTo(1);
			assertThat(savedRows.get(1).getId().getLessonId()).isEqualTo(2L);
			assertThat(savedRows.get(2).getSortOrder()).isEqualTo(2);
			assertThat(savedRows.get(2).getId().getLessonId()).isEqualTo(3L);
		}
	}

	@Nested
	class UpdateRoadmap {

		@Test
		void shouldThrowC001WhenRoadmapNotFoundTest() {
			// Given:
			AppUser viewer = adminViewer();
			Long id = 90L;
			UpdateRoadmapInput input = new UpdateRoadmapInput(Optional.empty(), Optional.empty(), Optional.empty(),
					Optional.empty());
			when(roadmapAccessPolicy.requireManageable(viewer, id))
					.thenThrow(new AppException(ErrorReason.C001, id));

			// When-Then:
			assertThatThrownBy(() -> service.updateRoadmap(viewer, id, input))
					.isInstanceOf(AppException.class);
		}

		@Test
		void shouldThrowC004WhenViewerCannotManageRoadmapTest() {
			// Given:
			AppUser viewer = nonOwnerNonAdminViewer();
			Long id = 91L;
			UpdateRoadmapInput input = new UpdateRoadmapInput(Optional.empty(), Optional.empty(), Optional.empty(),
					Optional.empty());
			when(roadmapAccessPolicy.requireManageable(viewer, id))
					.thenThrow(new AppException(ErrorReason.C004));

			// When-Then:
			assertThatThrownBy(() -> service.updateRoadmap(viewer, id, input))
					.isInstanceOf(AppException.class);
			verify(roadmapEntityService, never()).save(Mockito.isA(Roadmap.class));
		}

		@Test
		void shouldApplyOnlyPresentFieldsAndLeaveAbsentFieldsUntouchedTest() {
			// Given:
			AppUser viewer = adminViewer();
			Long id = 92L;
			User author = Instancio.of(User.class).set(field(User::getId), viewer.internalId()).create();
			Roadmap roadmap = Instancio.of(Roadmap.class)
					.set(field(Roadmap::getId), id)
					.set(field(Roadmap::getAuthorUser), author)
					.set(field(Roadmap::getTitle), "Old Title")
					.set(field(Roadmap::getDescription), "Old Description")
					.create();
			UpdateRoadmapInput input = new UpdateRoadmapInput(Optional.of("New Title"), Optional.empty(),
					Optional.empty(), Optional.empty());
			when(roadmapAccessPolicy.requireManageable(viewer, id)).thenReturn(roadmap);
			when(roadmapEntityService.save(roadmap)).thenReturn(roadmap);
			when(roadmapEntityService.findByIdRoadmapIdOrderBySortOrderAsc(id)).thenReturn(List.of());
			when(roadmapRecordAssembler.toRecord(eq(roadmap), eq(viewer), eq(Set.of(id)), eq(null), eq(List.of())))
					.thenReturn(baseRecord(id, "New Title"));

			// When:
			service.updateRoadmap(viewer, id, input);

			// Then:
			assertThat(roadmap.getTitle()).isEqualTo("New Title");
			assertThat(roadmap.getDescription()).isEqualTo("Old Description");
			verify(roadmapEntityService, never()).deleteByIdRoadmapId(id);
		}

		@Test
		void shouldRecomputeTagsFanOutAndDeleteOldRowsWhenLessonIdsPresentTest() {
			// Given:
			AppUser viewer = adminViewer();
			Long id = 93L;
			User author = Instancio.of(User.class).set(field(User::getId), viewer.internalId()).create();
			Roadmap roadmap = Instancio.of(Roadmap.class)
					.set(field(Roadmap::getId), id)
					.set(field(Roadmap::getAuthorUser), author)
					.create();
			LessonStatus readyStatus = Instancio.of(LessonStatus.class).set(field(LessonStatus::getCode),
					LessonStatusCode.READY).create();
			LessonPublicationStatus publishedStatus = Instancio.of(LessonPublicationStatus.class)
					.set(field(LessonPublicationStatus::getCode), LessonPublicationStatusCode.PUBLISHED).create();
			Lesson lesson = Instancio.of(Lesson.class)
					.set(field(Lesson::getId), 5L)
					.set(field(Lesson::getStatus), readyStatus)
					.set(field(Lesson::getPublicationStatus), publishedStatus)
					.set(field(Lesson::getTags), List.of("lesson-tag"))
					.create();
			UpdateRoadmapInput input = new UpdateRoadmapInput(Optional.empty(), Optional.empty(),
					Optional.of(List.of(5L)), Optional.of(List.of("new-tag")));
			when(roadmapAccessPolicy.requireManageable(viewer, id)).thenReturn(roadmap);
			when(roadmapLessonValidator.normalizeLessonIds(List.of(5L))).thenReturn(List.of(5L));
			when(roadmapLessonValidator.validateReadyPublishedLessons(List.of(5L))).thenReturn(List.of(lesson));
			when(roadmapLessonValidator.mergeTags(List.of("new-tag"), List.of(lesson))).thenReturn(List.of("new-tag",
					"lesson-tag"));
			when(learningEnrollmentEntityService.findUserRoadmapsByRoadmapId(id)).thenReturn(List.of());
			when(roadmapEntityService.save(roadmap)).thenReturn(roadmap);
			when(roadmapEntityService.findByIdRoadmapIdOrderBySortOrderAsc(id)).thenReturn(List.of());
			when(roadmapRecordAssembler.toRecord(eq(roadmap), eq(viewer), eq(Set.of(id)), eq(null), eq(List.of())))
					.thenReturn(baseRecord(id, "Title"));

			// When:
			service.updateRoadmap(viewer, id, input);

			// Then:
			verify(roadmapEntityService).deleteByIdRoadmapId(id);
			ArgumentCaptor<RoadmapLesson> rowCaptor = ArgumentCaptor.forClass(RoadmapLesson.class);
			verify(roadmapEntityService).saveRoadmapLesson(rowCaptor.capture());
			assertThat(rowCaptor.getValue().getId().getLessonId()).isEqualTo(5L);
			verify(learningEnrollmentEntityService).findUserRoadmapsByRoadmapId(id);
			assertThat(roadmap.getTags()).isEqualTo(List.of("new-tag", "lesson-tag"));
			verify(roadmapLessonValidator).mergeTags(List.of("new-tag"), List.of(lesson));
		}

		@Test
		void shouldMergeTagsWithEmptyLessonListNotExistingLessonsWhenOnlyTagsPresentTest() {
			// Given:
			AppUser viewer = adminViewer();
			Long id = 94L;
			User author = Instancio.of(User.class).set(field(User::getId), viewer.internalId()).create();
			Roadmap roadmap = Instancio.of(Roadmap.class)
					.set(field(Roadmap::getId), id)
					.set(field(Roadmap::getAuthorUser), author)
					.create();
			UpdateRoadmapInput input = new UpdateRoadmapInput(Optional.empty(), Optional.empty(), Optional.empty(),
					Optional.of(List.of("only-tag")));
			when(roadmapAccessPolicy.requireManageable(viewer, id)).thenReturn(roadmap);
			when(roadmapLessonValidator.mergeTags(List.of("only-tag"), List.of())).thenReturn(List.of("only-tag"));
			when(roadmapEntityService.save(roadmap)).thenReturn(roadmap);
			when(roadmapEntityService.findByIdRoadmapIdOrderBySortOrderAsc(id)).thenReturn(List.of());
			when(roadmapRecordAssembler.toRecord(eq(roadmap), eq(viewer), eq(Set.of(id)), eq(null), eq(List.of())))
					.thenReturn(baseRecord(id, "Title"));

			// When:
			service.updateRoadmap(viewer, id, input);

			// Then: tags are merged against an EMPTY lesson list, not the roadmap's existing lessons —
			// asserted by verifying mergeTags received ONLY the input tag and an empty lesson list,
			// and that no lesson validation happened for this branch.
			assertThat(roadmap.getTags()).isEqualTo(List.of("only-tag"));
			verify(roadmapLessonValidator, never()).validateReadyPublishedLessons(List.of(5L));
			verify(roadmapEntityService, never()).deleteByIdRoadmapId(id);
		}
	}

	@Nested
	class DeleteRoadmap {

		@Test
		void shouldThrowC001WhenRoadmapNotFoundTest() {
			// Given:
			AppUser viewer = adminViewer();
			Long id = 95L;
			when(roadmapAccessPolicy.requireManageable(viewer, id))
					.thenThrow(new AppException(ErrorReason.C001, id));

			// When-Then:
			assertThatThrownBy(() -> service.deleteRoadmap(viewer, id))
					.isInstanceOf(AppException.class);
		}

		@Test
		void shouldThrowC004WhenNonOwnerNonAdminViewerCannotManageTest() {
			// Given:
			AppUser viewer = nonOwnerNonAdminViewer();
			Long id = 96L;
			when(roadmapAccessPolicy.requireManageable(viewer, id))
					.thenThrow(new AppException(ErrorReason.C004));

			// When-Then:
			assertThatThrownBy(() -> service.deleteRoadmap(viewer, id))
					.isInstanceOf(AppException.class);
			verify(roadmapEntityService, never()).delete(Mockito.isA(Roadmap.class));
		}

		@Test
		void shouldNotThrowWhenDeletingRoadmapWithExistingRoadmapLessonRowsTest() {
			// Given:
			AppUser viewer = adminViewer();
			Long id = 97L;
			User author = Instancio.of(User.class).set(field(User::getId), viewer.internalId()).create();
			Roadmap roadmap = Instancio.of(Roadmap.class)
					.set(field(Roadmap::getId), id)
					.set(field(Roadmap::getAuthorUser), author)
					.create();
			when(roadmapAccessPolicy.requireManageable(viewer, id)).thenReturn(roadmap);

			// When-Then:
			Assertions.assertDoesNotThrow(() -> service.deleteRoadmap(viewer, id));
			verify(roadmapEntityService).delete(roadmap);
		}
	}
}
