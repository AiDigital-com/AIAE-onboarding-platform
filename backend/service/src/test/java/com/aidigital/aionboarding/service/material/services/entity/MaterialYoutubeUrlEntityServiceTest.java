package com.aidigital.aionboarding.service.material.services.entity;

import com.aidigital.aionboarding.domain.material.entities.MaterialYoutubeUrl;
import com.aidigital.aionboarding.domain.material.repositories.MaterialYoutubeUrlRepository;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.Select.field;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialYoutubeUrlEntityServiceTest {

	@Mock
	private MaterialYoutubeUrlRepository materialYoutubeUrlRepository;

	@InjectMocks
	private MaterialYoutubeUrlEntityService entityService;

	@Test
	void claimMissingMetadataBatchShouldReturnRepositoryClaimResultTest() {
		// Given:
		MaterialYoutubeUrl claimed = Instancio.of(MaterialYoutubeUrl.class).set(field(MaterialYoutubeUrl::getId), 1L)
				.create();
		when(materialYoutubeUrlRepository.claimMissingMetadataBatch(eq(12))).thenReturn(List.of(claimed));

		// When:
		List<MaterialYoutubeUrl> result = entityService.claimMissingMetadataBatch(12);

		// Then:
		assertThat(result).containsExactly(claimed);
	}

	@Test
	void saveShouldReturnRepositorySaveResultTest() {
		// Given:
		MaterialYoutubeUrl entity = Instancio.of(MaterialYoutubeUrl.class).set(field(MaterialYoutubeUrl::getId), 2L)
				.create();
		when(materialYoutubeUrlRepository.save(eq(entity))).thenReturn(entity);

		// When:
		MaterialYoutubeUrl result = entityService.save(entity);

		// Then:
		assertThat(result).isSameAs(entity);
	}

	@Test
	void deleteByMaterialIdShouldDelegateToRepositoryTest() {
		// When:
		entityService.deleteByMaterialId(3L);

		// Then:
		verify(materialYoutubeUrlRepository).deleteByMaterial_Id(3L);
	}

	@Test
	void findByMaterialIdOrderBySortOrderAscShouldReturnRepositoryResultTest() {
		// Given:
		List<MaterialYoutubeUrl> expected = List.of(Instancio.create(MaterialYoutubeUrl.class));
		when(materialYoutubeUrlRepository.findByMaterialIdOrderBySortOrderAsc(eq(4L))).thenReturn(expected);

		// When:
		List<MaterialYoutubeUrl> result = entityService.findByMaterialIdOrderBySortOrderAsc(4L);

		// Then:
		assertThat(result).isSameAs(expected);
	}

	@Test
	void findByMaterialIdsOrderBySortOrderAscShouldReturnRepositoryResultTest() {
		// Given:
		List<Long> materialIds = List.of(5L, 6L);
		List<MaterialYoutubeUrl> expected = List.of(Instancio.create(MaterialYoutubeUrl.class));
		when(materialYoutubeUrlRepository.findByMaterialIdInOrderBySortOrderAsc(eq(materialIds))).thenReturn(expected);

		// When:
		List<MaterialYoutubeUrl> result = entityService.findByMaterialIdsOrderBySortOrderAsc(materialIds);

		// Then:
		assertThat(result).isSameAs(expected);
	}
}
