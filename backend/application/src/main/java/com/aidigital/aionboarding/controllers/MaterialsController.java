package com.aidigital.aionboarding.controllers;

import com.aidigital.aionboarding.api.v1.MaterialsApi;
import com.aidigital.aionboarding.api.v1.model.CountResponseV1;
import com.aidigital.aionboarding.api.v1.model.CreateMaterialRequestV1;
import com.aidigital.aionboarding.api.v1.model.CreateMaterialResponseV1;
import com.aidigital.aionboarding.api.v1.model.MaterialResponseV1;
import com.aidigital.aionboarding.api.v1.model.MaterialsListResponseV1;
import com.aidigital.aionboarding.api.v1.model.OkIdResponseV1;
import com.aidigital.aionboarding.api.v1.model.SearchMaterialsV1;
import com.aidigital.aionboarding.api.v1.model.UpdateMaterialRequestV1;
import com.aidigital.aionboarding.api.v1.model.UploadUrlRequestV1;
import com.aidigital.aionboarding.api.v1.model.UploadUrlResponseV1;
import com.aidigital.aionboarding.api.v1.model.UploadedFileResponseV1;
import com.aidigital.aionboarding.mappers.material.MaterialApiMapper;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.material.services.MaterialService;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import com.aidigital.aionboarding.service.storage.StorageService;
import com.aidigital.aionboarding.service.storage.enums.UploadPurpose;
import com.aidigital.aionboarding.support.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class MaterialsController implements MaterialsApi {

	private final CurrentUserSupport currentUser;
	private final MaterialService materialService;
	private final StorageService storageService;
	private final MaterialApiMapper materialApiMapper;
	private final ApiResponses apiResponses;

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<MaterialsListResponseV1> searchMaterials(SearchMaterialsV1 request) {
		AppUser viewer = currentUser.requireUser();
		return ResponseEntity.ok(
				materialApiMapper.toMaterialsListResponseV1(
						materialService.getAll(
								viewer,
								materialApiMapper.toMaterialListQuery(request),
								materialApiMapper.page(request),
								materialApiMapper.size(request)
						))
		);
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<CountResponseV1> countMaterials(SearchMaterialsV1 request) {
		AppUser viewer = currentUser.requireUser();
		long totalElements = materialService.count(viewer, materialApiMapper.toMaterialListQuery(request));
		return ResponseEntity.ok(materialApiMapper.toCountResponseV1(totalElements));
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<MaterialResponseV1> getMaterial(Long id) {
		currentUser.requireUser();
		return ResponseEntity.ok(materialApiMapper.toMaterialResponseV1(materialService.getById(id)));
	}

	@Override
	@PreAuthorize("@perm.has('" + PermissionKeys.MATERIALS_CREATE + "')")
	public ResponseEntity<CreateMaterialResponseV1> createMaterial(CreateMaterialRequestV1 request) {
		AppUser viewer = currentUser.requireUser();
		return ResponseEntity.status(HttpStatus.CREATED).body(materialApiMapper.toCreateMaterialResponseV1(
				materialService.create(viewer, materialApiMapper.toCreateMaterialInput(request))
		));
	}

	@Override
	@PreAuthorize("@perm.has('" + PermissionKeys.MATERIALS_EDIT + "')")
	public ResponseEntity<OkIdResponseV1> updateMaterial(Long id, UpdateMaterialRequestV1 request) {
		AppUser viewer = currentUser.requireUser();
		materialService.update(viewer, id, materialApiMapper.toUpdateMaterialInput(request));
		return ResponseEntity.ok(apiResponses.okId(id));
	}

	@Override
	@PreAuthorize("@perm.has('" + PermissionKeys.MATERIALS_DELETE + "')")
	@Transactional
	public ResponseEntity<OkIdResponseV1> deleteMaterial(Long id) {
		AppUser viewer = currentUser.requireUser();
		materialService.delete(viewer, id);
		return ResponseEntity.ok(apiResponses.okId(id));
	}

	@Override
	@PreAuthorize("@perm.has('" + PermissionKeys.MATERIALS_CREATE + "')")
	public ResponseEntity<UploadUrlResponseV1> createMaterialUploadUrl(UploadUrlRequestV1 request) {
		AppUser viewer = currentUser.requireUser();
		validateUploadUrlRequest(request);
		var presigned = storageService.presignPut(
				viewer, UploadPurpose.MATERIAL_UPLOAD, request.getFileName(), request.getContentType(),
                request.getSize());
		return ResponseEntity.ok(apiResponses.uploadUrl(presigned.uploadUrl(), presigned.storageKey()));
	}

	@Override
	@PreAuthorize("@perm.has('" + PermissionKeys.MATERIALS_CREATE + "')")
	public ResponseEntity<UploadedFileResponseV1> uploadMaterialFile(MultipartFile file) {
		AppUser viewer = currentUser.requireUser();
		validateUploadedFile(file);
		try (java.io.InputStream content = file.getInputStream()) {
			String storageKey = storageService.putObjectStreaming(
					viewer,
					UploadPurpose.MATERIAL_UPLOAD,
					content,
					file.getSize(),
					file.getOriginalFilename(),
					file.getContentType()
			);
			return ResponseEntity.ok(materialApiMapper.toUploadedFileResponseV1(
					storageKey,
					file.getOriginalFilename(),
					file.getContentType(),
					file.getSize()
			));
		} catch (java.io.IOException ex) {
			throw new AppException(ErrorReason.C000, ex.getMessage());
		}
	}

	void validateUploadUrlRequest(UploadUrlRequestV1 request) {
		if (request.getFileName() == null || request.getFileName().isBlank()) {
			throw new AppException(ErrorReason.C002, "fileName is required");
		}
		if (request.getContentType() == null || request.getContentType().isBlank()) {
			throw new AppException(ErrorReason.C002, "contentType is required");
		}
		if (request.getSize() == null || request.getSize() <= 0) {
			throw new AppException(ErrorReason.C002, "size must be greater than 0");
		}
	}

	void validateUploadedFile(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new AppException(ErrorReason.C002, "file is required");
		}
		if (file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()) {
			throw new AppException(ErrorReason.C002, "file name is required");
		}
		if (file.getSize() <= 0) {
			throw new AppException(ErrorReason.C002, "file size must be greater than 0");
		}
	}
}
