package com.aidigital.aionboarding.service.material.support;

import com.aidigital.aionboarding.domain.material.entities.MaterialFile;
import com.aidigital.aionboarding.domain.material.repositories.MaterialFileRepository;
import com.aidigital.aionboarding.service.common.mapping.TextValueNormalizer;
import com.aidigital.aionboarding.service.mappers.material.MaterialMapper;
import com.aidigital.aionboarding.service.material.models.MaterialAttachmentInput;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.Select.field;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialFileQuerySupportTest {

	@Mock
	private MaterialFileRepository materialFileRepository;
	@Mock
	private MaterialMapper materialMapper;
	@Spy
	private TextValueNormalizer textValueNormalizer = new TextValueNormalizer();

	@InjectMocks
	private MaterialFileQuerySupport querySupport;

	@Test
	void findRemovedStorageKeysShouldReturnKeysNotKeptByAttachmentsTest() {
		// Given:
		List<String> existingStorageKeys = List.of("kept-key", "removed-key");
		MaterialAttachmentInput attachment = Instancio.of(MaterialAttachmentInput.class)
				.set(field(MaterialAttachmentInput::storageKey), "kept-key")
				.create();

		// When:
		List<String> result = querySupport.findRemovedStorageKeys(existingStorageKeys, List.of(attachment));

		// Then:
		assertThat(result).containsExactly("removed-key");
	}

	@Test
	void findAttachmentsForMaterialsShouldReturnEmptyListForNullOrEmptyInputTest() {
		// When-Then:
		assertThat(querySupport.findAttachmentsForMaterials(null)).isEmpty();
		assertThat(querySupport.findAttachmentsForMaterials(List.of())).isEmpty();
	}

	@Test
	void findAttachmentsForMaterialsShouldMapFilesFromEachMaterialInOrderTest() {
		// Given:
		MaterialFile file = Instancio.create(MaterialFile.class);
		MaterialAttachmentInput mappedAttachment = Instancio.create(MaterialAttachmentInput.class);
		when(materialFileRepository.findByMaterialIdOrderByCreatedAtAsc(70L)).thenReturn(List.of(file));
		when(materialMapper.toAttachmentInput(file)).thenReturn(mappedAttachment);

		// When:
		List<MaterialAttachmentInput> result = querySupport.findAttachmentsForMaterials(Arrays.asList(70L, null));

		// Then:
		assertThat(result).containsExactly(mappedAttachment);
	}

	@Test
	void collectStorageKeysShouldFilterBlankAndNullKeysTest() {
		// Given:
		MaterialFile withKey = Instancio.of(MaterialFile.class)
				.set(field(MaterialFile::getStorageKey), "storage-key")
				.create();
		MaterialFile blankKey = Instancio.of(MaterialFile.class)
				.set(field(MaterialFile::getStorageKey), " ")
				.create();
		when(materialFileRepository.findByMaterialId(80L)).thenReturn(List.of(withKey, blankKey));

		// When:
		List<String> result = querySupport.collectStorageKeys(80L);

		// Then:
		assertThat(result).containsExactly("storage-key");
	}
}
