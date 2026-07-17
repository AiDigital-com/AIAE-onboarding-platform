package com.aidigital.aionboarding.controllers;

import com.aidigital.aionboarding.api.v1.model.FilePreviewsRequestV1;
import com.aidigital.aionboarding.api.v1.model.FilePreviewsResponseV1;
import com.aidigital.aionboarding.api.v1.model.FilePreviewResponseV1;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.storage.StorageKeyAuthorizationService;
import com.aidigital.aionboarding.service.storage.StorageService;
import com.aidigital.aionboarding.service.storage.models.FilePreviewRecord;
import com.aidigital.aionboarding.support.ApiResponses;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FilesControllerTest {

	@Mock
	private CurrentUserSupport currentUser;
	@Mock
	private StorageService storageService;
	@Mock
	private StorageKeyAuthorizationService authService;
	@Mock
	private ApiResponses apiResponses;

	@InjectMocks
	private FilesController controller;

	@Test
	void shouldRedirectToPresignedUrlTest() {
		// Given:
		AppUser viewer = viewer();
		when(currentUser.requireUser()).thenReturn(viewer);
		when(storageService.presignGet("key-1")).thenReturn("https://signed.url/1");

		// When:
		ResponseEntity<Void> response = controller.getFileObject("key-1");

		// Then:
		assertThat(response.getStatusCode().value()).isEqualTo(307);
		assertThat(response.getHeaders().getLocation()).isEqualTo(URI.create("https://signed.url/1"));
		assertThat(response.getHeaders().getFirst(HttpHeaders.LOCATION)).isEqualTo("https://signed.url/1");
	}

	@Test
	void shouldReturnFilePreviewTest() {
		// Given:
		AppUser viewer = viewer();
		FilePreviewResponseV1 expectedBody = Instancio.create(FilePreviewResponseV1.class);
		when(currentUser.requireUser()).thenReturn(viewer);
		when(storageService.presignGet("key-1")).thenReturn("https://preview.url/1");
		when(apiResponses.filePreview("https://preview.url/1")).thenReturn(expectedBody);

		// When:
		ResponseEntity<FilePreviewResponseV1> response = controller.getFilePreview("key-1");

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
	}

	@Test
	void shouldReturnFilePreviewsForDistinctKeysTest() {
		// Given:
		AppUser viewer = viewer();
		Set<String> storageKeys = new LinkedHashSet<>();
		storageKeys.add("key-1");
		storageKeys.add("key-2");
		FilePreviewsRequestV1 request = new FilePreviewsRequestV1(storageKeys);
		FilePreviewRecord preview1 = new FilePreviewRecord("key-1", "https://preview.url/1");
		FilePreviewRecord preview2 = new FilePreviewRecord("key-2", "https://preview.url/2");
		List<FilePreviewRecord> previews = List.of(preview1, preview2);
		FilePreviewsResponseV1 expectedBody = Instancio.create(FilePreviewsResponseV1.class);
		when(currentUser.requireUser()).thenReturn(viewer);
		when(storageService.presignGet(List.of("key-1", "key-2"))).thenReturn(previews);
		when(apiResponses.filePreviews(previews)).thenReturn(expectedBody);

		// When:
		ResponseEntity<FilePreviewsResponseV1> response = controller.getFilePreviews(request);

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
	}

	AppUser viewer() {
		return new AppUser(1L, "clerk-1", "user@test.com", "User", "member", "User", null, null, null);
	}
}
