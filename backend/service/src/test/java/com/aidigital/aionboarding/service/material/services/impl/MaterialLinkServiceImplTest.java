package com.aidigital.aionboarding.service.material.services.impl;

import com.aidigital.aionboarding.domain.material.entities.Material;
import com.aidigital.aionboarding.domain.material.entities.MaterialLink;
import com.aidigital.aionboarding.domain.material.repositories.MaterialLinkRepository;
import com.aidigital.aionboarding.service.common.mapping.TextValueNormalizer;
import com.aidigital.aionboarding.service.link.services.LinkMetadataService;
import com.aidigital.aionboarding.service.mappers.material.MaterialMapper;
import com.aidigital.aionboarding.service.mappers.material.MaterialMapperImpl;
import com.aidigital.aionboarding.service.material.services.MaterialLinkService.PreparedLinkRecord;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.Select.field;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialLinkServiceImplTest {

	@Mock
	private MaterialLinkRepository materialLinkRepository;
	@Mock
	private LinkMetadataService linkMetadataService;
	@Spy
	private TextValueNormalizer textValueNormalizer = new TextValueNormalizer();
	@Spy
	private MaterialMapper materialMapper = new MaterialMapperImpl();

	@InjectMocks
	private MaterialLinkServiceImpl service;

	@Test
	void saveLinksShouldAssignSortOrderByIndexAndSaveEachRecordTest() {
		// Given:
		Material material = Instancio.of(Material.class).set(field(Material::getId), 15L).create();
		PreparedLinkRecord first = new PreparedLinkRecord(
				"https://example.com/one", "Title 1", "Desc 1", "img1", "Site 1", "text1", ""
		);
		PreparedLinkRecord second = new PreparedLinkRecord(
				"https://example.com/two", "Title 2", "Desc 2", "img2", "Site 2", "text2", ""
		);

		// When:
		service.saveLinks(material, List.of(first, second));

		// Then:
		ArgumentCaptor<MaterialLink> captor = ArgumentCaptor.forClass(MaterialLink.class);
		verify(materialLinkRepository, times(2)).save(captor.capture());
		List<MaterialLink> saved = captor.getAllValues();
		assertThat(saved.get(0).getMaterial()).isSameAs(material);
		assertThat(saved.get(0).getUrl()).isEqualTo(first.url());
		assertThat(saved.get(0).getSortOrder()).isEqualTo(0);
		assertThat(saved.get(1).getUrl()).isEqualTo(second.url());
		assertThat(saved.get(1).getSortOrder()).isEqualTo(1);
	}

	@Test
	void deleteByMaterialIdShouldDelegateToRepositoryTest() {
		// Given:
		Long materialId = 25L;

		// When:
		service.deleteByMaterialId(materialId);

		// Then:
		verify(materialLinkRepository).deleteByMaterial_Id(materialId);
	}

	@Test
	void findByMaterialIdOrderBySortOrderAscShouldReturnRepositoryResultUnchangedTest() {
		// Given:
		Long materialId = 35L;
		List<MaterialLink> expected = List.of(Instancio.create(MaterialLink.class));
		when(materialLinkRepository.findByMaterialIdOrderBySortOrderAsc(materialId)).thenReturn(expected);

		// When:
		List<MaterialLink> result = service.findByMaterialIdOrderBySortOrderAsc(materialId);

		// Then:
		assertThat(result).isSameAs(expected);
		verify(materialLinkRepository).findByMaterialIdOrderBySortOrderAsc(materialId);
	}
}
