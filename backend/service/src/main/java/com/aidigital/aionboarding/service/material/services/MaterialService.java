package com.aidigital.aionboarding.service.material.services;

import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.material.models.CreateMaterialInput;
import com.aidigital.aionboarding.service.material.models.MaterialListQuery;
import com.aidigital.aionboarding.service.material.models.MaterialOpenAiUploadInput;
import com.aidigital.aionboarding.service.material.models.MaterialOpenAiUploadRecord;
import com.aidigital.aionboarding.service.material.models.MaterialRecord;
import com.aidigital.aionboarding.service.material.models.MaterialSearchSummaryRecord;
import com.aidigital.aionboarding.service.material.models.UpdateMaterialInput;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Application service for listing, creating, updating, and deleting training materials.
 *
 * <p>Read operations do not enforce additional service-layer permissions beyond authenticated access
 * enforced by the API layer. Mutating operations require the corresponding materials permission
 * ({@code materials.create}, {@code materials.edit}, or {@code materials.delete}).</p>
 */
public interface MaterialService {

	/**
	 * Returns a bounded, sorted page of materials matching the given filter, with related assets and
	 * lesson usage counts.
	 *
	 * @param viewer authenticated caller; retained for API symmetry
	 * @param query  typed filter and sort parameters
	 * @param page   zero-based page index
	 * @param size   maximum number of materials per page
	 * @return a page of material search summaries
	 */
	Page<MaterialSearchSummaryRecord> getAll(AppUser viewer, MaterialListQuery query, int page, int size);

	/**
	 * Counts materials matching the given filter, without fetching or paginating any rows.
	 *
	 * @param viewer authenticated caller; retained for API symmetry with {@link #getAll}
	 * @param query  typed filter and sort parameters
	 * @return the number of materials matching the filter
	 */
	long count(AppUser viewer, MaterialListQuery query);

	/**
	 * Returns materials for the requested identifiers, preserving the input order and skipping unknown ids.
	 *
	 * @param materialIds material identifiers to resolve; {@code null} and duplicate values are ignored
	 * @return matching material records in request order
	 */
	List<MaterialRecord> getByIds(List<Long> materialIds);

	/**
	 * Returns the full-fidelity material record for a single material, for detail-dialog fetches.
	 *
	 * @param id material identifier
	 * @return the full material record
	 * @throws com.aidigital.aionboarding.service.common.error.AppException {@link com.aidigital.aionboarding.service.common.error.ErrorReason#C001}
	 *                                                                      when the material does not exist
	 */
	MaterialRecord getById(Long id);

	/**
	 * Creates a material and its related YouTube, link, and file assets.
	 *
	 * @param viewer authenticated caller
	 * @param input  create payload
	 * @return persisted material record
	 * @throws com.aidigital.aionboarding.service.common.error.AppException {@link com.aidigital.aionboarding.service.common.error.ErrorReason#C004}
	 *                                                                      when the caller lacks {@code materials
	 *                                                                      .create}
	 * @throws com.aidigital.aionboarding.service.common.error.AppException {@link com.aidigital.aionboarding.service.common.error.ErrorReason#C002}
	 *                                                                      when the payload fails validation
	 */
	MaterialRecord create(AppUser viewer, CreateMaterialInput input);

	/**
	 * Replaces a material and all of its related assets.
	 *
	 * @param viewer authenticated caller
	 * @param id     material identifier
	 * @param input  update payload
	 * @return updated material record
	 * @throws com.aidigital.aionboarding.service.common.error.AppException {@link com.aidigital.aionboarding.service.common.error.ErrorReason#C004}
	 *                                                                      when the caller lacks {@code materials
	 *                                                                      .edit}
	 * @throws com.aidigital.aionboarding.service.common.error.AppException {@link com.aidigital.aionboarding.service.common.error.ErrorReason#C001}
	 *                                                                      when the material does not exist
	 * @throws com.aidigital.aionboarding.service.common.error.AppException {@link com.aidigital.aionboarding.service.common.error.ErrorReason#C002}
	 *                                                                      when the payload fails validation
	 */
	MaterialRecord update(AppUser viewer, Long id, UpdateMaterialInput input);

	/**
	 * Deletes a material and its related assets when it is not referenced by any lesson.
	 *
	 * @param viewer authenticated caller
	 * @param id     material identifier
	 * @throws com.aidigital.aionboarding.service.common.error.AppException {@link com.aidigital.aionboarding.service.common.error.ErrorReason#C004}
	 *                                                                      when the caller lacks {@code materials
	 *                                                                      .delete}
	 * @throws com.aidigital.aionboarding.service.common.error.AppException {@link com.aidigital.aionboarding.service.common.error.ErrorReason#C001}
	 *                                                                      when the material does not exist
	 * @throws com.aidigital.aionboarding.service.common.error.AppException {@link com.aidigital.aionboarding.service.common.error.ErrorReason#C006}
	 *                                                                      when the material is still used by one or
	 *                                                                      more lessons
	 */
	void delete(AppUser viewer, Long id);

	/**
	 * Updates OpenAI upload metadata for a material file row.
	 *
	 * <p>This operation is invoked by internal lesson-generation flows and does not perform an
	 * additional permission check in the service layer.</p>
	 *
	 * @param fileId material file identifier
	 * @param input  OpenAI upload fields to store
	 * @return updated upload record, or {@code null} when the file row does not exist
	 */
	MaterialOpenAiUploadRecord updateMaterialFileOpenAIUpload(Long fileId, MaterialOpenAiUploadInput input);
}
