package com.aidigital.aionboarding.service.material.services.impl;

import com.aidigital.aionboarding.domain.material.entities.Material;
import com.aidigital.aionboarding.domain.material.repositories.MaterialRepository;
import com.aidigital.aionboarding.domain.user.repositories.UserRepository;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.mapping.TextValueNormalizer;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.mappers.material.MaterialMapper;
import com.aidigital.aionboarding.service.material.models.CreateMaterialInput;
import com.aidigital.aionboarding.service.material.models.MaterialRecord;
import com.aidigital.aionboarding.service.material.models.UpdateMaterialInput;
import com.aidigital.aionboarding.service.material.services.MaterialFileService;
import com.aidigital.aionboarding.service.material.services.MaterialLinkService;
import com.aidigital.aionboarding.service.material.services.MaterialLinkService.PreparedLinkRecord;
import com.aidigital.aionboarding.service.material.services.MaterialRecordQueryService;
import com.aidigital.aionboarding.service.material.services.MaterialYoutubeService;
import com.aidigital.aionboarding.service.material.services.MaterialYoutubeService.PreparedYoutubeRecord;
import com.aidigital.aionboarding.service.material.validation.MaterialPayloadValidator.ValidatedMaterialPayload;
import com.aidigital.aionboarding.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Transactional persistence boundary for material create and update workflows.
 */
@Service
@RequiredArgsConstructor
public class MaterialPersistenceService {

	private static final Logger log = LoggerFactory.getLogger(MaterialPersistenceService.class);

	private final MaterialRepository materialRepository;
	private final UserRepository userRepository;
	private final MaterialRecordQueryService materialRecordQueryService;
	private final MaterialYoutubeService materialYoutubeService;
	private final MaterialLinkService materialLinkService;
	private final MaterialFileService materialFileService;
	private final TextValueNormalizer textValueNormalizer;
	private final MaterialMapper materialMapper;
	private final CurrentTime currentTime;
	private final StorageService storageService;

	/**
	 * Persists a material and all prepared child rows inside one transaction.
	 *
	 * @param viewer         authenticated material owner
	 * @param input          original create request
	 * @param payload        validated and normalized material payload
	 * @param linkRecords    pre-fetched link metadata
	 * @param youtubeRecords pre-fetched YouTube metadata
	 * @return persisted material record
	 */
	@Transactional
	public MaterialRecord create(
			AppUser viewer,
			CreateMaterialInput input,
			ValidatedMaterialPayload payload,
			List<PreparedLinkRecord> linkRecords,
			List<PreparedYoutubeRecord> youtubeRecords
	) {
		String coverImageStorageKey = textValueNormalizer.raw(input.coverImageStorageKey());
		if (!coverImageStorageKey.isBlank()) {
			storageService.confirmUpload(viewer, coverImageStorageKey);
		}

		LocalDateTime timestamp = currentTime.utcDateTime();
		Material material = materialMapper.toNewMaterial(
				payload,
				coverImageStorageKey,
				textValueNormalizer.raw(input.coverImageOriginalName()),
				textValueNormalizer.raw(input.coverImageMimeType()),
				viewer.name(),
				userRepository.getReferenceById(viewer.internalId()),
				timestamp
		);
		material = materialRepository.save(material);

		materialYoutubeService.saveYoutubeUrls(material, youtubeRecords);
		materialLinkService.saveLinks(material, linkRecords);
		materialFileService.saveAttachments(viewer, material, payload.attachments());

		return materialRecordQueryService.loadRecord(material, 0L);
	}

	/**
	 * Replaces a material and all prepared child rows inside one transaction.
	 *
	 * @param viewer              authenticated user performing the update
	 * @param id                  material identifier
	 * @param input               original update request
	 * @param payload             validated and normalized material payload
	 * @param linkRecords         pre-fetched link metadata
	 * @param youtubeRecords      pre-fetched YouTube metadata
	 * @param existingStorageKeys storage keys captured before child row reconciliation
	 * @return updated material record
	 */
	@Transactional
	public MaterialRecord update(
			AppUser viewer,
			Long id,
			UpdateMaterialInput input,
			ValidatedMaterialPayload payload,
			List<PreparedLinkRecord> linkRecords,
			List<PreparedYoutubeRecord> youtubeRecords,
			List<String> existingStorageKeys
	) {
		Material material = materialRepository.findById(id)
				.orElseThrow(() -> new AppException(ErrorReason.C001, id));

		String coverImageStorageKey = textValueNormalizer.raw(input.coverImageStorageKey());
		if (!coverImageStorageKey.isBlank() && !coverImageStorageKey.equals(material.getCoverImageStorageKey())) {
			storageService.confirmUpload(viewer, coverImageStorageKey);
		}

		materialMapper.updateMaterial(
				material,
				payload,
				coverImageStorageKey,
				textValueNormalizer.raw(input.coverImageOriginalName()),
				textValueNormalizer.raw(input.coverImageMimeType()),
				currentTime.utcDateTime()
		);
		materialRepository.save(material);

		materialYoutubeService.deleteByMaterialId(id);
		materialLinkService.deleteByMaterialId(id);

		materialYoutubeService.saveYoutubeUrls(material, youtubeRecords);
		materialLinkService.saveLinks(material, linkRecords);
		materialFileService.reconcileAttachments(viewer, material, payload.attachments());

		List<String> keysToDelete = List.copyOf(
				materialFileService.findRemovedStorageKeys(existingStorageKeys, payload.attachments())
		);
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				try {
					materialFileService.deleteStorageKeysQuietly(keysToDelete);
				} catch (RuntimeException e) {
					log.warn("Storage cleanup failed after material update (non-fatal): {}", e.getMessage());
				}
			}
		});

		return materialRecordQueryService.loadRecord(
				material,
				materialRecordQueryService.countLessonUsage(material.getId())
		);
	}
}
