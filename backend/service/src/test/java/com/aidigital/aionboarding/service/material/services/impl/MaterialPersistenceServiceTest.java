package com.aidigital.aionboarding.service.material.services.impl;

import com.aidigital.aionboarding.domain.material.entities.Material;
import com.aidigital.aionboarding.domain.material.repositories.MaterialRepository;
import com.aidigital.aionboarding.domain.user.repositories.UserRepository;
import com.aidigital.aionboarding.service.common.mapping.TextValueNormalizer;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.mappers.material.MaterialMapper;
import com.aidigital.aionboarding.service.mappers.material.MaterialMapperImpl;
import com.aidigital.aionboarding.service.material.models.MaterialRecord;
import com.aidigital.aionboarding.service.material.models.UpdateMaterialInput;
import com.aidigital.aionboarding.service.material.services.MaterialFileService;
import com.aidigital.aionboarding.service.material.services.MaterialLinkService;
import com.aidigital.aionboarding.service.material.services.MaterialRecordQueryService;
import com.aidigital.aionboarding.service.material.services.MaterialYoutubeService;
import com.aidigital.aionboarding.service.material.validation.MaterialPayloadValidator.ValidatedMaterialPayload;
import com.aidigital.aionboarding.service.storage.StorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialPersistenceServiceTest {

	@Mock
	private MaterialRepository materialRepository;
	@Mock
	private UserRepository userRepository;
	@Mock
	private MaterialRecordQueryService materialRecordQueryService;
	@Mock
	private MaterialYoutubeService materialYoutubeService;
	@Mock
	private MaterialLinkService materialLinkService;
	@Mock
	private MaterialFileService materialFileService;
	@Mock
	private StorageService storageService;
	@Spy
	private TextValueNormalizer textValueNormalizer = new TextValueNormalizer();
	@Spy
	private MaterialMapper materialMapper = new MaterialMapperImpl();

	@Spy
	private CurrentTime currentTime = new CurrentTime();

	@InjectMocks
	private MaterialPersistenceService service;

	@Test
	void shouldRegisterAfterCommitForRemovedStorageKeysTest() {
		// Given:
		AppUser viewer = new AppUser(1L, "clerk-1", "viewer@test.com", "Viewer", "member", "Viewer", null, null, null);
		Long materialId = 30L;
		Material material = material(materialId);
		UpdateMaterialInput input = updateInput();
		ValidatedMaterialPayload payload = payload();
		List<String> existingKeys = List.of("old-key");
		List<String> removedKeys = List.of("old-key");
		MaterialRecord materialRecord = org.mockito.Mockito.mock(MaterialRecord.class);

		when(materialRepository.findById(materialId)).thenReturn(Optional.of(material));
		when(materialRepository.save(any(Material.class))).thenReturn(material);
		when(materialFileService.findRemovedStorageKeys(existingKeys, payload.attachments()))
				.thenReturn(removedKeys);
		when(materialRecordQueryService.loadRecord(any(), anyLong())).thenReturn(materialRecord);
		when(materialRecordQueryService.countLessonUsage(materialId)).thenReturn(0L);

		try (MockedStatic<TransactionSynchronizationManager> txMgr =
					 mockStatic(TransactionSynchronizationManager.class)) {
			ArgumentCaptor<TransactionSynchronization> syncCaptor =
					ArgumentCaptor.forClass(TransactionSynchronization.class);

			// When:
			MaterialRecord result = service.update(viewer, materialId, input, payload, List.of(), List.of(),
					existingKeys);

			// Then:
			assertThat(result).isSameAs(materialRecord);
			verify(materialYoutubeService).deleteByMaterialId(materialId);
			verify(materialLinkService).deleteByMaterialId(materialId);
			verify(materialFileService).reconcileAttachments(viewer, material, payload.attachments());
			verifyNoInteractions(storageService);
			txMgr.verify(() ->
					TransactionSynchronizationManager.registerSynchronization(syncCaptor.capture())
			);

			syncCaptor.getValue().afterCommit();
			verify(materialFileService).deleteStorageKeysQuietly(removedKeys);
		}
	}

	@Test
	void shouldConfirmUploadWhenCoverImageStorageKeyChangesOnUpdateTest() {
		// Given:
		AppUser viewer = new AppUser(2L, "clerk-2", "viewer2@test.com", "Viewer2", "member", "Viewer2", null, null,
				null);
		Long materialId = 31L;
		Material material = material(materialId);
		material.setCoverImageStorageKey("old-cover-key");
		UpdateMaterialInput input = new UpdateMaterialInput(
				"Test title", "desc", "text",
				List.of(), List.of(), List.of(), List.of(),
				"new-cover-key", "cover.png", "image/png"
		);
		ValidatedMaterialPayload payload = payload();
		MaterialRecord materialRecord = org.mockito.Mockito.mock(MaterialRecord.class);

		when(materialRepository.findById(materialId)).thenReturn(Optional.of(material));
		when(materialRepository.save(any(Material.class))).thenReturn(material);
		when(materialRecordQueryService.loadRecord(any(), anyLong())).thenReturn(materialRecord);
		when(materialRecordQueryService.countLessonUsage(materialId)).thenReturn(0L);

		try (MockedStatic<TransactionSynchronizationManager> txMgr =
					 mockStatic(TransactionSynchronizationManager.class)) {
			// When:
			service.update(viewer, materialId, input, payload, List.of(), List.of(), List.of());

			// Then:
			verify(storageService).confirmUpload(viewer, "new-cover-key");
		}
	}

	private Material material(Long id) {
		Material material = new Material();
		material.setId(id);
		return material;
	}

	private ValidatedMaterialPayload payload() {
		return new ValidatedMaterialPayload(
				"Test title", "desc", "text",
				List.of(), List.of(), List.of(), List.of()
		);
	}

	private UpdateMaterialInput updateInput() {
		return new UpdateMaterialInput(
				"Test title", "desc", "text",
				List.of(), List.of(), List.of(), List.of(),
				null, null, null
		);
	}
}
