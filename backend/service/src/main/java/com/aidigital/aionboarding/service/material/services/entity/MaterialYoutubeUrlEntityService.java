package com.aidigital.aionboarding.service.material.services.entity;

import com.aidigital.aionboarding.domain.material.entities.MaterialYoutubeUrl;
import com.aidigital.aionboarding.domain.material.repositories.MaterialYoutubeUrlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * Short-transaction CRUD helpers for the {@link MaterialYoutubeUrl} entity.
 * <p>
 * This is the only service that may inject {@link MaterialYoutubeUrlRepository} directly.
 * All other services that require YouTube-URL row data must depend on this service.
 * <p>
 * Each public method opens and closes its own transaction so an orchestrator (e.g.
 * {@code MaterialYoutubeServiceImpl}) can call {@link #claimMissingMetadataBatch(int)},
 * then make an external oEmbed call with no transaction open, then call
 * {@link #save(MaterialYoutubeUrl)} once the call returns — the shape
 * {@code .claude/rules/14-performance.md} requires for work spanning a third-party call.
 * Calling these methods from within the same class would not achieve this: Spring's
 * {@code @Transactional} proxy only intercepts calls made through the managed bean
 * reference, so the claim and the save must live on a different bean than the loop that
 * makes the external call, which is exactly why this class exists as a separate collaborator.
 */
@Service
@RequiredArgsConstructor
public class MaterialYoutubeUrlEntityService {

	private final MaterialYoutubeUrlRepository materialYoutubeUrlRepository;

	/**
	 * Returns all YouTube URL rows for a material, ordered by sort order ascending.
	 *
	 * @param materialId the material primary key
	 * @return YouTube URL rows for the material, in sort order
	 */
	@Transactional(readOnly = true)
	public List<MaterialYoutubeUrl> findByMaterialIdOrderBySortOrderAsc(Long materialId) {
		return materialYoutubeUrlRepository.findByMaterialIdOrderBySortOrderAsc(materialId);
	}

	/**
	 * Returns YouTube URL rows for exactly the given materials, ordered by sort order ascending.
	 *
	 * @param materialIds material identifiers to load rows for
	 * @return YouTube URL rows for the given materials, in sort order
	 */
	@Transactional(readOnly = true)
	public List<MaterialYoutubeUrl> findByMaterialIdsOrderBySortOrderAsc(Collection<Long> materialIds) {
		return materialYoutubeUrlRepository.findByMaterialIdInOrderBySortOrderAsc(materialIds);
	}

	/**
	 * Deletes all YouTube URL rows for the given material.
	 *
	 * @param materialId material identifier
	 */
	@Transactional
	public void deleteByMaterialId(Long materialId) {
		materialYoutubeUrlRepository.deleteByMaterial_Id(materialId);
	}

	/**
	 * Persists a YouTube URL row.
	 *
	 * @param entity the row to persist
	 * @return the saved {@link MaterialYoutubeUrl}
	 */
	@Transactional
	public MaterialYoutubeUrl save(MaterialYoutubeUrl entity) {
		return materialYoutubeUrlRepository.save(entity);
	}

	/**
	 * Atomically claims a bounded batch of rows still missing oEmbed metadata. See
	 * {@link MaterialYoutubeUrlRepository#claimMissingMetadataBatch(int)} for the locking
	 * shape. The returned rows are detached once this method returns; callers may read their
	 * plain columns (e.g. {@code getUrl()}) freely but must not traverse the lazy
	 * {@code material} association outside a transaction.
	 *
	 * @param limit maximum number of rows to claim
	 * @return claimed rows, never more than {@code limit}
	 */
	@Transactional
	public List<MaterialYoutubeUrl> claimMissingMetadataBatch(int limit) {
		return materialYoutubeUrlRepository.claimMissingMetadataBatch(limit);
	}
}
