package com.aidigital.aionboarding.service.material.services;

import com.aidigital.aionboarding.domain.material.entities.Material;
import com.aidigital.aionboarding.domain.material.entities.MaterialLink;
import com.aidigital.aionboarding.domain.material.repositories.MaterialLinkSummaryProjection;

import java.util.Collection;
import java.util.List;

/**
 * Persists link rows for materials with fetched Open Graph metadata.
 */
public interface MaterialLinkService {

	/**
	 * Fetches Open Graph metadata for each URL without persisting anything.
	 * Safe to call outside a database transaction.
	 *
	 * @param urls link URLs to prefetch
	 * @return list of prepared records containing fetched metadata
	 */
	List<PreparedLinkRecord> prepareLinks(List<String> urls);

	/**
	 * Persists pre-fetched link records for the given material.
	 * Must be called inside a database transaction.
	 *
	 * @param material parent material entity
	 * @param records  pre-fetched link records to persist
	 */
	void saveLinks(Material material, List<PreparedLinkRecord> records);

	/**
	 * Deletes all link rows for the given material.
	 *
	 * @param materialId material identifier
	 */
	void deleteByMaterialId(Long materialId);

	/**
	 * Returns all link rows for the given material, ordered by sort order ascending.
	 *
	 * @param materialId material identifier
	 * @return links for the material, in sort order
	 */
	List<MaterialLink> findByMaterialIdOrderBySortOrderAsc(Long materialId);

	/**
	 * Returns link rows for exactly the given materials, ordered by sort order ascending.
	 *
	 * @param materialIds material identifiers to load rows for
	 * @return links for the given materials, in sort order
	 */
	List<MaterialLink> findByMaterialIdsOrderBySortOrderAsc(Collection<Long> materialIds);

	/**
	 * Returns bounded link preview projections for the given materials, omitting the extracted
	 * page text and metadata error a Library search card does not need.
	 *
	 * @param materialIds material identifiers to load previews for
	 * @return link previews for the given materials, in sort order
	 */
	List<MaterialLinkSummaryProjection> findSummariesByMaterialIds(Collection<Long> materialIds);

	/**
	 * Pre-fetched Open Graph metadata for a single link URL.
	 */
	record PreparedLinkRecord(
			String url,
			String title,
			String description,
			String imageUrl,
			String siteName,
			String extractedText,
			String metadataError
	) {

	}
}
