package com.aidigital.aionboarding.service.lesson.services;

import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.lesson.models.ReviseLessonInput;
import com.aidigital.aionboarding.service.lesson.models.RevisionResultRecord;

/**
 * Orchestrates AI-assisted revision of ready lessons based on user feedback and selected options.
 */
public interface LessonRevisionService {

	/**
	 * Revises a ready lesson using planner and writer prompts, then persists updated content and metadata.
	 *
	 * @param viewer   authenticated manager with {@code lessons.manage}
	 * @param lessonId lesson identifier
	 * @param request  revision notes and selected revision options
	 * @return revised lesson detail and planner brief
	 * @throws com.aidigital.aionboarding.service.common.error.AppException when the lesson is missing,
	 *                                                                      not ready, has empty content, lacks
	 *                                                                      revision input, or the caller lacks
	 *                                                                      permission
	 */
	RevisionResultRecord reviseLesson(AppUser viewer, Long lessonId, ReviseLessonInput request);
}
