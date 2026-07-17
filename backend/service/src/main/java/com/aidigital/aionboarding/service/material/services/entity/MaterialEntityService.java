package com.aidigital.aionboarding.service.material.services.entity;

import com.aidigital.aionboarding.domain.material.entities.Material;
import com.aidigital.aionboarding.domain.material.repositories.MaterialRepository;
import com.aidigital.aionboarding.domain.material.repositories.MaterialSearchSummaryProjection;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.material.models.MaterialListQuery;
import com.aidigital.aionboarding.service.material.support.MaterialSpecificationBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Short-transaction CRUD helpers for the {@link Material} entity.
 * <p>
 * This is the only service that may inject {@link MaterialRepository} directly.
 * All other services that require material data must depend on this service.
 */
@Service
@RequiredArgsConstructor
public class MaterialEntityService {

	private final MaterialRepository materialRepository;
	private final MaterialSpecificationBuilder materialSpecificationBuilder;

	/**
	 * Loads a single material by primary key, throwing if it does not exist.
	 *
	 * @param materialId the material primary key
	 * @return the {@link Material} entity
	 * @throws AppException C001 if no material with the given ID exists
	 */
	@Transactional(readOnly = true)
	public Material getReference(Long materialId) {
		return materialRepository.findById(materialId)
				.orElseThrow(() -> new AppException(ErrorReason.C001, materialId));
	}

	/**
	 * Loads a single material by primary key, returning {@code null} if it does not exist.
	 *
	 * @param materialId the material primary key
	 * @return the {@link Material} entity, or {@code null} if not found
	 */
	@Transactional(readOnly = true)
	public Material findById(Long materialId) {
		return materialRepository.findById(materialId).orElse(null);
	}

	/**
	 * Batch-loads materials by their IDs, preserving the caller's requested order and
	 * silently skipping any IDs that no longer exist.
	 *
	 * @param materialIds ordered list of material IDs to load
	 * @return list of {@link Material} entities in the same order as the input IDs
	 */
	@Transactional(readOnly = true)
	public List<Material> findAllById(List<Long> materialIds) {
		if (materialIds == null || materialIds.isEmpty()) {
			return List.of();
		}
		Map<Long, Material> byId = materialRepository.findAllById(materialIds).stream()
				.collect(Collectors.toMap(Material::getId, m -> m));
		return materialIds.stream().map(byId::get).filter(Objects::nonNull).toList();
	}

	/**
	 * Searches materials with the given typed filters, returning a bounded, sorted page of lean
	 * summary projections — full body text replaced by a presence flag, with no per-child
	 * association data selected.
	 *
	 * @param filter typed filter and sort parameters
	 * @param page   zero-based page index
	 * @param size   maximum number of materials per page
	 * @return a page of {@link MaterialSearchSummaryProjection} matching the filter
	 */
	@Transactional(readOnly = true)
	public Page<MaterialSearchSummaryProjection> searchSummaries(MaterialListQuery filter, int page, int size) {
		Specification<Material> specification = materialSpecificationBuilder.build(filter);
		Pageable pageable = PageRequest.of(page, size);
		return materialRepository.searchSummaries(specification, pageable);
	}

	/**
	 * Counts materials matching the given typed filters, without fetching or paginating any
	 * rows — for cheap tab-count display independent of the active Library tab.
	 *
	 * @param filter typed filter and sort parameters
	 * @return the number of materials matching the filter
	 */
	@Transactional(readOnly = true)
	public long countSummaries(MaterialListQuery filter) {
		Specification<Material> specification = materialSpecificationBuilder.build(filter);
		return materialRepository.count(specification);
	}
}
