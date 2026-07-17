package com.aidigital.aionboarding.service.learning.services.entity;

import com.aidigital.aionboarding.domain.learning.entities.UserLesson;
import com.aidigital.aionboarding.domain.learning.entities.UserRoadmap;
import com.aidigital.aionboarding.domain.learning.repositories.CompletedRoadmapProjection;
import com.aidigital.aionboarding.domain.learning.repositories.MyLessonSummaryProjection;
import com.aidigital.aionboarding.domain.learning.repositories.UserLessonRepository;
import com.aidigital.aionboarding.domain.learning.repositories.UserRoadmapRepository;
import com.aidigital.aionboarding.service.learning.models.CompletedRoadmapRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Short-transaction CRUD helpers for the {@link UserLesson} and {@link UserRoadmap} enrollment
 * join entities.
 * <p>
 * This is the only service that may inject {@link UserLessonRepository} or
 * {@link UserRoadmapRepository} directly. All other services that require learning-enrollment
 * data must depend on this service.
 */
@Service
@RequiredArgsConstructor
public class LearningEnrollmentEntityService {

	private final UserLessonRepository userLessonRepository;
	private final UserRoadmapRepository userRoadmapRepository;

	/**
	 * Loads a single lesson enrollment by its composite primary key.
	 *
	 * @param id the composite user/lesson primary key
	 * @return the {@link UserLesson} enrollment, if it exists
	 */
	@Transactional(readOnly = true)
	public Optional<UserLesson> findUserLessonById(UserLesson.UserLessonId id) {
		return userLessonRepository.findById(id);
	}

	/**
	 * Returns whether a lesson enrollment exists for the given composite primary key.
	 *
	 * @param id the composite user/lesson primary key
	 * @return {@code true} if the enrollment exists
	 */
	@Transactional(readOnly = true)
	public boolean existsUserLessonById(UserLesson.UserLessonId id) {
		return userLessonRepository.existsById(id);
	}

	/**
	 * Loads every lesson enrollment for a single user.
	 *
	 * @param userId the user primary key
	 * @return every {@link UserLesson} row for the given user
	 */
	@Transactional(readOnly = true)
	public List<UserLesson> findUserLessonsByUserId(Long userId) {
		return userLessonRepository.findByUserId(userId);
	}

	/**
	 * Loads a single lesson enrollment for a user and lesson pair.
	 *
	 * @param userId   the user primary key
	 * @param lessonId the lesson primary key
	 * @return the matching {@link UserLesson} enrollment, if it exists
	 */
	@Transactional(readOnly = true)
	public Optional<UserLesson> findUserLessonByUserIdAndLessonId(Long userId, Long lessonId) {
		return userLessonRepository.findByUserIdAndLessonId(userId, lessonId);
	}

	/**
	 * Loads lesson enrollments for a lesson with the enrolled user eagerly fetched.
	 *
	 * @param lessonId lesson primary key
	 * @return enrollments ordered newest first
	 */
	@Transactional(readOnly = true)
	public List<UserLesson> findByLessonIdWithUser(Long lessonId) {
		return userLessonRepository.findByLessonIdWithUser(lessonId);
	}

	/**
	 * Loads roadmap enrollments for a roadmap with the enrolled user eagerly fetched.
	 *
	 * @param roadmapId roadmap primary key
	 * @return enrollments ordered newest first
	 */
	@Transactional(readOnly = true)
	public List<UserRoadmap> findByRoadmapIdWithUser(Long roadmapId) {
		return userRoadmapRepository.findByRoadmapIdWithUser(roadmapId);
	}

	/**
	 * Loads every lesson enrollment for a user, restricted to a set of lesson IDs, for
	 * building a per-lesson completion map.
	 *
	 * @param userId    the user primary key
	 * @param lessonIds the lesson primary keys to restrict to
	 * @return every matching {@link UserLesson} row
	 */
	@Transactional(readOnly = true)
	public List<UserLesson> findByUserIdAndLessonIdIn(Long userId, Collection<Long> lessonIds) {
		return userLessonRepository.findByUserIdAndLessonIdIn(userId, List.copyOf(lessonIds));
	}

	/**
	 * Loads lesson enrollment rows for many users and lessons in one query.
	 *
	 * @param userIds   user primary keys to restrict to
	 * @param lessonIds lesson primary keys to restrict to
	 * @return every matching {@link UserLesson} row
	 */
	@Transactional(readOnly = true)
	public List<UserLesson> findUserLessonsByUserIdsAndLessonIds(Collection<Long> userIds,
																 Collection<Long> lessonIds) {
		if (userIds == null || userIds.isEmpty() || lessonIds == null || lessonIds.isEmpty()) {
			return List.of();
		}
		return userLessonRepository.findByUserIdsAndLessonIds(userIds, lessonIds);
	}

