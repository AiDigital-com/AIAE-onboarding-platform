package com.aidigital.aionboarding.service.learning.services;

import com.aidigital.aionboarding.service.learning.models.CompletedRoadmapRecord;

import java.util.List;

/**
 * Computes roadmap completion side effects after lesson progress changes.
 */
public interface RoadmapEnrollmentSyncService {

	/**
	 * Returns roadmaps fully completed by the user after finishing the given lesson.
	 *
	 * @param userId   learner identifier
	 * @param lessonId lesson identifier just completed or evaluated
	 * @return completed roadmap summaries sorted by title
	 */
	List<CompletedRoadmapRecord> getCompletedRoadmapsForUserLesson(Long userId, Long lessonId);
}
