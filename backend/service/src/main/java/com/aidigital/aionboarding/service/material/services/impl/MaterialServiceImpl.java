package com.aidigital.aionboarding.service.material.services.impl;

import com.aidigital.aionboarding.domain.material.entities.Material;
import com.aidigital.aionboarding.domain.material.repositories.MaterialRepository;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.material.models.CreateMaterialInput;
import com.aidigital.aionboarding.service.material.models.MaterialListQuery;
import com.aidigital.aionboarding.service.material.models.MaterialOpenAiUploadInput;
import com.aidigital.aionboarding.service.material.models.MaterialOpenAiUploadRecord;
import com.aidigital.aionboarding.service.material.models.MaterialRecord;
import com.aidigital.aionboarding.service.material.models.MaterialSearchSummaryRecord;
import com.aidigital.aionboarding.service.material.models.UpdateMaterialInput;
import com.aidigital.aionboarding.service.material.services.MaterialFileService;
import com.aidigital.aionboarding.service.material.services.MaterialLinkService;
import com.aidigital.aionboarding.service.material.services.MaterialLinkService.PreparedLinkRecord;
import com.aidigital.aionboarding.service.material.services.MaterialRecordQueryService;
import com.aidigital.aionboarding.service.material.services.MaterialService;
import com.aidigital.aionboarding.service.material.services.MaterialYoutubeService;
import com.aidigital.aionboarding.service.material.services.MaterialYoutubeService.PreparedYoutubeRecord;
import com.aidigital.aionboarding.service.material.validation.MaterialPayloadValidator;
import com.aidigital.aionboarding.service.material.validation.MaterialPayloadValidator.ValidatedMaterialPayload;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MaterialServiceImpl implements MaterialService {

	private static final Logger log = LoggerFactory.getLogger(MaterialServiceImpl.class);

	private final MaterialRepository materialRepository;
	private final PermissionService permissionService;
	private final MaterialRecordQueryService materialRecordQueryService;
	private final MaterialPayloadValidator materialPayloadValidator;
	private final MaterialYoutubeService materialYoutubeService;
	private final MaterialLinkService materialLinkService;
	private final MaterialFileService materialFileService;
	private final MaterialPersistenceService materialPersistenceService;

	@Override
	@Transactional
	public Page<MaterialSearchSummaryRecord> getAll(AppUser viewer, MaterialListQuery query, int page, int size) {
		return materialRecordQueryService.loadMaterialSummaries(query, page, size);
	}

	@Override
	@Transactional(readOnly = true)
	public long count(AppUser viewer, MaterialListQuery query) {
		return materialRecordQueryService.countSummaries(query);
	}

	@Override
	@Transactional(readOnly = true)
	public MaterialRecord getById(Long id) {
		Material material = materialRepository.findById(id)
				.orElseThrow(() -> new AppException(ErrorReason.C001, id));
		long usageCount = materialRecordQueryService.countLessonUsage(id);
		return materialRecordQueryService.loadRecord(material, usageCount);
	}

	@Override
	@Transactional
	public List<MaterialRecord> getByIds(List<Long> materialIds) {
		List<Long> uniqueIds = materialIds == null
				? List.of()
				: materialIds.stream().filter(Objects::nonNull).distinct().toList();
		if (uniqueIds.isEmpty()) {
			return List.of();
		}

		Map<Long, MaterialRecord> materialById =
				materialRecordQueryService.loadMaterialRecordsByIds(uniqueIds).stream()
				.collect(Collectors.toMap(MaterialRecord::id, item -> item, (left, right) -> left,
						LinkedHashMap::new));

		return uniqueIds.stream()
				.map(materialById::get)
				.filter(Objects::nonNull)
				.toList();
	}

	/**
	 * Non-@Transactional orchestrator: performs permission checks, validates input,
	 * pre-fetches link and YouTube metadata (network calls outside any TX), then
	 * delegates to the transactional persistence boundary.
	 */
	@Override
	public MaterialRecord create(AppUser viewer, CreateMaterialInput input) {
		permissionService.requirePermission(viewer, PermissionKeys.MATERIALS_CREATE);
		ValidatedMaterialPayload payload = materialPayloadValidator.validateCreateInput(input);

		List<PreparedLinkRecord> linkRecords = materialLinkService.prepareLinks(payload.links());
		List<PreparedYoutubeRecord> youtubeRecords =
				materialYoutubeService.prepareYoutubeMetadata(payload.youtubeUrls());

		return materialPersistenceService.create(viewer, input, payload, linkRecords, youtubeRecords);
	}

	/**
	 * Non-@Transactional orchestrator: collects existing storage keys and pre-fetches
	 * link/YouTube metadata before opening the write transaction.
	 */
	@Override
	public MaterialRecord update(AppUser viewer, Long id, UpdateMaterialInput input) {
		permissionService.requirePermission(viewer, PermissionKeys.MATERIALS_EDIT);

		ValidatedMaterialPayload payload = materialPayloadValidator.validateUpdateInput(input);

		// Collect existing keys and pre-fetch metadata outside the write transaction.
		List<String> existingStorageKeys = materialFileService.collectStorageKeys(id);
		List<PreparedLinkRecord> linkRecords = materialLinkService.prepareLinks(payload.links());
		List<PreparedYoutubeRecord> youtubeRecords =
				materialYoutubeService.prepareYoutubeMetadata(payload.youtubeUrls());

		return materialPersistenceService.update(viewer, id, input, payload, linkRecords, youtubeRecords,
				existingStorageKeys);
	}

	@Override
	@Transactional
	public void delete(AppUser viewer, Long id) {
		permissionService.requirePermission(viewer, PermissionKeys.MATERIALS_DELETE);
		Material material = materialRepository.findById(id)
				.orElseThrow(() -> new AppException(ErrorReason.C001, id));

		materialRecordQueryService.requireDeletable(id);

		List<String> storageKeys = materialFileService.collectStorageKeys(id);

		materialYoutubeService.deleteByMaterialId(id);
		materialLinkService.deleteByMaterialId(id);
		materialFileService.deleteByMaterialId(id);
		materialRepository.delete(material);

		List<String> keysToDelete = List.copyOf(storageKeys);
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				try {
					materialFileService.deleteStorageKeysQuietly(keysToDelete);
				} catch (RuntimeException e) {
					log.warn("Storage cleanup failed after material delete (non-fatal): {}", e.getMessage());
				}
			}
		});
	}

	@Override
	@Transactional
	public MaterialOpenAiUploadRecord updateMaterialFileOpenAIUpload(Long fileId, MaterialOpenAiUploadInput input) {
		return materialFileService.updateMaterialFileOpenAIUpload(fileId, input);
	}
}