	/**
	 * Loads a bounded page of published-lesson enrollment summaries for "My Lessons",
	 * incomplete-first then newest-enrolled-first. Returns a lean projection, never the full
	 * {@code Lesson} entity.
	 *
	 * @param userId   the user primary key
	 * @param pageable page and size request
	 * @return the user's published-lesson summary page, ordered incomplete-first then newest-enrolled-first
	 */
	@Transactional(readOnly = true)
	public Page<MyLessonSummaryProjection> findMyLessonsPage(Long userId, Pageable pageable) {
		return userLessonRepository.findMyLessonsPage(userId, pageable);
	}

	/**
	 * Persists a lesson enrollment row.
	 *
	 * @param userLesson the lesson enrollment to save
	 * @return the saved {@link UserLesson}
	 */
	@Transactional
	public UserLesson save(UserLesson userLesson) {
		return userLessonRepository.save(userLesson);
	}

	/**
	 * Persists lesson enrollment rows in a batch.
	 *
	 * @param userLessons lesson enrollments to save
	 * @return saved lesson enrollments
	 */
	@Transactional
	public List<UserLesson> saveAllUserLessons(List<UserLesson> userLessons) {
		if (userLessons == null || userLessons.isEmpty()) {
			return List.of();
		}
		return userLessonRepository.saveAll(userLessons);
	}

	/**
	 * Removes a lesson enrollment row.
	 *
	 * @param userLesson the lesson enrollment to delete
	 */
	@Transactional
	public void delete(UserLesson userLesson) {
		userLessonRepository.delete(userLesson);
	}

	/**
	 * Bulk-deletes the lesson enrollments a set of users gained via one roadmap's lesson list,
	 * except lessons still granted by another roadmap the same user remains enrolled in. One
	 * set-based statement regardless of user or lesson count.
	 *
	 * @param userIds   users whose roadmap-derived lesson enrollments are being revoked
	 * @param roadmapId roadmap being revoked
	 * @return number of deleted lesson-enrollment rows
	 */
	@Transactional
	public int deleteRoadmapDerivedLessonEnrollments(Collection<Long> userIds, Long roadmapId) {
		if (userIds == null || userIds.isEmpty()) {
			return 0;
		}
		return userLessonRepository.deleteRoadmapDerivedLessonEnrollments(userIds, roadmapId);
	}

	/**
	 * Bulk-deletes a lesson enrollment row for a set of users. One set-based statement
	 * regardless of user count.
	 *
	 * @param userIds  users whose lesson enrollment is being revoked
	 * @param lessonId lesson being revoked
	 * @return number of deleted lesson-enrollment rows
	 */
	@Transactional
	public int deleteUserLessonsByUserIdsAndLessonId(Collection<Long> userIds, Long lessonId) {
		if (userIds == null || userIds.isEmpty()) {
			return 0;
		}
		return userLessonRepository.deleteByUserIdsAndLessonId(userIds, lessonId);
	}

	/**
	 * Loads a single roadmap enrollment by its composite primary key.
	 *
	 * @param id the composite user/roadmap primary key
	 * @return the {@link UserRoadmap} enrollment, if it exists
	 */
	@Transactional(readOnly = true)
	public Optional<UserRoadmap> findUserRoadmapById(UserRoadmap.UserRoadmapId id) {
		return userRoadmapRepository.findById(id);
	}

	/**
	 * Loads every roadmap enrollment for a single user.
	 *
	 * @param userId the user primary key
	 * @return every {@link UserRoadmap} row for the given user
	 */
	@Transactional(readOnly = true)
	public List<UserRoadmap> findUserRoadmapsByUserId(Long userId) {
		return userRoadmapRepository.findByIdUserId(userId);
	}

	/**
	 * Loads one user's roadmap enrollment rows for a bounded set of roadmap IDs.
	 *
	 * @param userId     user primary key
	 * @param roadmapIds roadmap primary keys to restrict to
	 * @return matching {@link UserRoadmap} rows
	 */
	@Transactional(readOnly = true)
	public List<UserRoadmap> findUserRoadmapsByUserIdAndRoadmapIds(Long userId, Collection<Long> roadmapIds) {
		if (roadmapIds == null || roadmapIds.isEmpty()) {
			return List.of();
		}
		return userRoadmapRepository.findByUserIdAndRoadmapIds(userId, roadmapIds);
	}

