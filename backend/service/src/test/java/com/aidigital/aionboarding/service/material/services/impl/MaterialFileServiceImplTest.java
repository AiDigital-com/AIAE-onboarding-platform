package com.aidigital.aionboarding.service.material.services.impl;

import com.aidigital.aionboarding.domain.material.entities.Material;
import com.aidigital.aionboarding.domain.material.entities.MaterialFile;
import com.aidigital.aionboarding.domain.material.repositories.MaterialFileRepository;
import com.aidigital.aionboarding.service.common.dictionary.DictionaryLookupService;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.mapping.TextValueNormalizer;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.mappers.material.MaterialMapper;
import com.aidigital.aionboarding.service.mappers.material.MaterialMapperImpl;
import com.aidigital.aionboarding.service.material.models.MaterialAttachmentInput;
import com.aidigital.aionboarding.service.material.models.MaterialOpenAiUploadInput;
import com.aidigital.aionboarding.service.material.models.MaterialOpenAiUploadRecord;
import com.aidigital.aionboarding.service.material.support.MaterialFileQuerySupport;
import com.aidigital.aionboarding.service.storage.StorageService;
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
	@Mock
	private MaterialFileQuerySupport materialFileQuerySupport;
	@Spy
	private TextValueNormalizer textValueNormalizer = new TextValueNormalizer();
	@Spy
	private CurrentTime currentTime = new CurrentTime();

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
	void findByMaterialIdShouldDelegateToQuerySupportTest() {
		// Given:
		Long materialId = 60L;
		List<MaterialFile> expected = List.of(Instancio.create(MaterialFile.class));
		when(materialFileQuerySupport.findByMaterialId(materialId)).thenReturn(expected);

		// When:
		List<MaterialFile> result = service.findByMaterialId(materialId);

		// Then:
		assertThat(result).isSameAs(expected);
		verify(materialFileQuerySupport).findByMaterialId(materialId);
	}

	@Test
	void findByMaterialIdOrderByCreatedAtAscShouldDelegateToQuerySupportTest() {
		// Given:
		Long materialId = 61L;
		List<MaterialFile> expected = List.of(Instancio.create(MaterialFile.class));
		when(materialFileQuerySupport.findByMaterialIdOrderByCreatedAtAsc(materialId)).thenReturn(expected);

		// When:
		List<MaterialFile> result = service.findByMaterialIdOrderByCreatedAtAsc(materialId);

		// Then:
		assertThat(result).isSameAs(expected);
		verify(materialFileQuerySupport).findByMaterialIdOrderByCreatedAtAsc(materialId);
	}
}
