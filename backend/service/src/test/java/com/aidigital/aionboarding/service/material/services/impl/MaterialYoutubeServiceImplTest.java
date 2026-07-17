package com.aidigital.aionboarding.service.material.services.impl;

import com.aidigital.aionboarding.domain.material.entities.Material;
import com.aidigital.aionboarding.domain.material.entities.MaterialYoutubeUrl;
import com.aidigital.aionboarding.domain.material.repositories.MaterialYoutubeUrlRepository;
import com.aidigital.aionboarding.external.youtube.YoutubeClient;
import com.aidigital.aionboarding.external.youtube.model.YoutubeOEmbedMetadata;
import com.aidigital.aionboarding.service.common.mapping.TextValueNormalizer;
import com.aidigital.aionboarding.service.mappers.material.MaterialMapper;
import com.aidigital.aionboarding.service.mappers.material.MaterialMapperImpl;
import com.aidigital.aionboarding.service.material.services.MaterialYoutubeService.PreparedYoutubeRecord;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.Select.field;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialYoutubeServiceImplTest {

	@Mock
	private MaterialYoutubeUrlRepository materialYoutubeUrlRepository;
	@Mock
	private YoutubeClient youtubeClient;
	@Spy
	private TextValueNormalizer textValueNormalizer = new TextValueNormalizer();
	@Spy
	private MaterialMapper materialMapper = new MaterialMapperImpl();

	@InjectMocks
	private MaterialYoutubeServiceImpl service;

	@Test
	void backfillMissingYoutubeMetadataShouldFetchAndSaveEachMissingRowTest() {
		// Given:
		MaterialYoutubeUrl missingRow = Instancio.of(MaterialYoutubeUrl.class)
				.set(field(MaterialYoutubeUrl::getId), 40L)
				.set(field(MaterialYoutubeUrl::getUrl), "https://youtu.be/abc123")
				.create();
		when(materialYoutubeUrlRepository.findWithMissingMetadata(PageRequest.of(0, 12)))
				.thenReturn(List.of(missingRow));
		YoutubeOEmbedMetadata metadata = new YoutubeOEmbedMetadata(
				"Video title", "Author", "https://youtube.com/author", "https://img", 120, 90, "YouTube", ""
		);
		when(youtubeClient.fetchOembed("https://youtu.be/abc123")).thenReturn(metadata);

		// When:
		service.backfillMissingYoutubeMetadata();

		// Then:
		ArgumentCaptor<MaterialYoutubeUrl> captor = ArgumentCaptor.forClass(MaterialYoutubeUrl.class);
		verify(materialYoutubeUrlRepository).save(captor.capture());
		assertThat(captor.getValue()).isSameAs(missingRow);
		assertThat(captor.getValue().getTitle()).isEqualTo("Video title");
		assertThat(captor.getValue().getAuthorName()).isEqualTo("Author");
		assertThat(captor.getValue().getThumbnailWidth()).isEqualTo(120);
	}

	@Test
	void saveYoutubeUrlsShouldAssignSortOrderByIndexAndSaveEachRecordTest() {
		// Given:
		Material material = Instancio.of(Material.class).set(field(Material::getId), 50L).create();
		PreparedYoutubeRecord first = new PreparedYoutubeRecord(
				"https://youtu.be/one", "T1", "A1", "https://a1", "https://thumb1", 100, 75, "YouTube", ""
		);
		PreparedYoutubeRecord second = new PreparedYoutubeRecord(
				"https://youtu.be/two", "T2", "A2", "https://a2", "https://thumb2", 200, 150, "YouTube", ""
		);

		// When:
		service.saveYoutubeUrls(material, List.of(first, second));

		// Then:
		ArgumentCaptor<MaterialYoutubeUrl> captor = ArgumentCaptor.forClass(MaterialYoutubeUrl.class);
		verify(materialYoutubeUrlRepository, times(2)).save(captor.capture());
		List<MaterialYoutubeUrl> saved = captor.getAllValues();
		assertThat(saved.get(0).getMaterial()).isSameAs(material);
		assertThat(saved.get(0).getUrl()).isEqualTo(first.url());
		assertThat(saved.get(0).getSortOrder()).isEqualTo(0);
		assertThat(saved.get(1).getUrl()).isEqualTo(second.url());
		assertThat(saved.get(1).getSortOrder()).isEqualTo(1);
	}

	@Test
	void findByMaterialIdOrderBySortOrderAscShouldReturnRepositoryResultUnchangedTest() {
		// Given:
		Long materialId = 65L;
		List<MaterialYoutubeUrl> expected = List.of(Instancio.create(MaterialYoutubeUrl.class));
		when(materialYoutubeUrlRepository.findByMaterialIdOrderBySortOrderAsc(materialId)).thenReturn(expected);

		// When:
		List<MaterialYoutubeUrl> result = service.findByMaterialIdOrderBySortOrderAsc(materialId);

		// Then:
		assertThat(result).isSameAs(expected);
		verify(materialYoutubeUrlRepository).findByMaterialIdOrderBySortOrderAsc(materialId);
	}
}
