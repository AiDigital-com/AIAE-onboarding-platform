package com.aidigital.aionboarding.controllers;

import com.aidigital.aionboarding.api.v1.FilesApi;
import com.aidigital.aionboarding.api.v1.model.FilePreviewResponseV1;
import com.aidigital.aionboarding.api.v1.model.FilePreviewsRequestV1;
import com.aidigital.aionboarding.api.v1.model.FilePreviewsResponseV1;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.storage.StorageKeyAuthorizationService;
import com.aidigital.aionboarding.service.storage.StorageService;
import com.aidigital.aionboarding.support.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class FilesController implements FilesApi {

	private final CurrentUserSupport currentUser;
	private final StorageService storageService;
	private final StorageKeyAuthorizationService authService;
	private final ApiResponses apiResponses;

	@Override
	public ResponseEntity<Void> getFileObject(String storageKey) {
		AppUser appUser = currentUser.requireUser();
		authService.requireAccess(appUser, storageKey);
		String url = storageService.presignGet(storageKey);
		return ResponseEntity.status(307).location(URI.create(url)).header(HttpHeaders.LOCATION, url).build();
	}

	@Override
	public ResponseEntity<FilePreviewResponseV1> getFilePreview(String storageKey) {
		AppUser appUser = currentUser.requireUser();
		authService.requireAccess(appUser, storageKey);
		return ResponseEntity.ok(apiResponses.filePreview(storageService.presignGet(storageKey)));
	}

	@Override
	public ResponseEntity<FilePreviewsResponseV1> getFilePreviews(FilePreviewsRequestV1 request) {
		AppUser appUser = currentUser.requireUser();
		List<String> storageKeys = request.getStorageKeys().stream().distinct().toList();
		storageKeys.forEach(storageKey -> authService.requireAccess(appUser, storageKey));
		return ResponseEntity.ok(apiResponses.filePreviews(storageService.presignGet(storageKeys)));
	}
}
