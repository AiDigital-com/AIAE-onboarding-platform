package com.aidigital.aionboarding.domain.material.repositories;

import com.aidigital.aionboarding.domain.material.entities.Material;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

/**
 * Criteria-API-backed queries for {@code MaterialRepository} that a derived or JPQL {@code @Query}
 * method cannot express.
 */
public interface MaterialRepositoryCustom {

	/**
	 * Searches materials matching the given specification, projecting only the bounded summary
	 * fields a Library search card needs instead of the full entity.
	 *
	 * @param specification dynamic filter and sort predicate, built the same way as the
	 *                      full-entity search
	 * @param pageable      zero-based page index and page size
	 * @return a page of {@link MaterialSearchSummaryProjection}
	 */
	Page<MaterialSearchSummaryProjection> searchSummaries(Specification<Material> specification, Pageable pageable);
}
