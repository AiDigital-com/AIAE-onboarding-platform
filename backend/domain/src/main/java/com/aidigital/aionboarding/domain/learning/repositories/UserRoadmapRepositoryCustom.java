package com.aidigital.aionboarding.domain.learning.repositories;

import java.util.List;

/**
 * Criteria-API-backed queries for {@code UserRoadmapRepository} that a derived or JPQL
 * {@code @Query} method cannot express.
 */
public interface UserRoadmapRepositoryCustom {

	/**
	 * Finds roadmaps the user is enrolled in that contain the given lesson and are now fully
	 * completed (every lesson in the roadmap has a completed {@code UserLesson} row for this
	 * user).
	 *
	 * @param userId   the user primary key
	 * @param lessonId the lesson primary key that just changed completion state
	 * @return completed roadmaps containing the lesson, ordered by title
	 */
	List<CompletedRoadmapProjection> findCompletedRoadmapsForUserLesson(Long userId, Long lessonId);
}
