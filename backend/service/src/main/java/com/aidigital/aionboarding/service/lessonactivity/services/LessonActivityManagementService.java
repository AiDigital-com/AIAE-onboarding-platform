package com.aidigital.aionboarding.service.lessonactivity.services;

import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.lessonactivity.models.GenerateActivityResultRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.LessonActivityRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.UpdateActivityInput;
import com.aidigital.aionboarding.service.lessonactivity.models.UpdateActivityResultRecord;

import java.util.List;

/**
 * Generates and edits lesson activities for managers.
 */
public interface LessonActivityManagementService {

	/**
	 * Generates a new AI activity for a ready lesson.
	 *
	 * @param viewer   authenticated manager with {@code lessons.manage_activities}
	 * @param lessonId lesson identifier
	 * @param type     requested activity type code
	 * @param count    optional item count for generation
	 * @return generated activity and prompt metadata
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
	 */
	UpdateActivityResultRecord updateActivity(
			AppUser viewer,
			Long lessonId,
			Long activityId,
			UpdateActivityInput request
	);

	/**
	 * Deletes an existing activity and returns the lesson's refreshed activity list.
	 *
	 * @param viewer     authenticated manager with {@code lessons.manage_activities}
	 * @param lessonId   lesson identifier
	 * @param activityId activity identifier
	 * @return refreshed activities for the lesson
	 */
	List<LessonActivityRecord> deleteActivity(AppUser viewer, Long lessonId, Long activityId);
}
