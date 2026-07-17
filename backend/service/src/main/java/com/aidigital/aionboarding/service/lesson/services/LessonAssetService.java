package com.aidigital.aionboarding.service.lesson.services;

import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.lesson.models.CreateLessonAssetInput;
import com.aidigital.aionboarding.service.lesson.models.LessonAssetDeleteResultRecord;
import com.aidigital.aionboarding.service.lesson.models.LessonAssetResultRecord;

/**
 * Creates and enriches lesson assets such as links, YouTube videos, images, and uploaded files.
 */
public interface LessonAssetService {

	/**
	 * Adds an asset to a lesson after validating input and fetching external metadata when needed.
	 *
	 * @param viewer   authenticated manager with {@code lessons.manage_assets}
	 * @param lessonId lesson identifier
	 * @param input    asset payload including kind, URL, or uploaded file details
	 * @return saved asset and refreshed lesson detail
	 * @throws com.aidigital.aionboarding.service.common.error.AppException when the lesson is missing,
	 *                                                                      the caller lacks permission, or the asset
	 *                                                                      payload is invalid
	 */
	LessonAssetResultRecord createAsset(AppUser viewer, Long lessonId, CreateLessonAssetInput input);

	/**
	 * Removes an asset from a lesson.
	 *
	 * @param viewer   authenticated manager with {@code lessons.manage_assets}
	 * @param lessonId lesson identifier
	 * @param assetId  asset identifier scoped to the lesson
	 * @return refreshed lesson detail
	 * @throws com.aidigital.aionboarding.service.common.error.AppException when the lesson or asset is missing,
	 *                                                                      or the caller lacks permission
	 */
	LessonAssetDeleteResultRecord deleteAsset(AppUser viewer, Long lessonId, Long assetId);
}
