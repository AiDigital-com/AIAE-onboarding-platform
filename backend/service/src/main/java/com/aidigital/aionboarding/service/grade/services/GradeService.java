package com.aidigital.aionboarding.service.grade.services;

import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.grade.models.CreateGradeInput;
import com.aidigital.aionboarding.service.grade.models.GradeRecord;
import com.aidigital.aionboarding.service.grade.models.UpdateGradeInput;

import java.util.List;

/**
 * Manages the Admin-configurable grade dictionary (e.g. Junior, Middle, Senior) used to tag user
 * profiles and to filter standing roadmap group assignments.
 */
public interface GradeService {

	/**
	 * Lists active grades in display order. Readable by any authenticated user, since grade
	 * values themselves are not sensitive.
	 *
	 * @return active grades in display order
	 */
	List<GradeRecord> listActive();

	/**
	 * Lists every grade, including deactivated ones, for the grade management UI.
	 *
	 * @param viewer authenticated caller
	 * @return all grades in display order
	 * @throws com.aidigital.aionboarding.service.common.error.AppException when the caller lacks {@code grades.manage}
	 */
	List<GradeRecord> listAll(AppUser viewer);

	/**
	 * Creates a new grade.
	 *
	 * @param viewer authenticated caller
	 * @param input  grade name; the internal code is derived from it
	 * @return the created grade
	 * @throws com.aidigital.aionboarding.service.common.error.AppException when the caller lacks
	 *                                                                      {@code grades.manage}, the name is
	 *                                                                      invalid, or the name/code is already in use
	 */
	GradeRecord create(AppUser viewer, CreateGradeInput input);

	/**
	 * Renames an existing grade.
	 *
	 * @param viewer authenticated caller
	 * @param id     grade primary key
	 * @param input  new grade name
	 * @return the updated grade
	 * @throws com.aidigital.aionboarding.service.common.error.AppException when the caller lacks
	 *                                                                      {@code grades.manage}, the grade is
	 *                                                                      missing, the name is invalid, or the name
	 *                                                                      is already in use
	 */
	GradeRecord update(AppUser viewer, Long id, UpdateGradeInput input);

	/**
	 * Deactivates a grade so it no longer appears in active-grade lists. Existing users keep their
	 * current grade reference; the grade simply stops being offered for new assignment.
	 *
	 * @param viewer authenticated caller
	 * @param id     grade primary key
	 * @throws com.aidigital.aionboarding.service.common.error.AppException when the caller lacks
	 *                                                                      {@code grades.manage} or the grade is
	 *                                                                      missing
	 */
	void deactivate(AppUser viewer, Long id);

	/**
	 * Reactivates a previously deactivated grade so it appears in active-grade lists again.
	 *
	 * @param viewer authenticated caller
	 * @param id     grade primary key
	 * @throws com.aidigital.aionboarding.service.common.error.AppException when the caller lacks
	 *                                                                      {@code grades.manage} or the grade is
	 *                                                                      missing
	 */
	void activate(AppUser viewer, Long id);
}
