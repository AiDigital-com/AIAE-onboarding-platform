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
import com.aidigital.aionboarding.service.lesson.services.entity.LessonEntityService;
import com.aidigital.aionboarding.service.mappers.material.MaterialMapper;
import com.aidigital.aionboarding.service.material.models.MaterialListQuery;
import com.aidigital.aionboarding.service.material.models.MaterialRecord;
import com.aidigital.aionboarding.service.material.models.MaterialSearchSummaryRecord;
import com.aidigital.aionboarding.service.material.models.MaterialSortField;
import com.aidigital.aionboarding.service.material.services.MaterialFileService;
import com.aidigital.aionboarding.service.material.services.MaterialLinkService;
import com.aidigital.aionboarding.service.material.services.MaterialYoutubeService;
import com.aidigital.aionboarding.service.material.services.entity.MaterialEntityService;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.instancio.Select.field;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialRecordQueryServiceImplTest {

	@Mock
	private MaterialEntityService materialEntityService;
	@Mock
	private MaterialYoutubeService materialYoutubeService;
	@Mock
	private MaterialLinkService materialLinkService;
	@Mock
	private MaterialFileService materialFileService;
	@Mock
	private LessonEntityService lessonEntityService;
	@Mock
	private MaterialMapper materialMapper;

	@InjectMocks
	private MaterialRecordQueryServiceImpl service;

	@Test
	void loadMaterialSummariesShouldBatchLoadChildrenOnlyForPageMaterialIdsTest() {
		// Given:
		MaterialSearchSummaryProjection summary = Instancio.of(MaterialSearchSummaryProjection.class)
				.set(field(MaterialSearchSummaryProjection::id), 50L)
				.create();
		MaterialListQuery query = new MaterialListQuery(
				null, null, null, null, null, null, MaterialSortField.CREATED_AT, Sort.Direction.DESC
		);
		Page<MaterialSearchSummaryProjection> summariesPage = new PageImpl<>(List.of(summary), PageRequest.of(0, 20),
				1);
		Material material = Instancio.of(Material.class).set(field(Material::getId), 50L).create();
		MaterialYoutubeUrl youtubeUrl = Instancio.create(MaterialYoutubeUrl.class);
		youtubeUrl.setMaterial(material);
		MaterialLinkSummaryProjection linkPreview = Instancio.of(MaterialLinkSummaryProjection.class)
				.set(field(MaterialLinkSummaryProjection::materialId), 50L)
				.create();
		MaterialFileSummaryProjection fileSummary = Instancio.of(MaterialFileSummaryProjection.class)
				.set(field(MaterialFileSummaryProjection::materialId), 50L)
				.create();
		List<MaterialYoutubeUrl> youtubeUrls = List.of(youtubeUrl);
		List<MaterialLinkSummaryProjection> linkPreviews = List.of(linkPreview);
		List<MaterialFileSummaryProjection> fileSummaries = List.of(fileSummary);
		MaterialSearchSummaryRecord expected = Instancio.create(MaterialSearchSummaryRecord.class);

		when(materialEntityService.searchSummaries(query, 0, 20)).thenReturn(summariesPage);
		when(materialYoutubeService.findByMaterialIdsOrderBySortOrderAsc(List.of(50L))).thenReturn(youtubeUrls);
		when(materialLinkService.findSummariesByMaterialIds(List.of(50L))).thenReturn(linkPreviews);
		when(materialFileService.findSummariesByMaterialIds(List.of(50L))).thenReturn(fileSummaries);
		when(lessonEntityService.findLessonUsageCounts(List.of(50L)))
				.thenReturn(List.of(new MaterialUsageCount(50L, 3L)));
		when(materialMapper.toSearchSummaryRecord(summary, youtubeUrls, linkPreviews, fileSummaries, 3L)).thenReturn(expected);

		// When:
		Page<MaterialSearchSummaryRecord> result = service.loadMaterialSummaries(query, 0, 20);

		// Then:
		assertThat(result.getContent()).containsExactly(expected);
		assertThat(result.getTotalElements()).isEqualTo(1);
		verify(materialYoutubeService).findByMaterialIdsOrderBySortOrderAsc(List.of(50L));
		verify(materialLinkService).findSummariesByMaterialIds(List.of(50L));
		verify(materialFileService).findSummariesByMaterialIds(List.of(50L));
	}

	@Test
	void countSummariesShouldDelegateToEntityServiceTest() {
		// Given:
		MaterialListQuery query = new MaterialListQuery(
				null, null, null, null, null, null, MaterialSortField.CREATED_AT, Sort.Direction.DESC
		);
		when(materialEntityService.countSummaries(query)).thenReturn(5L);

		// When:
		long result = service.countSummaries(query);

		// Then:
		assertThat(result).isEqualTo(5L);
	}

	@Test
	void loadMaterialRecordsByIdsShouldReturnEmptyListWithoutQueryingWhenNoIdsGivenTest() {
		// When:
		List<MaterialRecord> result = service.loadMaterialRecordsByIds(List.of());

		// Then:
		assertThat(result).isEmpty();
	}

	@Test
	void loadRecordShouldAssembleFromTheThreeChildServicesTest() {
		// Given:
		Material material = Instancio.of(Material.class).set(field(Material::getId), 10L).create();
		List<MaterialYoutubeUrl> youtubeUrls = List.of(Instancio.create(MaterialYoutubeUrl.class));
		List<MaterialLink> links = List.of(Instancio.create(MaterialLink.class));
		List<MaterialFile> files = List.of(Instancio.create(MaterialFile.class));
		MaterialRecord expected = Instancio.create(MaterialRecord.class);
		when(materialYoutubeService.findByMaterialIdOrderBySortOrderAsc(10L)).thenReturn(youtubeUrls);
		when(materialLinkService.findByMaterialIdOrderBySortOrderAsc(10L)).thenReturn(links);
		when(materialFileService.findByMaterialIdOrderByCreatedAtAsc(10L)).thenReturn(files);
		when(materialMapper.toRecord(material, youtubeUrls, links, files, 7L)).thenReturn(expected);

		// When:
		MaterialRecord result = service.loadRecord(material, 7L);

		// Then:
		assertThat(result).isSameAs(expected);
		ArgumentCaptor<List<MaterialYoutubeUrl>> youtubeCaptor = ArgumentCaptor.forClass(List.class);
		ArgumentCaptor<List<MaterialLink>> linkCaptor = ArgumentCaptor.forClass(List.class);
		ArgumentCaptor<List<MaterialFile>> fileCaptor = ArgumentCaptor.forClass(List.class);
		verify(materialMapper).toRecord(
				org.mockito.ArgumentMatchers.eq(material),
				youtubeCaptor.capture(),
				linkCaptor.capture(),
				fileCaptor.capture(),
				org.mockito.ArgumentMatchers.eq(7L)
		);
		assertThat(youtubeCaptor.getValue()).isSameAs(youtubeUrls);
		assertThat(linkCaptor.getValue()).isSameAs(links);
		assertThat(fileCaptor.getValue()).isSameAs(files);
	}

	@Test
	void countLessonUsageShouldDelegateToLessonEntityServiceTest() {
		// Given:
		Long materialId = 20L;
		when(lessonEntityService.countLessonUsage(materialId)).thenReturn(4L);

		// When:
		long result = service.countLessonUsage(materialId);

		// Then:
		assertThat(result).isEqualTo(4L);
		verify(lessonEntityService).countLessonUsage(materialId);
	}

	@Test
	void requireDeletableShouldNotThrowWhenUsageCountIsZeroTest() {
		// Given:
		Long materialId = 30L;
		when(lessonEntityService.countLessonUsage(materialId)).thenReturn(0L);

		// When:
		service.requireDeletable(materialId);

		// Then:
		verify(lessonEntityService, never()).findLessonTitlesByMaterialId(
				org.mockito.ArgumentMatchers.eq(materialId),
				org.mockito.ArgumentMatchers.any()
		);
	}

	@Test
	void requireDeletableShouldThrowConflictWithLessonTitlesWhenUsedTest() {
		// Given:
		Long materialId = 40L;
		when(lessonEntityService.countLessonUsage(materialId)).thenReturn(2L);
		when(lessonEntityService.findLessonTitlesByMaterialId(materialId, PageRequest.of(0, 3)))
				.thenReturn(List.of("Lesson A", "Lesson B"));

		// When-Then:
		assertThatThrownBy(() -> service.requireDeletable(materialId))
				.isInstanceOf(AppException.class)
				.satisfies(ex -> assertThat(((AppException) ex).getMessage())
						.contains("Lesson A")
						.contains("Lesson B"));
	}
}
