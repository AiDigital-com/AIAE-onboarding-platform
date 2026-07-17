package com.aidigital.aionboarding.service.lessonactivity.services;

import com.aidigital.aionboarding.service.lessonactivity.models.ActivityAttemptRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.LessonActivityCountsRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.LessonActivityRecord;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Loads and orders lesson activities for manager and learner views.
 */
public interface LessonActivityAssemblyService {

	/**
	 * Returns all activities for a lesson without learner progress.
	 *
	 * @param lessonId lesson identifier
	 * @return ordered activity records
	 */
	List<LessonActivityRecord> getLessonActivities(Long lessonId);

	/**
	 * Returns activities for a lesson including the learner's progress state.
	 *
	 * @param lessonId lesson identifier
	 * @param userId   learner identifier
	 * @return ordered activity records with progress when present
	 */
	List<LessonActivityRecord> getLessonActivitiesForUser(Long lessonId, Long userId);

	/**
	 * Returns activities for several lessons including one learner's progress state.
	 *
	 * @param userId    learner identifier
	 * @param lessonIds lesson identifiers
	 * @return activity records grouped by lesson ID
	 */
	Map<Long, List<LessonActivityRecord>> getLessonActivitiesForUserByLessonIds(Long userId,
																				Collection<Long> lessonIds);

	/**
	 * Returns flashcard/quiz activity counts for several lessons, without loading each
	 * activity's JSONB payload or generation metadata — for card-level summaries.
	 *
	 * @param lessonIds lesson identifiers
	 * @return activity counts grouped by lesson ID; lessons with no activities are omitted
	 */
	Map<Long, LessonActivityCountsRecord> countActivitiesByLessonIds(Collection<Long> lessonIds);

	/**
	 * Returns a single activity including the learner's progress state.
	 *
	 * @param lessonId   lesson identifier
	 * @param activityId activity identifier
	 * @param userId     learner identifier
	 * @return activity record with progress when present, or {@code null} when not found
	 */
	LessonActivityRecord getLessonActivity(Long lessonId, Long activityId, Long userId);

	/**
	 * Returns a learner's attempts on a single activity, most recent first.
	 *
	 * @param lessonId   lesson identifier
	 * @param activityId activity identifier
	 * @param userId     learner identifier
	 * @return attempt records ordered by attempt number descending
	 */
	List<ActivityAttemptRecord> getAttemptsForActivity(Long lessonId, Long activityId, Long userId);
}
