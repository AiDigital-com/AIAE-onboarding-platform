package com.aidigital.aionboarding.service.lesson.services;

import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.lesson.enums.LessonStatusAction;
import com.aidigital.aionboarding.service.lesson.models.CreateLessonAssetInput;
import com.aidigital.aionboarding.service.lesson.models.CreateLessonInput;
import com.aidigital.aionboarding.service.lesson.models.LessonAssetDeleteResultRecord;
import com.aidigital.aionboarding.service.lesson.models.LessonAssetResultRecord;
import com.aidigital.aionboarding.service.lesson.models.LessonDetailRecord;
import com.aidigital.aionboarding.service.lesson.models.LessonListQuery;
import com.aidigital.aionboarding.service.lesson.models.LessonSearchSummaryRecord;
import com.aidigital.aionboarding.service.lesson.models.LessonSummaryRecord;
import com.aidigital.aionboarding.service.lesson.models.UpdateLessonContentInput;
import org.springframework.data.domain.Page;

/**
 * Manages lesson lifecycle operations including listing, creation, content updates, publication state,
 * deletion, and asset attachment.
 */
public interface LessonService {

	/**
	 * Returns a bounded, sorted page of lessons visible to the viewer based on role, ownership, and
	 * publication status.
	 *
	 * @param viewer authenticated user
	 * @param query  typed filter and sort parameters
	 * @param page   zero-based page index
	 * @param size   maximum number of lessons per page
	 * @return a page of visible lesson summaries
	 */
	Page<LessonSearchSummaryRecord> getAllLessons(AppUser viewer, LessonListQuery query, int page, int size);

	/**
	 * Counts lessons visible to the viewer matching the given filter, without fetching or
	 * paginating any rows.
	 *
	 * @param viewer authenticated user
	 * @param query  typed filter and sort parameters
	 * @return the number of visible lessons matching the filter
	 */
	long countLessons(AppUser viewer, LessonListQuery query);

	/**
	 * Returns a single lesson by id if visible to the viewer.
	 *
	 * @param viewer authenticated user
	 * @param id     lesson identifier
	 * @return lesson detail
	 * @throws com.aidigital.aionboarding.service.common.error.AppException when the lesson is missing
	 *                                                                      or not visible
	 */
	LessonDetailRecord getLesson(AppUser viewer, Long id);

	/**
	 * Returns only a lesson's current generation status, for lightweight polling while a lesson
	 * is generating instead of refetching the full search list or lesson detail.
	 *
	 * @param viewer authenticated user
	 * @param id     lesson identifier
	 * @return the lesson's current status code
	 * @throws com.aidigital.aionboarding.service.common.error.AppException when the lesson is missing
	 *                                                                      or not visible
	 */
	String getLessonGenerationStatus(AppUser viewer, Long id);

	/**
	 * Creates a draft lesson with default generation settings.
	 *
	 * @param viewer authenticated manager with {@code lessons.create}
	 * @param input  initial title and generation preferences
	 * @return created lesson summary
	 * @throws com.aidigital.aionboarding.service.common.error.AppException when the caller lacks permission
	 */
	LessonSummaryRecord createLesson(AppUser viewer, CreateLessonInput input);

	/**
	 * Updates editable lesson content fields.
	 *
	 * @param viewer authenticated manager allowed to manage the lesson
	 * @param id     lesson identifier
	 * @param input  optional title, markdown, and HTML updates
	 * @return the refreshed full lesson detail, so API responses stay authoritative for
	 * assets, materials, and revision history
	 * @throws com.aidigital.aionboarding.service.common.error.AppException when the lesson is missing
	 *                                                                      or the caller lacks permission
	 */
	LessonDetailRecord updateLessonContent(AppUser viewer, Long id, UpdateLessonContentInput input);

	/**
	 * Changes lesson publication state.
	 *
	 * @param viewer authenticated manager with {@code lessons.publish_archive}
	 * @param id     lesson identifier
	 * @param action publication-state transition to apply
	 * @return the refreshed full lesson detail, so API responses stay authoritative for
	 * assets, materials, and revision history
	 * @throws com.aidigital.aionboarding.service.common.error.AppException when the lesson is missing
	 *                                                                      or the caller lacks permission
	 */
	LessonDetailRecord changeLessonStatus(AppUser viewer, Long id, LessonStatusAction action);

	/**
	 * Permanently deletes a lesson.
	 *
	 * @param viewer authenticated manager with {@code lessons.manage}
	 * @param id     lesson identifier
	 * @throws com.aidigital.aionboarding.service.common.error.AppException when the lesson is missing
	 *                                                                      or the caller lacks permission
	 */
	void deleteLesson(AppUser viewer, Long id);

	/**
	 * Adds an asset to a lesson.
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
	 * @param assetId  lesson asset identifier
	 * @return refreshed lesson detail
	 * @throws com.aidigital.aionboarding.service.common.error.AppException when the lesson or asset is missing,
	 *                                                                      or the caller lacks permission
	 */
	LessonAssetDeleteResultRecord deleteAsset(AppUser viewer, Long lessonId, Long assetId);
}
