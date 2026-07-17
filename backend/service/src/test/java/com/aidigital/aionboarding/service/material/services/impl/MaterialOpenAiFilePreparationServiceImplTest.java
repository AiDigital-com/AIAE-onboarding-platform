package com.aidigital.aionboarding.service.material.services.impl;

import com.aidigital.aionboarding.external.openai.OpenAiClient;
import com.aidigital.aionboarding.external.openai.OpenAiExternalException;
import com.aidigital.aionboarding.external.openai.model.OpenAiFileInput;
import com.aidigital.aionboarding.external.openai.model.OpenAiFileUploadResponse;
import com.aidigital.aionboarding.service.material.models.MaterialAttachmentInput;
import com.aidigital.aionboarding.service.material.models.MaterialOpenAiUploadInput;
import com.aidigital.aionboarding.service.material.services.MaterialFileService;
import com.aidigital.aionboarding.service.storage.StorageService;
import com.aidigital.aionboarding.service.storage.enums.UploadPurpose;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialOpenAiFilePreparationServiceImplTest {

	@Mock
	private ObjectProvider<OpenAiClient> openAiClientProvider;
	@Mock
	private OpenAiClient openAiClient;
	@Mock
	private MaterialFileService materialFileService;
	@Mock
	private StorageService storageService;

	@Test
	void shouldReuseExistingFileIdWhenAlreadyUploadedTest() {
		// Given:
		MaterialOpenAiFilePreparationServiceImpl service =
				new MaterialOpenAiFilePreparationServiceImpl(openAiClientProvider, materialFileService,
						storageService);
		when(openAiClientProvider.getIfAvailable()).thenReturn(openAiClient);
		MaterialAttachmentInput attachment = attachmentWith("file-existing-001", "uploaded", "application/pdf");
		when(materialFileService.findAttachmentsForMaterials(List.of(1L))).thenReturn(List.of(attachment));

		// When:
		List<OpenAiFileInput> result = service.prepareFileInputs(List.of(1L));

		// Then:
		assertThat(result).hasSize(1);
		assertThat(result.get(0).fileId()).isEqualTo("file-existing-001");
		verify(openAiClient, never()).uploadFile(any(), any(), any());
		verify(materialFileService, never()).updateMaterialFileOpenAIUpload(any(), any());
	}

	@Test
	void shouldUploadFreshAttachmentAndPersistSuccessTest() {
		// Given:
		MaterialOpenAiFilePreparationServiceImpl service =
				new MaterialOpenAiFilePreparationServiceImpl(openAiClientProvider, materialFileService,
						storageService);
		when(openAiClientProvider.getIfAvailable()).thenReturn(openAiClient);
		MaterialAttachmentInput attachment = attachmentWith(null, null, "application/pdf");
		when(materialFileService.findAttachmentsForMaterials(List.of(1L))).thenReturn(List.of(attachment));
		byte[] bytes = new byte[]{1, 2, 3};
		when(storageService.getObjectBuffer(eq("storage-key-001"), eq(UploadPurpose.MATERIAL_UPLOAD.maxSizeBytes()))).thenReturn(bytes);
		when(openAiClient.uploadFile(bytes, "report.pdf", "user_data"))
				.thenReturn(new OpenAiFileUploadResponse("file-new-xyz", "user_data", 3L));

		// When:
		List<OpenAiFileInput> result = service.prepareFileInputs(List.of(1L));

		// Then:
		assertThat(result).hasSize(1);
		assertThat(result.get(0).fileId()).isEqualTo("file-new-xyz");

		ArgumentCaptor<MaterialOpenAiUploadInput> captor = ArgumentCaptor.forClass(MaterialOpenAiUploadInput.class);
		verify(materialFileService).updateMaterialFileOpenAIUpload(eq(42L), captor.capture());
		MaterialOpenAiUploadInput persisted = captor.getValue();
		assertThat(persisted.openaiFileId()).isEqualTo("file-new-xyz");
		assertThat(persisted.openaiFileStatus()).isEqualTo("uploaded");
		assertThat(persisted.openaiFilePurpose()).isEqualTo("user_data");
	}

	@Test
	void shouldPersistFailureWhenUploadThrowsTest() {
		// Given:
		MaterialOpenAiFilePreparationServiceImpl service =
				new MaterialOpenAiFilePreparationServiceImpl(openAiClientProvider, materialFileService,
						storageService);
		when(openAiClientProvider.getIfAvailable()).thenReturn(openAiClient);
		MaterialAttachmentInput attachment = attachmentWith(null, null, "application/pdf");
		when(materialFileService.findAttachmentsForMaterials(List.of(1L))).thenReturn(List.of(attachment));
		when(storageService.getObjectBuffer(any(), eq(UploadPurpose.MATERIAL_UPLOAD.maxSizeBytes()))).thenReturn(new byte[]{1});
		when(openAiClient.uploadFile(any(), any(), any()))
				.thenThrow(new OpenAiExternalException("upload rejected", 422, "{}"));

		// When:
		List<OpenAiFileInput> result = service.prepareFileInputs(List.of(1L));

		// Then:
		assertThat(result).isEmpty();

		ArgumentCaptor<MaterialOpenAiUploadInput> captor = ArgumentCaptor.forClass(MaterialOpenAiUploadInput.class);
		verify(materialFileService).updateMaterialFileOpenAIUpload(eq(42L), captor.capture());
		MaterialOpenAiUploadInput persisted = captor.getValue();
		assertThat(persisted.openaiFileId()).isNull();
		assertThat(persisted.openaiFileStatus()).isEqualTo("error");
		assertThat(persisted.openaiFileError()).isNotBlank();
	}

	@Test
	void shouldSkipIncompatibleMimeTypeWithoutUploadTest() {
		// Given:
		MaterialOpenAiFilePreparationServiceImpl service =
				new MaterialOpenAiFilePreparationServiceImpl(openAiClientProvider, materialFileService,
						storageService);
		when(openAiClientProvider.getIfAvailable()).thenReturn(openAiClient);
		MaterialAttachmentInput attachment = attachmentWith(null, null, "application/zip");
		when(materialFileService.findAttachmentsForMaterials(List.of(1L))).thenReturn(List.of(attachment));

		// When:
		List<OpenAiFileInput> result = service.prepareFileInputs(List.of(1L));

		// Then:
		assertThat(result).isEmpty();
		verify(openAiClient, never()).uploadFile(any(), any(), any());
		verify(materialFileService, never()).updateMaterialFileOpenAIUpload(any(), any());
	}

	@Test
	void shouldReturnEmptyListForEmptyMaterialIdsTest() {
		// Given:
		MaterialOpenAiFilePreparationServiceImpl service =
				new MaterialOpenAiFilePreparationServiceImpl(openAiClientProvider, materialFileService,
						storageService);

		// When:
		List<OpenAiFileInput> result = service.prepareFileInputs(List.of());

		// Then:
		assertThat(result).isEmpty();
		verify(openAiClientProvider, never()).getIfAvailable();
		verify(materialFileService, never()).findAttachmentsForMaterials(any());
	}

	// ---------------------------------------------------------------------------
	// Helpers
	// ---------------------------------------------------------------------------

	private MaterialAttachmentInput attachmentWith(String openaiFileId, String openaiFileStatus, String mimeType) {
		return new MaterialAttachmentInput(
				42L,
				"report.pdf",
				"storage-key-001",
				mimeType,
				1024L,
				"file",
				openaiFileId,
				openaiFileStatus != null ? "user_data" : "",
				openaiFileStatus != null ? openaiFileStatus : "",
				"",
				openaiFileId != null ? LocalDateTime.of(2026, 1, 1, 0, 0) : null
		);
	}
}
