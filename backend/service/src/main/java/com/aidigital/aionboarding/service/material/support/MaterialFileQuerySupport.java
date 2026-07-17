package com.aidigital.aionboarding.service.material.support;

import com.aidigital.aionboarding.domain.material.entities.MaterialFile;
import com.aidigital.aionboarding.domain.material.repositories.MaterialFileRepository;
import com.aidigital.aionboarding.service.common.mapping.TextValueNormalizer;
import com.aidigital.aionboarding.service.mappers.material.MaterialMapper;
import com.aidigital.aionboarding.service.material.models.MaterialAttachmentInput;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Read/query passthroughs for {@link MaterialFile} rows, extracted from
 * {@code MaterialFileServiceImpl} to keep that service under the line-count gate.
 */
@Component
@RequiredArgsConstructor
public class MaterialFileQuerySupport {

	private final MaterialFileRepository materialFileRepository;
	private final MaterialMapper materialMapper;
	private final TextValueNormalizer textValueNormalizer;

	/**
	 * Returns non-blank storage keys currently associated with a material.
	 *
	 * @param materialId material identifier
	 * @return storage keys attached to the material
	 */
	public List<String> collectStorageKeys(Long materialId) {
		return materialFileRepository.findByMaterialId(materialId).stream()
				.map(MaterialFile::getStorageKey)
				.filter(key -> key != null && !key.isBlank())
				.toList();
	}

	/**
	 * Returns storage keys present before an update but absent from the new attachment list.
	 *
	 * @param existingStorageKeys storage keys captured before related rows were replaced
	 * @param attachments         new attachment inputs
	 * @return storage keys that should be removed from object storage
	 */
	public List<String> findRemovedStorageKeys(
			List<String> existingStorageKeys,
			List<MaterialAttachmentInput> attachments
	) {
		Set<String> keptStorageKeys = attachments.stream()
				.map(attachment -> textValueNormalizer.trimmed(attachment.storageKey()))
				.filter(key -> !key.isBlank())
				.collect(Collectors.toSet());
		return existingStorageKeys.stream()
				.filter(key -> !keptStorageKeys.contains(key))
				.toList();
	}

	/**
	 * Returns the persisted file attachments (mapped to {@link MaterialAttachmentInput}) for all given
	 * material IDs, preserving per-material {@code createdAt} order. An empty or null input returns an
	 * empty list.
	 *
	 * @param materialIds material identifiers to query
	 * @return list of attachment inputs, in per-material creation order
	 */
	public List<MaterialAttachmentInput> findAttachmentsForMaterials(List<Long> materialIds) {
		if (materialIds == null || materialIds.isEmpty()) {
			return List.of();
		}
		List<MaterialAttachmentInput> result = new ArrayList<>();
		for (Long materialId : materialIds) {
			if (materialId == null) {
				continue;
			}
			for (MaterialFile file : materialFileRepository.findByMaterialIdOrderByCreatedAtAsc(materialId)) {
				result.add(materialMapper.toAttachmentInput(file));
			}
		}
		return result;
	}

	/**
	 * Returns all file rows for the given material, joined with their kind.
	 *
	 * @param materialId material identifier
	 * @return file attachments for the material, unordered
	 */
	public List<MaterialFile> findByMaterialId(Long materialId) {
		return materialFileRepository.findByMaterialId(materialId);
	}

	/**
	 * Returns all file rows for the given material, ordered by creation time ascending.
	 *
	 * @param materialId material identifier
	 * @return file attachments for the material, oldest first
	 */
	public List<MaterialFile> findByMaterialIdOrderByCreatedAtAsc(Long materialId) {
		return materialFileRepository.findByMaterialIdOrderByCreatedAtAsc(materialId);
	}

	/**
	 * Returns file rows for exactly the given materials, ordered by creation time ascending.
	 *
	 * @param materialIds material identifiers to load rows for
	 * @return file attachments for the given materials, oldest first
	 */
	public List<MaterialFile> findByMaterialIdsOrderByCreatedAtAsc(Collection<Long> materialIds) {
		if (materialIds == null || materialIds.isEmpty()) {
			return List.of();
		}
		return materialFileRepository.findByMaterialIdInOrderByCreatedAtAsc(materialIds);
	}

}
