package com.aidigital.aionboarding.domain.material.repositories;

import com.aidigital.aionboarding.domain.material.entities.MaterialLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface MaterialLinkRepository extends JpaRepository<MaterialLink, Long> {

	@Query("SELECT ml FROM MaterialLink ml WHERE ml.material.id = :materialId ORDER BY ml.sortOrder ASC")
	java.util.List<MaterialLink> findByMaterialIdOrderBySortOrderAsc(@Param("materialId") Long materialId);

	@Query("SELECT ml FROM MaterialLink ml WHERE ml.material.id IN :materialIds ORDER BY ml.material.id ASC, ml" +
			".sortOrder ASC")
	java.util.List<MaterialLink> findByMaterialIdInOrderBySortOrderAsc(@Param("materialIds") Collection<Long> materialIds);

	void deleteByMaterial_Id(Long materialId);

	/**
	 * Returns bounded link preview projections for the given materials, omitting the extracted
	 * page text and metadata error a Library search card does not need.
	 *
	 * @param materialIds material identifiers to restrict to
	 * @return link previews ordered by material id, then sort order
	 */
	@Query("""
			SELECT NEW com.aidigital.aionboarding.domain.material.repositories.MaterialLinkSummaryProjection(
			    ml.material.id, ml.url, ml.title, ml.description, ml.imageUrl, ml.siteName
			)
			FROM MaterialLink ml
			WHERE ml.material.id IN :materialIds
			ORDER BY ml.material.id ASC, ml.sortOrder ASC
			""")
	java.util.List<MaterialLinkSummaryProjection> findSummariesByMaterialIdIn(@Param("materialIds") Collection<Long> materialIds);
}