	/**
	 * Loads every roadmap enrollment for a single roadmap, across all users.
	 *
	 * @param roadmapId the roadmap primary key
	 * @return every {@link UserRoadmap} row for the given roadmap
	 */
	@Transactional(readOnly = true)
	public List<UserRoadmap> findUserRoadmapsByRoadmapId(Long roadmapId) {
		return userRoadmapRepository.findByIdRoadmapId(roadmapId);
	}

	/**
	 * Loads roadmap enrollment rows for many users and one roadmap in one query.
	 *
	 * @param userIds   user primary keys to restrict to
	 * @param roadmapId roadmap primary key
	 * @return matching {@link UserRoadmap} rows
	 */
	@Transactional(readOnly = true)
	public List<UserRoadmap> findUserRoadmapsByUserIdsAndRoadmapId(Collection<Long> userIds, Long roadmapId) {
		if (userIds == null || userIds.isEmpty()) {
			return List.of();
		}
		return userRoadmapRepository.findByUserIdsAndRoadmapId(userIds, roadmapId);
	}

	/**
	 * Returns roadmaps that became complete for a user after a lesson was completed.
	 *
	 * @param userId   user primary key
	 * @param lessonId changed lesson primary key
	 * @return completed roadmap records ordered by title
	 */
	@Transactional(readOnly = true)
	public List<CompletedRoadmapRecord> findCompletedRoadmapsForUserLesson(Long userId, Long lessonId) {
		return userRoadmapRepository.findCompletedRoadmapsForUserLesson(userId, lessonId).stream()
				.map(this::toCompletedRoadmapRecord)
				.toList();
	}

	/**
	 * Persists a roadmap enrollment row.
	 *
	 * @param userRoadmap the roadmap enrollment to save
	 * @return the saved {@link UserRoadmap}
	 */
	@Transactional
	public UserRoadmap save(UserRoadmap userRoadmap) {
		return userRoadmapRepository.save(userRoadmap);
	}

	/**
	 * Persists roadmap enrollment rows in a batch.
	 *
	 * @param userRoadmaps roadmap enrollments to save
	 * @return saved roadmap enrollments
	 */
	@Transactional
	public List<UserRoadmap> saveAllUserRoadmaps(List<UserRoadmap> userRoadmaps) {
		if (userRoadmaps == null || userRoadmaps.isEmpty()) {
			return List.of();
		}
		return userRoadmapRepository.saveAll(userRoadmaps);
	}

	/**
	 * Bulk-deletes the direct roadmap enrollment rows for a set of users and one roadmap. One
	 * set-based statement regardless of user count.
	 *
	 * @param userIds   users whose roadmap enrollment is being revoked
	 * @param roadmapId roadmap being revoked
	 * @return number of deleted roadmap-enrollment rows
	 */
	@Transactional
	public int deleteUserRoadmapsByUserIdsAndRoadmapId(Collection<Long> userIds, Long roadmapId) {
		if (userIds == null || userIds.isEmpty()) {
			return 0;
		}
		return userRoadmapRepository.deleteByUserIdsAndRoadmapId(userIds, roadmapId);
	}

	/**
	 * Removes a roadmap enrollment row.
	 *
	 * @param userRoadmap the roadmap enrollment to delete
	 */
	@Transactional
	public void delete(UserRoadmap userRoadmap) {
		userRoadmapRepository.delete(userRoadmap);
	}

	/**
	 * Returns the set of lesson IDs, among the given lesson IDs, that a user is currently
	 * enrolled in. Bounded by the given lesson IDs (e.g. one results page) rather than the
	 * user's total lifetime enrollment count.
	 *
	 * @param userId    the user primary key
	 * @param lessonIds the lesson primary keys to restrict to
	 * @return the distinct enrolled lesson IDs, among {@code lessonIds}
	 */
	@Transactional(readOnly = true)
	public Set<Long> findEnrolledLessonIds(Long userId, Collection<Long> lessonIds) {
		if (lessonIds == null || lessonIds.isEmpty()) {
			return Set.of();
		}
		return userLessonRepository.findByUserIdAndLessonIdIn(userId, List.copyOf(lessonIds)).stream()
				.map(userLesson -> userLesson.getId().getLessonId())
				.collect(Collectors.toSet());
	}

	CompletedRoadmapRecord toCompletedRoadmapRecord(CompletedRoadmapProjection projection) {
		return new CompletedRoadmapRecord(projection.getId(), projection.getTitle());
	}
}
