package com.aidigital.aionboarding.service.learning.services.entity;

import com.aidigital.aionboarding.domain.learning.entities.UserLesson;
import com.aidigital.aionboarding.domain.learning.entities.UserRoadmap;
import com.aidigital.aionboarding.domain.learning.repositories.UserLessonRepository;
import com.aidigital.aionboarding.domain.learning.repositories.UserRoadmapRepository;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.Select.field;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningEnrollmentEntityServiceTest {

	@Mock
	private UserLessonRepository userLessonRepository;
	@Mock
	private UserRoadmapRepository userRoadmapRepository;

	@InjectMocks
	private LearningEnrollmentEntityService learningEnrollmentEntityService;

	@Test
	void findUserLessonByIdShouldReturnRepositoryResultTest() {
		// Given:
		UserLesson.UserLessonId id = Instancio.of(UserLesson.UserLessonId.class)
				.set(field(UserLesson.UserLessonId::getUserId), 1L)
				.set(field(UserLesson.UserLessonId::getLessonId), 2L)
				.create();
		UserLesson userLesson = Instancio.create(UserLesson.class);
		when(userLessonRepository.findById(id)).thenReturn(Optional.of(userLesson));

		// When:
		Optional<UserLesson> result = learningEnrollmentEntityService.findUserLessonById(id);

		// Then:
		assertThat(result).contains(userLesson);
	}

	@Test
	void existsUserLessonByIdShouldReturnRepositoryResultTest() {
		// Given:
		UserLesson.UserLessonId id = Instancio.of(UserLesson.UserLessonId.class)
				.set(field(UserLesson.UserLessonId::getUserId), 1L)
				.set(field(UserLesson.UserLessonId::getLessonId), 2L)
				.create();
		when(userLessonRepository.existsById(id)).thenReturn(true);

		// When:
		boolean result = learningEnrollmentEntityService.existsUserLessonById(id);

		// Then:
		assertThat(result).isTrue();
	}

	@Test
	void findUserLessonsByUserIdShouldReturnRepositoryResultTest() {
		// Given:
		Long userId = 3L;
		UserLesson userLesson = Instancio.create(UserLesson.class);
		when(userLessonRepository.findByUserId(userId)).thenReturn(List.of(userLesson));

		// When:
		List<UserLesson> result = learningEnrollmentEntityService.findUserLessonsByUserId(userId);

		// Then:
		assertThat(result).containsExactly(userLesson);
	}

	@Test
	void findByUserIdAndLessonIdInShouldReturnRepositoryResultTest() {
		// Given:
		Long userId = 4L;
		List<Long> lessonIds = List.of(10L, 11L);
		UserLesson userLesson = Instancio.create(UserLesson.class);
		when(userLessonRepository.findByUserIdAndLessonIdIn(userId, lessonIds)).thenReturn(List.of(userLesson));

		// When:
		List<UserLesson> result = learningEnrollmentEntityService.findByUserIdAndLessonIdIn(userId, lessonIds);

		// Then:
		assertThat(result).containsExactly(userLesson);
	}

	@Test
	void findMyLessonsPageShouldReturnRepositoryResultTest() {
		// Given:
		Long userId = 5L;
		org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 20);
		com.aidigital.aionboarding.domain.learning.repositories.MyLessonSummaryProjection summary =
				new com.aidigital.aionboarding.domain.learning.repositories.MyLessonSummaryProjection(
						1L, "Title", "Description", "ready", "published",
						"", "", "", "", "",
						List.of(), "Author", null, null, null, null
				);
		org.springframework.data.domain.Page<com.aidigital.aionboarding.domain.learning.repositories.MyLessonSummaryProjection> page =
				new org.springframework.data.domain.PageImpl<>(List.of(summary), pageable, 1);
		when(userLessonRepository.findMyLessonsPage(userId, pageable)).thenReturn(page);

		// When:
		var result = learningEnrollmentEntityService.findMyLessonsPage(userId, pageable);

		// Then:
		assertThat(result.getContent()).containsExactly(summary);
	}

	@Test
	void saveUserLessonShouldPersistAndReturnSavedEntityTest() {
		// Given:
		UserLesson userLesson = Instancio.create(UserLesson.class);
		when(userLessonRepository.save(userLesson)).thenReturn(userLesson);

		// When:
		UserLesson result = learningEnrollmentEntityService.save(userLesson);

		// Then:
		assertThat(result).isSameAs(userLesson);
		verify(userLessonRepository, times(1)).save(userLesson);
	}

	@Test
	void deleteUserLessonShouldDelegateToRepositoryTest() {
		// Given:
		UserLesson userLesson = Instancio.create(UserLesson.class);

		// When:
		learningEnrollmentEntityService.delete(userLesson);

		// Then:
		verify(userLessonRepository, times(1)).delete(userLesson);
	}

	@Test
	void deleteRoadmapDerivedLessonEnrollmentsShouldReturnRepositoryDeleteCountTest() {
		// Given:
		List<Long> userIds = List.of(1L, 2L);
		Long roadmapId = 10L;
		when(userLessonRepository.deleteRoadmapDerivedLessonEnrollments(userIds, roadmapId)).thenReturn(3);

		// When:
		int deletedCount = learningEnrollmentEntityService.deleteRoadmapDerivedLessonEnrollments(userIds, roadmapId);

		// Then:
		assertThat(deletedCount).isEqualTo(3);
	}

	@Test
	void deleteRoadmapDerivedLessonEnrollmentsShouldNotQueryRepositoryWhenUserIdsEmptyTest() {
		// When:
		int deletedCount = learningEnrollmentEntityService.deleteRoadmapDerivedLessonEnrollments(List.of(), 10L);

		// Then:
		assertThat(deletedCount).isZero();
		verify(userLessonRepository, times(0)).deleteRoadmapDerivedLessonEnrollments(any(), any());
	}

	@Test
	void findUserRoadmapByIdShouldReturnRepositoryResultTest() {
		// Given:
		UserRoadmap.UserRoadmapId id = Instancio.of(UserRoadmap.UserRoadmapId.class)
				.set(field(UserRoadmap.UserRoadmapId::getUserId), 1L)
				.set(field(UserRoadmap.UserRoadmapId::getRoadmapId), 6L)
				.create();
		UserRoadmap userRoadmap = Instancio.create(UserRoadmap.class);
		when(userRoadmapRepository.findById(id)).thenReturn(Optional.of(userRoadmap));

		// When:
		Optional<UserRoadmap> result = learningEnrollmentEntityService.findUserRoadmapById(id);

		// Then:
		assertThat(result).contains(userRoadmap);
	}

	@Test
	void findUserRoadmapsByUserIdShouldReturnRepositoryResultTest() {
		// Given:
		Long userId = 7L;
		UserRoadmap userRoadmap = Instancio.create(UserRoadmap.class);
		when(userRoadmapRepository.findByIdUserId(userId)).thenReturn(List.of(userRoadmap));

		// When:
		List<UserRoadmap> result = learningEnrollmentEntityService.findUserRoadmapsByUserId(userId);

		// Then:
		assertThat(result).containsExactly(userRoadmap);
	}

	@Test
	void findUserRoadmapsByRoadmapIdShouldReturnRepositoryResultTest() {
		// Given:
		Long roadmapId = 8L;
		UserRoadmap userRoadmap = Instancio.create(UserRoadmap.class);
		when(userRoadmapRepository.findByIdRoadmapId(roadmapId)).thenReturn(List.of(userRoadmap));

		// When:
		List<UserRoadmap> result = learningEnrollmentEntityService.findUserRoadmapsByRoadmapId(roadmapId);

		// Then:
		assertThat(result).containsExactly(userRoadmap);
	}

	@Test
	void saveUserRoadmapShouldPersistAndReturnSavedEntityTest() {
		// Given:
		UserRoadmap userRoadmap = Instancio.create(UserRoadmap.class);
		when(userRoadmapRepository.save(userRoadmap)).thenReturn(userRoadmap);

		// When:
		UserRoadmap result = learningEnrollmentEntityService.save(userRoadmap);

		// Then:
		assertThat(result).isSameAs(userRoadmap);
		verify(userRoadmapRepository, times(1)).save(userRoadmap);
	}

	@Test
	void deleteUserRoadmapShouldDelegateToRepositoryTest() {
		// Given:
		UserRoadmap userRoadmap = Instancio.create(UserRoadmap.class);

		// When:
		learningEnrollmentEntityService.delete(userRoadmap);

		// Then:
		verify(userRoadmapRepository, times(1)).delete(userRoadmap);
	}

	@Test
	void deleteUserRoadmapsByUserIdsAndRoadmapIdShouldReturnRepositoryDeleteCountTest() {
		// Given:
		List<Long> userIds = List.of(1L, 2L);
		Long roadmapId = 10L;
		when(userRoadmapRepository.deleteByUserIdsAndRoadmapId(userIds, roadmapId)).thenReturn(2);

		// When:
		int deletedCount = learningEnrollmentEntityService.deleteUserRoadmapsByUserIdsAndRoadmapId(userIds, roadmapId);

		// Then:
		assertThat(deletedCount).isEqualTo(2);
	}

	@Test
	void deleteUserRoadmapsByUserIdsAndRoadmapIdShouldNotQueryRepositoryWhenUserIdsEmptyTest() {
		// When:
		int deletedCount = learningEnrollmentEntityService.deleteUserRoadmapsByUserIdsAndRoadmapId(List.of(), 10L);

		// Then:
		assertThat(deletedCount).isZero();
		verify(userRoadmapRepository, times(0)).deleteByUserIdsAndRoadmapId(any(), any());
	}

	@Test
	void findEnrolledLessonIdsShouldReturnDistinctLessonIdsFromUserLessonsInGivenPageTest() {
		// Given:
		Long userId = 9L;
		List<Long> pageLessonIds = List.of(20L, 21L);
		UserLesson.UserLessonId lessonId1 = Instancio.of(UserLesson.UserLessonId.class)
				.set(field(UserLesson.UserLessonId::getUserId), userId)
				.set(field(UserLesson.UserLessonId::getLessonId), 20L)
				.create();
		UserLesson.UserLessonId lessonId2 = Instancio.of(UserLesson.UserLessonId.class)
				.set(field(UserLesson.UserLessonId::getUserId), userId)
				.set(field(UserLesson.UserLessonId::getLessonId), 21L)
				.create();
		UserLesson userLesson1 = Instancio.of(UserLesson.class)
				.set(field(UserLesson::getId), lessonId1)
				.create();
		UserLesson userLesson2 = Instancio.of(UserLesson.class)
				.set(field(UserLesson::getId), lessonId2)
				.create();
		when(userLessonRepository.findByUserIdAndLessonIdIn(userId, pageLessonIds))
				.thenReturn(List.of(userLesson1, userLesson2));

		// When:
		Set<Long> result = learningEnrollmentEntityService.findEnrolledLessonIds(userId, pageLessonIds);

		// Then:
		assertThat(result).containsExactlyInAnyOrder(20L, 21L);
	}

	@Test
	void findEnrolledLessonIdsShouldNotQueryRepositoryWhenLessonIdsEmptyTest() {
		// When:
		Set<Long> result = learningEnrollmentEntityService.findEnrolledLessonIds(9L, List.of());

		// Then:
		assertThat(result).isEmpty();
		verify(userLessonRepository, times(0)).findByUserIdAndLessonIdIn(any(), any());
	}
}
