package com.aidigital.aionboarding.controllers;

import com.aidigital.aionboarding.api.v1.model.CountResponseV1;
import com.aidigital.aionboarding.api.v1.model.MaterialResponseV1;
import com.aidigital.aionboarding.api.v1.model.SearchMaterialsV1;
import com.aidigital.aionboarding.api.v1.model.UploadUrlRequestV1;
import com.aidigital.aionboarding.api.v1.model.UploadUrlResponseV1;
import com.aidigital.aionboarding.api.v1.model.UploadedFileResponseV1;
import com.aidigital.aionboarding.mappers.material.MaterialApiMapper;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.material.models.MaterialListQuery;
import com.aidigital.aionboarding.service.material.models.MaterialRecord;
import com.aidigital.aionboarding.service.material.services.MaterialService;
import com.aidigital.aionboarding.service.storage.StorageService;
import com.aidigital.aionboarding.service.storage.enums.UploadPurpose;
import com.aidigital.aionboarding.support.ApiResponses;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialsControllerTest {

	@Mock
	private CurrentUserSupport currentUser;
	@Mock
	private MaterialService materialService;
	@Mock
	private StorageService storageService;
	@Mock
	private MaterialApiMapper materialApiMapper;
	@Mock
	private ApiResponses apiResponses;

	@InjectMocks
	private MaterialsController controller;

	@Test
	void getMaterialShouldRequireAuthenticationAndReturnTheFullFidelityRecordTest() {
		// Given: the detail endpoint requires only authentication, unlike lesson detail's
		// visibility/enrollment rules — materials have no per-viewer visibility restriction
		AppUser viewer = new AppUser(1L, "clerk-1", "viewer@test.com", "Viewer", "member", "Viewer", null, null, null);
		MaterialRecord record = mock(MaterialRecord.class);
		MaterialResponseV1 expectedBody = mock(MaterialResponseV1.class);
		when(currentUser.requireUser()).thenReturn(viewer);
		when(materialService.getById(7L)).thenReturn(record);
		when(materialApiMapper.toMaterialResponseV1(record)).thenReturn(expectedBody);

		// When:
		ResponseEntity<MaterialResponseV1> response = controller.getMaterial(7L);

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
	}

	@Test
	void countMaterialsShouldDelegateToServiceAndWrapTheTotalTest() {
		// Given:
		AppUser viewer = new AppUser(1L, "clerk-1", "viewer@test.com", "Viewer", "member", "Viewer", null, null, null);
		SearchMaterialsV1 request = mock(SearchMaterialsV1.class);
		MaterialListQuery query = mock(MaterialListQuery.class);
		CountResponseV1 expectedBody = mock(CountResponseV1.class);
		when(currentUser.requireUser()).thenReturn(viewer);
		when(materialApiMapper.toMaterialListQuery(request)).thenReturn(query);
		when(materialService.count(viewer, query)).thenReturn(12L);
		when(materialApiMapper.toCountResponseV1(12L)).thenReturn(expectedBody);

		// When:
		ResponseEntity<CountResponseV1> response = controller.countMaterials(request);

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
	}

	@Test
	void createMaterialUploadUrlShouldPassTheViewerToStoragePresignTest() {
		// Given:
		AppUser viewer = new AppUser(2L, "clerk-2", "viewer2@test.com", "Viewer2", "member", "Viewer2", null, null,
				null);
		UploadUrlRequestV1 request = new UploadUrlRequestV1();
		request.setFileName("clip.mp4");
		request.setContentType("video/mp4");
		request.setSize(1024L);
		StorageService.PresignedUpload presigned = new StorageService.PresignedUpload("https://bucket/presigned",
				"uploads/abc/clip.mp4");
		UploadUrlResponseV1 expectedBody = mock(UploadUrlResponseV1.class);
		when(currentUser.requireUser()).thenReturn(viewer);
		when(storageService.presignPut(viewer, UploadPurpose.MATERIAL_UPLOAD, "clip.mp4", "video/mp4", 1024L)).thenReturn(presigned);
		when(apiResponses.uploadUrl("https://bucket/presigned", "uploads/abc/clip.mp4")).thenReturn(expectedBody);

		// When:
		ResponseEntity<UploadUrlResponseV1> response = controller.createMaterialUploadUrl(request);

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
	}

	@Test
	void uploadMaterialFileShouldStreamTheFileWithoutBufferingItIntoAByteArrayTest() throws Exception {
		// Given:
		AppUser viewer = new AppUser(3L, "clerk-3", "viewer3@test.com", "Viewer3", "member", "Viewer3", null, null,
				null);
		MockMultipartFile file = new MockMultipartFile("file", "notes.pdf", "application/pdf", "content".getBytes());
		UploadedFileResponseV1 expectedBody = mock(UploadedFileResponseV1.class);
		when(currentUser.requireUser()).thenReturn(viewer);
		when(storageService.putObjectStreaming(
				eq(viewer), eq(UploadPurpose.MATERIAL_UPLOAD), any(InputStream.class), eq(7L), eq("notes.pdf"), eq(
						"application/pdf")))
				.thenReturn("uploads/def/notes.pdf");
		when(materialApiMapper.toUploadedFileResponseV1("uploads/def/notes.pdf", "notes.pdf", "application/pdf", 7L))
				.thenReturn(expectedBody);

		// When:
		ResponseEntity<UploadedFileResponseV1> response = controller.uploadMaterialFile(file);

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
	}
}
