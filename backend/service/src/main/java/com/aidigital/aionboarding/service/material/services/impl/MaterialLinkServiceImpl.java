package com.aidigital.aionboarding.service.material.services.impl;

import com.aidigital.aionboarding.domain.material.entities.Material;
import com.aidigital.aionboarding.domain.material.entities.MaterialLink;
import com.aidigital.aionboarding.domain.material.repositories.MaterialLinkRepository;
import com.aidigital.aionboarding.domain.material.repositories.MaterialLinkSummaryProjection;
import com.aidigital.aionboarding.service.common.mapping.TextValueNormalizer;
import com.aidigital.aionboarding.service.link.services.LinkMetadataService;
import com.aidigital.aionboarding.service.mappers.material.MaterialMapper;
import com.aidigital.aionboarding.service.material.services.MaterialLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MaterialLinkServiceImpl implements MaterialLinkService {

	private final MaterialLinkRepository materialLinkRepository;
	private final LinkMetadataService linkMetadataService;
	private final TextValueNormalizer textValueNormalizer;
	private final MaterialMapper materialMapper;

	@Override
	public List<PreparedLinkRecord> prepareLinks(List<String> urls) {
		List<PreparedLinkRecord> records = new ArrayList<>();
		for (String url : urls) {
			Map<String, Object> metadata = linkMetadataService.fetch(url);
			records.add(new PreparedLinkRecord(
					url,
					textValueNormalizer.raw(metadata.get("title")),
					textValueNormalizer.raw(metadata.get("description")),
					textValueNormalizer.raw(metadata.get("imageUrl")),
					textValueNormalizer.raw(metadata.get("siteName")),
					textValueNormalizer.raw(metadata.get("extractedText")),
					textValueNormalizer.raw(metadata.get("error"))
			));
		}
		return records;
	}

	@Override
	public void saveLinks(Material material, List<PreparedLinkRecord> records) {
		for (int index = 0; index < records.size(); index += 1) {
			PreparedLinkRecord record = records.get(index);
			MaterialLink entity = materialMapper.toNewMaterialLink(material, record, index);
			materialLinkRepository.save(entity);
		}
	}

	@Override
	public void deleteByMaterialId(Long materialId) {
		materialLinkRepository.deleteByMaterial_Id(materialId);
	}

	@Override
	public List<MaterialLink> findByMaterialIdOrderBySortOrderAsc(Long materialId) {
		return materialLinkRepository.findByMaterialIdOrderBySortOrderAsc(materialId);
	}

	@Override
	public List<MaterialLink> findByMaterialIdsOrderBySortOrderAsc(Collection<Long> materialIds) {
		if (materialIds == null || materialIds.isEmpty()) {
			return List.of();
		}
		return materialLinkRepository.findByMaterialIdInOrderBySortOrderAsc(materialIds);
	}

	@Override
	public List<MaterialLinkSummaryProjection> findSummariesByMaterialIds(Collection<Long> materialIds) {
		if (materialIds == null || materialIds.isEmpty()) {
			return List.of();
		}
		return materialLinkRepository.findSummariesByMaterialIdIn(materialIds);
	}

}
