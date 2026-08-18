package com.aidigital.aionboarding.service.material.services.impl;

import com.aidigital.aionboarding.domain.material.entities.Material;
import com.aidigital.aionboarding.domain.material.entities.MaterialLink;
import com.aidigital.aionboarding.domain.material.repositories.MaterialLinkRepository;
import com.aidigital.aionboarding.domain.material.repositories.MaterialLinkSummaryProjection;
import com.aidigital.aionboarding.service.common.mapping.TextValueNormalizer;
import com.aidigital.aionboarding.service.link.models.LinkMetadataRecord;
import com.aidigital.aionboarding.service.link.services.LinkMetadataService;
import com.aidigital.aionboarding.service.mappers.material.MaterialMapper;
import com.aidigital.aionboarding.service.material.services.MaterialLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
			LinkMetadataRecord metadata = linkMetadataService.fetch(url);
			records.add(new PreparedLinkRecord(
					url,
					textValueNormalizer.raw(metadata.title()),
					textValueNormalizer.raw(metadata.description()),
					textValueNormalizer.raw(metadata.imageUrl()),
					textValueNormalizer.raw(metadata.siteName()),
					textValueNormalizer.raw(metadata.extractedText()),
					textValueNormalizer.raw(metadata.error())
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
