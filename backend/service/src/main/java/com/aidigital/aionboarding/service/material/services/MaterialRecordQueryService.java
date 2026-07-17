package com.aidigital.aionboarding.service.material.services;

import com.aidigital.aionboarding.domain.material.entities.Material;
import com.aidigital.aionboarding.service.material.models.MaterialListQuery;
import com.aidigital.aionboarding.service.material.models.MaterialRecord;
import com.aidigital.aionboarding.service.material.models.MaterialSearchSummaryRecord;
import org.springframework.data.domain.Page;

import java.util.Collection;
import java.util.List;

/**
 * Loads material records with related assets, usage counts, and delete guards.
 */
public interface MaterialRecordQueryService {

    /**
     * Loads a bounded, sorted page of material search summaries matching the given filter — full
     * body text replaced by a presence flag, link/attachment children carrying only their preview
     * fields — with related assets and lesson usage counts batch-loaded only for the materials in
     * that page.
     *
     * @param query typed filter and sort parameters
     * @param page  zero-based page index
     * @param size  maximum number of materials per page
     * @return a page of assembled material search summaries
     */
    Page<MaterialSearchSummaryRecord> loadMaterialSummaries(MaterialListQuery query, int page, int size);

    /**
     * Counts materials matching the given filter, without fetching or paginating any rows.
     *
     * @param query typed filter and sort parameters
     * @return the number of materials matching the filter
     */
    long countSummaries(MaterialListQuery query);

    /**
     * Loads material records for exactly the given identifiers, batch-fetching related assets and
     * usage counts only for those identifiers.
     *
     * @param materialIds material identifiers to resolve; {@code null} and duplicate values are ignored
     * @return matching material records, unordered
     */
    List<MaterialRecord> loadMaterialRecordsByIds(Collection<Long> materialIds);

    /**
     * Builds a material record for a single persisted material entity.
     *
     * @param material persisted material entity
     * @param usageCount number of lessons referencing the material
     * @return assembled material record
     */
    MaterialRecord loadRecord(Material material, long usageCount);

    /**
     * Returns how many lessons reference the given material.
     *
     * @param materialId material identifier
     * @return lesson usage count
     */
    long countLessonUsage(Long materialId);

    /**
     * Ensures a material can be deleted because it is not referenced by any lesson.
     *
     * @param materialId material identifier
     * @throws com.aidigital.aionboarding.service.common.error.AppException {@link com.aidigital.aionboarding.service.common.error.ErrorReason#C006}
     *         when the material is still used by one or more lessons
     */
    void requireDeletable(Long materialId);
}
