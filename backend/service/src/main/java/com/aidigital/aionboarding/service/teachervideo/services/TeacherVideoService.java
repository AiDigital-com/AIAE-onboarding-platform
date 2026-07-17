package com.aidigital.aionboarding.service.teachervideo.services;

import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.lesson.models.TeacherVideoDeleteResultRecord;
import com.aidigital.aionboarding.service.lesson.models.TeacherVideoResultRecord;

/**
 * Manages HeyGen teacher video generation lifecycle for lessons.
 */
public interface TeacherVideoService {

	/**
	 * Starts teacher video generation for a ready lesson.
	 *
	 * @param viewer   authenticated admin with {@code lessons.manage} who can manage the lesson
	 * @param lessonId lesson identifier
	 * @return teacher video status and updated lesson detail
	 * @throws com.aidigital.aionboarding.service.common.error.AppException when the caller lacks permission,
	 *                                                                      is not an admin, the lesson is missing or
	 *                                                                      not ready, content is empty, generation is
	 *                                                                      already active,
	 *                                                                      or HeyGen fails
	 */
	TeacherVideoResultRecord create(AppUser viewer, Long lessonId);

	/**
	 * Returns the current teacher video status, refreshing signed URLs when needed.
	 *
	 * @param viewer   authenticated admin with {@code lessons.manage}
	 * @param lessonId lesson identifier
	 * @return refreshed teacher video status and lesson detail
	 * @throws com.aidigital.aionboarding.service.common.error.AppException when the caller lacks permission,
	 *                                                                      is not an admin, the lesson is missing, no
	 *                                                                      teacher video was requested, or HeyGen
	 *                                                                      status lookup fails
	 */
	TeacherVideoResultRecord getStatus(AppUser viewer, Long lessonId);

	/**
	 * Removes teacher video metadata from a lesson.
	 *
	 * @param viewer   authenticated admin with {@code lessons.manage}
	 * @param lessonId lesson identifier
	 * @return updated lesson detail after metadata removal
	 * @throws com.aidigital.aionboarding.service.common.error.AppException when the caller lacks permission,
	 *                                                                      is not an admin, or the lesson is missing
	 */
	TeacherVideoDeleteResultRecord delete(AppUser viewer, Long lessonId);
}
