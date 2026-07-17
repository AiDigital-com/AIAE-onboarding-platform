package com.aidigital.aionboarding.service.lessonactivity.services;

import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.lessonactivity.models.GenerateActivityResultRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.LessonActivityRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.LessonActivityWithAttemptsRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.SubmitActivityProgressResultRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.UpdateActivityInput;
import com.aidigital.aionboarding.service.lessonactivity.models.UpdateActivityResultRecord;

import java.util.List;
import java.util.Map;

/**
 * Coordinates lesson activity generation, editing, and learner progress submission.
 */
public interface LessonActivityService {

	/**
	 * Returns activities for a visible lesson with the current user's progress.
	 *
	 * @param viewer   authenticated user
	 * @param lessonId lesson identifier
	 * @return ordered activity records with progress
	 * @throws com.aidigital.aionboarding.service.common.error.AppException when the lesson is missing
	 *                                                                      or not visible
	 */
	List<LessonActivityRecord> getLessonActivities(AppUser viewer, Long lessonId);

	/**
	 * Returns a single activity payload, progress, and attempts for the current user.
	 *
	 * @param viewer     authenticated user
	 * @param lessonId   lesson identifier
	 * @param activityId activity identifier
	 * @return activity with attempts
	 * @throws com.aidigital.aionboarding.service.common.error.AppException when the lesson or activity
	 *                                                                      is missing or not visible
	 */
	LessonActivityWithAttemptsRecord getLessonActivity(AppUser viewer, Long lessonId, Long activityId);

	/**
	 * Generates a new AI activity for a ready lesson.
	 *
	 * @param viewer   authenticated manager with {@code lessons.manage_activities}
	 * @param lessonId lesson identifier
	 * @param type     requested activity type code
	 * @param count    optional item count for generation
	 * @return generated activity and prompt metadata
	 * @throws com.aidigital.aionboarding.service.common.error.AppException when the lesson is not ready,
	 *                                                                      content is empty, or the caller lacks
	 *                                                                      permission
	 */
	GenerateActivityResultRecord generateActivity(AppUser viewer, Long lessonId, String type, Integer count);

	/**
	 * Updates an existing activity payload for a lesson.
	 *
	 * @param viewer     authenticated manager with {@code lessons.manage_activities}
	 * @param lessonId   lesson identifier
	 * @param activityId activity identifier
	 * @param request    normalized flashcards or quiz payload
	 * @return updated activity and refreshed lesson activity list
	 * @throws com.aidigital.aionboarding.service.common.error.AppException when the activity is missing
	 *                                                                      or the caller lacks permission
	 */
	UpdateActivityResultRecord updateActivity(
			AppUser viewer,
			Long lessonId,
			Long activityId,
			UpdateActivityInput request
	);

	/**
	 * Records learner progress for a quiz or flashcards activity.
	 *
	 * @param viewer     authenticated learner with {@code learning.complete}
	 * @param lessonId   lesson identifier
	 * @param activityId activity identifier
	 * @param request    submission payload including activity type and answers
	 * @return progress, activities, enrollment, attempt, and completed roadmap side effects
	 * @throws com.aidigital.aionboarding.service.common.error.AppException when the lesson is not enrolled,
	 *                                                                      not published, or the submission is invalid
	 */
	SubmitActivityProgressResultRecord submitActivityProgress(
			AppUser viewer,
			Long lessonId,
			Long activityId,
			Map<String, Object> request
	);

	/**
	 * Clears stored progress for one activity and unmarks lesson completion when needed.
	 *
	 * @param viewer     authenticated learner with {@code learning.complete}
	 * @param lessonId   lesson identifier
	 * @param activityId activity identifier
	 * @throws com.aidigital.aionboarding.service.common.error.AppException when the lesson is not enrolled
	 *                                                                      or not published
	 */
	void resetActivityProgress(AppUser viewer, Long lessonId, Long activityId);

	/**
	 * Deletes an existing activity and returns the lesson's refreshed activity list.
	 *
	 * @param viewer     authenticated manager with {@code lessons.manage_activities}
	 * @param lessonId   lesson identifier
	 * @param activityId activity identifier
	 * @return refreshed activities for the lesson
	 * @throws com.aidigital.aionboarding.service.common.error.AppException when the activity is missing
	 *                                                                      or the caller lacks permission
	 */
	List<LessonActivityRecord> deleteActivity(AppUser viewer, Long lessonId, Long activityId);
}
