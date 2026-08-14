package com.aidigital.aionboarding.service.material.services.impl;

import com.aidigital.aionboarding.domain.material.entities.Material;
import com.aidigital.aionboarding.domain.material.entities.MaterialFile;
import com.aidigital.aionboarding.domain.material.repositories.MaterialFileRepository;
import com.aidigital.aionboarding.domain.material.repositories.MaterialFileSummaryProjection;
import com.aidigital.aionboarding.service.common.dictionary.DictionaryLookupService;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.mapping.TextValueNormalizer;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.common.time.CurrentTimeImpl;
import com.aidigital.aionboarding.service.mappers.material.MaterialMapper;
import com.aidigital.aionboarding.service.mappers.material.MaterialMapperImpl;
import com.aidigital.aionboarding.service.material.models.MaterialAttachmentInput;
import com.aidigital.aionboarding.service.material.models.MaterialOpenAiUploadInput;
import com.aidigital.aionboarding.service.material.models.MaterialOpenAiUploadRecord;
import com.aidigital.aionboarding.service.storage.StorageService;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.instancio.Select.field;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialFileServiceImplTest {

	@Mock
	private MaterialFileRepository materialFileRepository;
	@Mock
	private DictionaryLookupService dictionaryLookupService;
	@Mock
	private StorageService storageService;
	@Spy
	private MaterialMapper materialMapper = new MaterialMapperImpl();
	@Spy
	private TextValueNormalizer textValueNormalizer = new TextValueNormalizer();
	@Spy
	private CurrentTime currentTime = new CurrentTimeImpl();

	@InjectMocks
	private MaterialFileServiceImpl service;

	@Test
	void saveAttachmentsShouldPersistOneFileRowPerAttachmentTest() {
		// Given:
		AppUser viewer = new AppUser(1L, "clerk-1", "viewer@test.com", "Viewer", "member", "Viewer", null, null, null);
		Material material = Instancio.of(Material.class).set(field(Material::getId), 10L).create();
		MaterialAttachmentInput attachment = Instancio.of(MaterialAttachmentInput.class)
				.set(field(MaterialAttachmentInput::kind), "file")
				.set(field(MaterialAttachmentInput::openaiFileId), "")
				.create();
		when(dictionaryLookupService.materialFileKindId(eq("file"))).thenReturn(1L);

		// When:
		service.saveAttachments(viewer, material, List.of(attachment));

		// Then:
		ArgumentCaptor<MaterialFile> captor = ArgumentCaptor.forClass(MaterialFile.class);
		verify(materialFileRepository).save(captor.capture());
		MaterialFile saved = captor.getValue();
		assertThat(saved.getMaterial()).isSameAs(material);
		assertThat(saved.getOriginalName()).isEqualTo(attachment.originalName());
		assertThat(saved.getStorageKey()).isEqualTo(attachment.storageKey());
		assertThat(saved.getCreatedAt()).isNotNull();
		verify(storageService).confirmUpload(viewer, attachment.storageKey());
	}

	@Test
	void reconcileAttachmentsShouldUpdateExistingRowAndDeleteOrphanTest() {
		// Given:
		AppUser viewer = new AppUser(2L, "clerk-2", "viewer2@test.com", "Viewer2", "member", "Viewer2", null, null,
				null);
		Material material = Instancio.of(Material.class).set(field(Material::getId), 20L).create();
		MaterialFile existingKept = Instancio.of(MaterialFile.class).set(field(MaterialFile::getId), 1L).create();
		MaterialFile existingOrphan = Instancio.of(MaterialFile.class).set(field(MaterialFile::getId), 2L).create();
		when(materialFileRepository.findByMaterialId(20L)).thenReturn(List.of(existingKept, existingOrphan));
		when(dictionaryLookupService.materialFileKindId(eq("file"))).thenReturn(1L);

		MaterialAttachmentInput updatedInput = Instancio.of(MaterialAttachmentInput.class)
				.set(field(MaterialAttachmentInput::id), 1L)
				.set(field(MaterialAttachmentInput::kind), "file")
				.set(field(MaterialAttachmentInput::openaiFileId), "")
				.create();

		// When:
		service.reconcileAttachments(viewer, material, List.of(updatedInput));

		// Then:
		ArgumentCaptor<MaterialFile> savedCaptor = ArgumentCaptor.forClass(MaterialFile.class);
		verify(materialFileRepository).save(savedCaptor.capture());
		assertThat(savedCaptor.getValue()).isSameAs(existingKept);
		assertThat(savedCaptor.getValue().getOriginalName()).isEqualTo(updatedInput.originalName());
		verify(storageService).confirmUpload(viewer, updatedInput.storageKey());

		ArgumentCaptor<Iterable<MaterialFile>> deletedCaptor = ArgumentCaptor.forClass(Iterable.class);
		verify(materialFileRepository).deleteAll(deletedCaptor.capture());
		assertThat(deletedCaptor.getValue()).containsExactly(existingOrphan);
	}

	@Test
	void reconcileAttachmentsShouldNotReconfirmAnUnchangedStorageKeyTest() {
		// Given:
		AppUser viewer = new AppUser(3L, "clerk-3", "viewer3@test.com", "Viewer3", "member", "Viewer3", null, null,
				null);
		Material material = Instancio.of(Material.class).set(field(Material::getId), 21L).create();
		MaterialFile existingKept = Instancio.of(MaterialFile.class)
				.set(field(MaterialFile::getId), 1L)
				.set(field(MaterialFile::getStorageKey), "same-key")
				.create();
		when(materialFileRepository.findByMaterialId(21L)).thenReturn(List.of(existingKept));
		when(dictionaryLookupService.materialFileKindId(eq("file"))).thenReturn(1L);

		MaterialAttachmentInput unchangedInput = Instancio.of(MaterialAttachmentInput.class)
				.set(field(MaterialAttachmentInput::id), 1L)
				.set(field(MaterialAttachmentInput::kind), "file")
				.set(field(MaterialAttachmentInput::storageKey), "same-key")
				.set(field(MaterialAttachmentInput::openaiFileId), "")
				.create();

		// When:
		service.reconcileAttachments(viewer, material, List.of(unchangedInput));

		// Then:
		verifyNoInteractions(storageService);
	}

	@Test
	void reconcileAttachmentsShouldThrowWhenAttachmentIdDoesNotBelongToMaterialTest() {
		// Given:
		AppUser viewer = new AppUser(4L, "clerk-4", "viewer4@test.com", "Viewer4", "member", "Viewer4", null, null,
				null);
		Material material = Instancio.of(Material.class).set(field(Material::getId), 30L).create();
		when(materialFileRepository.findByMaterialId(30L)).thenReturn(List.of());
		MaterialAttachmentInput invalidInput = Instancio.of(MaterialAttachmentInput.class)
				.set(field(MaterialAttachmentInput::id), 999L)
				.create();

		// When-Then:
		assertThatThrownBy(() -> service.reconcileAttachments(viewer, material, List.of(invalidInput)))
				.isInstanceOf(AppException.class);
	}

	@Test
	void updateMaterialFileOpenAIUploadShouldReturnNullWhenFileMissingTest() {
		// Given:
		when(materialFileRepository.findById(404L)).thenReturn(java.util.Optional.empty());
		MaterialOpenAiUploadInput input = Instancio.create(MaterialOpenAiUploadInput.class);

		// When:
		MaterialOpenAiUploadRecord result = service.updateMaterialFileOpenAIUpload(404L, input);

		// Then:
		assertThat(result).isNull();
	}

	@Test
	void updateMaterialFileOpenAIUploadShouldNullBlankOpenaiFileIdTest() {
		// Given:
		MaterialFile file = Instancio.of(MaterialFile.class).set(field(MaterialFile::getId), 5L).create();
		when(materialFileRepository.findById(5L)).thenReturn(java.util.Optional.of(file));
		MaterialOpenAiUploadInput input = Instancio.of(MaterialOpenAiUploadInput.class)
				.set(field(MaterialOpenAiUploadInput::openaiFileId), "   ")
				.create();
		MaterialOpenAiUploadRecord expectedRecord = Instancio.create(MaterialOpenAiUploadRecord.class);
		when(materialMapper.toOpenAiUploadRecord(file)).thenReturn(expectedRecord);

		// When:
		MaterialOpenAiUploadRecord result = service.updateMaterialFileOpenAIUpload(5L, input);

		// Then:
		assertThat(result).isSameAs(expectedRecord);
		assertThat(file.getOpenaiFileId()).isNull();
		verify(materialFileRepository).save(file);
	}

	@Test
	void deleteByMaterialIdShouldDelegateToRepositoryTest() {
		// Given:
		Long materialId = 55L;

		// When:
		service.deleteByMaterialId(materialId);

		// Then:
		verify(materialFileRepository).deleteByMaterial_Id(materialId);
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
		List<String> result = service.collectStorageKeys(80L);

		// Then:
		assertThat(result).containsExactly("storage-key");
	}

	@Test
	void findRemovedStorageKeysShouldReturnKeysNotKeptByAttachmentsTest() {
		// Given:
		List<String> existingStorageKeys = List.of("kept-key", "removed-key");
		MaterialAttachmentInput attachment = Instancio.of(MaterialAttachmentInput.class)
				.set(field(MaterialAttachmentInput::storageKey), "kept-key")
				.create();

		// When:
		List<String> result = service.findRemovedStorageKeys(existingStorageKeys, List.of(attachment));

		// Then:
		assertThat(result).containsExactly("removed-key");
	}

	@Test
	void findAttachmentsForMaterialsShouldReturnEmptyListForNullOrEmptyInputTest() {
		// When-Then:
		assertThat(service.findAttachmentsForMaterials(null)).isEmpty();
		assertThat(service.findAttachmentsForMaterials(List.of())).isEmpty();
	}

	@Test
	void findAttachmentsForMaterialsShouldMapFilesFromEachMaterialInOrderTest() {
		// Given:
		MaterialFile file = Instancio.create(MaterialFile.class);
		when(materialFileRepository.findByMaterialIdOrderByCreatedAtAsc(70L)).thenReturn(List.of(file));
		MaterialAttachmentInput expected = materialMapper.toAttachmentInput(file);

		// When:
		List<MaterialAttachmentInput> result = service.findAttachmentsForMaterials(Arrays.asList(70L, null));

		// Then:
		assertThat(result).containsExactly(expected);
	}

	@Test
	void findByMaterialIdsOrderByCreatedAtAscShouldReturnEmptyListForNullOrEmptyInputTest() {
		// When-Then:
		assertThat(service.findByMaterialIdsOrderByCreatedAtAsc(null)).isEmpty();
		assertThat(service.findByMaterialIdsOrderByCreatedAtAsc(List.of())).isEmpty();
	}

	@Test
	void findByMaterialIdsOrderByCreatedAtAscShouldDelegateToRepositoryTest() {
		// Given:
		List<Long> materialIds = List.of(90L, 91L);
		List<MaterialFile> expected = List.of(Instancio.create(MaterialFile.class));
		when(materialFileRepository.findByMaterialIdInOrderByCreatedAtAsc(materialIds)).thenReturn(expected);

		// When:
		List<MaterialFile> result = service.findByMaterialIdsOrderByCreatedAtAsc(materialIds);

		// Then:
		assertThat(result).isSameAs(expected);
	}

	@Test
	void findSummariesByMaterialIdsShouldReturnEmptyListForNullOrEmptyInputTest() {
		// When-Then:
		assertThat(service.findSummariesByMaterialIds(null)).isEmpty();
		assertThat(service.findSummariesByMaterialIds(List.of())).isEmpty();
	}

	@Test
	void findSummariesByMaterialIdsShouldDelegateToRepositoryTest() {
		// Given:
		List<Long> materialIds = List.of(95L);
		List<MaterialFileSummaryProjection> expected = List.of(
				Instancio.create(MaterialFileSummaryProjection.class));
		when(materialFileRepository.findSummariesByMaterialIdIn(materialIds)).thenReturn(expected);

		// When:
		List<MaterialFileSummaryProjection> result = service.findSummariesByMaterialIds(materialIds);

		// Then:
		assertThat(result).isSameAs(expected);
	}

	@Test
	void existsByStorageKeyShouldDelegateToRepositoryTest() {
		// Given:
		when(materialFileRepository.existsByStorageKey("uploads/key")).thenReturn(true);

		// When:
		boolean result = service.existsByStorageKey("uploads/key");

		// Then:
		assertThat(result).isTrue();
	}
}
