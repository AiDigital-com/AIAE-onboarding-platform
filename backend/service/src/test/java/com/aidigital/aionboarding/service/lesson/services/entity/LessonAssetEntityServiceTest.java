package com.aidigital.aionboarding.service.lesson.services.entity;

import com.aidigital.aionboarding.domain.lesson.entities.LessonAsset;
import com.aidigital.aionboarding.domain.lesson.repositories.LessonAssetRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonAssetEntityServiceTest {

	@Mock
	private LessonAssetRepository lessonAssetRepository;

	@InjectMocks
	private LessonAssetEntityService lessonAssetEntityService;

	@Test
	void findByLessonIdOrderByCreatedAtAscShouldReturnRepositoryResultTest() {
		// Given:
		LessonAsset asset = Instancio.of(LessonAsset.class).set(field(LessonAsset::getId), 1L).create();
		when(lessonAssetRepository.findByLessonIdOrderByCreatedAtAsc(eq(42L))).thenReturn(List.of(asset));

		// When:
		List<LessonAsset> result = lessonAssetEntityService.findByLessonIdOrderByCreatedAtAsc(42L);

		// Then:
		assertThat(result).containsExactly(asset);
	}

	@Test
	void saveShouldReturnRepositorySaveResultTest() {
		// Given:
		LessonAsset asset = Instancio.of(LessonAsset.class).set(field(LessonAsset::getId), 2L).create();
		when(lessonAssetRepository.save(eq(asset))).thenReturn(asset);

		// When:
		LessonAsset result = lessonAssetEntityService.save(asset);

		// Then:
		assertThat(result).isSameAs(asset);
	}
}
