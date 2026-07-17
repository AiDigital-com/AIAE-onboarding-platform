package com.aidigital.aionboarding.service.material.services.impl;

import com.aidigital.aionboarding.domain.lesson.models.MaterialUsageCount;
import com.aidigital.aionboarding.domain.material.entities.Material;
import com.aidigital.aionboarding.domain.material.entities.MaterialFile;
import com.aidigital.aionboarding.domain.material.entities.MaterialLink;
import com.aidigital.aionboarding.domain.material.entities.MaterialYoutubeUrl;
import com.aidigital.aionboarding.domain.material.repositories.MaterialFileSummaryProjection;
import com.aidigital.aionboarding.domain.material.repositories.MaterialLinkSummaryProjection;
import com.aidigital.aionboarding.domain.material.repositories.MaterialSearchSummaryProjection;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonEntityService;
import com.aidigital.aionboarding.service.mappers.material.MaterialMapper;
import com.aidigital.aionboarding.service.material.models.MaterialListQuery;
import com.aidigital.aionboarding.service.material.models.MaterialRecord;
import com.aidigital.aionboarding.service.material.models.MaterialSearchSummaryRecord;
import com.aidigital.aionboarding.service.material.services.MaterialFileService;
import com.aidigital.aionboarding.service.material.services.MaterialLinkService;
import com.aidigital.aionboarding.service.material.services.MaterialRecordQueryService;
import com.aidigital.aionboarding.service.material.services.MaterialYoutubeService;
import com.aidigital.aionboarding.service.material.services.entity.MaterialEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MaterialRecordQueryServiceImpl implements MaterialRecordQueryService {

	private static final int LESSON_USAGE_TITLE_LIMIT = 3;

	private final MaterialEntityService materialEntityService;
	private final MaterialYoutubeService materialYoutubeService;
	private final MaterialLinkService materialLinkService;
	private final MaterialFileService materialFileService;
	private final LessonEntityService lessonEntityService;
	private final MaterialMapper materialMapper;

	@Override
	public Page<MaterialSearchSummaryRecord> loadMaterialSummaries(MaterialListQuery query, int page, int size) {
		Page<MaterialSearchSummaryProjection> summariesPage = materialEntityService.searchSummaries(query, page, size);
		List<MaterialSearchSummaryProjection> summaries = summariesPage.getContent();
		List<Long> materialIds = summaries.stream().map(MaterialSearchSummaryProjection::id).toList();

		List<MaterialSearchSummaryRecord> records = assembleSummaries(summaries, materialIds);
		return new PageImpl<>(records, summariesPage.getPageable(), summariesPage.getTotalElements());
	}

	@Override
	public long countSummaries(MaterialListQuery query) {
		return materialEntityService.countSummaries(query);
	}

	List<MaterialSearchSummaryRecord> assembleSummaries(List<MaterialSearchSummaryProjection> summaries,
														Collection<Long> materialIds) {
		Map<Long, List<MaterialYoutubeUrl>> youtubeByMaterialId = groupYoutubeUrls(materialIds);
		Map<Long, List<MaterialLinkSummaryProjection>> linkPreviewsByMaterialId = groupLinkPreviews(materialIds);
		Map<Long, List<MaterialFileSummaryProjection>> fileSummariesByMaterialId = groupFileSummaries(materialIds);
		Map<Long, Long> usageCountByMaterialId = loadUsageCounts(materialIds);

		return summaries.stream()
				.map(summary -> materialMapper.toSearchSummaryRecord(
						summary,
						youtubeByMaterialId.getOrDefault(summary.id(), List.of()),
						linkPreviewsByMaterialId.getOrDefault(summary.id(), List.of()),
						fileSummariesByMaterialId.getOrDefault(summary.id(), List.of()),
						usageCountByMaterialId.getOrDefault(summary.id(), 0L)
				))
				.toList();
	}

	@Override
	public List<MaterialRecord> loadMaterialRecordsByIds(Collection<Long> materialIds) {
		List<Long> uniqueIds = materialIds == null
				? List.of()
				: materialIds.stream().filter(Objects::nonNull).distinct().toList();
		if (uniqueIds.isEmpty()) {
			return List.of();
		}
		List<Material> materials = materialEntityService.findAllById(uniqueIds);
		return assembleRecords(materials, uniqueIds);
	}

	List<MaterialRecord> assembleRecords(List<Material> materials, Collection<Long> materialIds) {
		Map<Long, List<MaterialYoutubeUrl>> youtubeByMaterialId = groupYoutubeUrls(materialIds);
		Map<Long, List<MaterialLink>> linksByMaterialId = groupLinks(materialIds);
		Map<Long, List<MaterialFile>> filesByMaterialId = groupFiles(materialIds);
		Map<Long, Long> usageCountByMaterialId = loadUsageCounts(materialIds);

		return materials.stream()
				.map(material -> materialMapper.toRecord(
						material,
						youtubeByMaterialId.getOrDefault(material.getId(), List.of()),
						linksByMaterialId.getOrDefault(material.getId(), List.of()),
						filesByMaterialId.getOrDefault(material.getId(), List.of()),
						usageCountByMaterialId.getOrDefault(material.getId(), 0L)
				))
				.toList();
	}

	@Override
	public MaterialRecord loadRecord(Material material, long usageCount) {
		return materialMapper.toRecord(
				material,
				materialYoutubeService.findByMaterialIdOrderBySortOrderAsc(material.getId()),
				materialLinkService.findByMaterialIdOrderBySortOrderAsc(material.getId()),
				materialFileService.findByMaterialIdOrderByCreatedAtAsc(material.getId()),
				usageCount
		);
	}

	@Override
	public long countLessonUsage(Long materialId) {
		return lessonEntityService.countLessonUsage(materialId);
	}

	@Override
	public void requireDeletable(Long materialId) {
		long lessonUsageCount = lessonEntityService.countLessonUsage(materialId);
		if (lessonUsageCount > 0) {
			List<String> lessonTitles = lessonEntityService
					.findLessonTitlesByMaterialId(materialId, PageRequest.of(0, LESSON_USAGE_TITLE_LIMIT))
					.stream()
					.filter(title -> title != null && !title.isBlank())
					.toList();
			String titleList = lessonTitles.stream()
					.map(title -> "\"" + title + "\"")
					.collect(Collectors.joining(", "));
			String suffix = lessonUsageCount > lessonTitles.size() ? ", and more" : "";
			throw new AppException(
					ErrorReason.C006,
					"This material cannot be deleted because it is used in " + titleList + suffix
							+ ". Delete or update those lessons first."
			);
		}
	}

	Map<Long, List<MaterialYoutubeUrl>> groupYoutubeUrls(Collection<Long> materialIds) {
		return materialYoutubeService.findByMaterialIdsOrderBySortOrderAsc(materialIds).stream()
				.sorted(Comparator.comparing(MaterialYoutubeUrl::getSortOrder))
				.collect(Collectors.groupingBy(
						item -> item.getMaterial().getId(),
						LinkedHashMap::new,
						Collectors.toList()
				));
	}

	Map<Long, List<MaterialLink>> groupLinks(Collection<Long> materialIds) {
		return materialLinkService.findByMaterialIdsOrderBySortOrderAsc(materialIds).stream()
				.sorted(Comparator.comparing(MaterialLink::getSortOrder))
				.collect(Collectors.groupingBy(
						item -> item.getMaterial().getId(),
						LinkedHashMap::new,
						Collectors.toList()
				));
	}

	Map<Long, List<MaterialLinkSummaryProjection>> groupLinkPreviews(Collection<Long> materialIds) {
		return materialLinkService.findSummariesByMaterialIds(materialIds).stream()
				.collect(Collectors.groupingBy(
						MaterialLinkSummaryProjection::materialId,
						LinkedHashMap::new,
						Collectors.toList()
				));
	}

	Map<Long, List<MaterialFileSummaryProjection>> groupFileSummaries(Collection<Long> materialIds) {
		return materialFileService.findSummariesByMaterialIds(materialIds).stream()
				.collect(Collectors.groupingBy(
						MaterialFileSummaryProjection::materialId,
						LinkedHashMap::new,
						Collectors.toList()
				));
	}

	Map<Long, List<MaterialFile>> groupFiles(Collection<Long> materialIds) {
		return materialFileService.findByMaterialIdsOrderByCreatedAtAsc(materialIds).stream()
				.sorted(Comparator.comparing(MaterialFile::getCreatedAt,
						Comparator.nullsLast(Comparator.naturalOrder())))
				.collect(Collectors.groupingBy(
						item -> item.getMaterial().getId(),
						LinkedHashMap::new,
						Collectors.toList()
				));
	}

	Map<Long, Long> loadUsageCounts(Collection<Long> materialIds) {
		Map<Long, Long> usageCounts = new HashMap<>();
		for (MaterialUsageCount usageCount : lessonEntityService.findLessonUsageCounts(materialIds)) {
			usageCounts.put(usageCount.materialId(), usageCount.count());
		}
		return usageCounts;
	}
}
