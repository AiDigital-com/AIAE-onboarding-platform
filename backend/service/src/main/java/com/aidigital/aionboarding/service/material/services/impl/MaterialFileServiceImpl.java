package com.aidigital.aionboarding.service.material.services.impl;

import com.aidigital.aionboarding.domain.common.dictionary.MaterialFileKindCode;
import com.aidigital.aionboarding.domain.common.dictionary.entities.MaterialFileKind;
import com.aidigital.aionboarding.domain.material.entities.Material;
import com.aidigital.aionboarding.domain.material.entities.MaterialFile;
import com.aidigital.aionboarding.domain.material.repositories.MaterialFileRepository;
import com.aidigital.aionboarding.domain.material.repositories.MaterialFileSummaryProjection;
import com.aidigital.aionboarding.service.common.dictionary.DictionaryLookupService;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.mapping.TextValueNormalizer;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.mappers.material.MaterialMapper;
import com.aidigital.aionboarding.service.material.models.MaterialAttachmentInput;
import com.aidigital.aionboarding.service.material.models.MaterialOpenAiUploadInput;
import com.aidigital.aionboarding.service.material.models.MaterialOpenAiUploadRecord;
import com.aidigital.aionboarding.service.material.services.MaterialFileService;
import com.aidigital.aionboarding.service.material.support.MaterialFileQuerySupport;
import com.aidigital.aionboarding.service.storage.StorageService;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MaterialFileServiceImpl implements MaterialFileService {

    private final MaterialFileRepository materialFileRepository;
    private final DictionaryLookupService dictionaryLookupService;
    private final StorageService storageService;
    private final MaterialMapper materialMapper;
    private final MaterialFileQuerySupport materialFileQuerySupport;
    private final TextValueNormalizer textValueNormalizer;
    private final CurrentTime currentTime;

    @Override
    public void saveAttachments(AppUser viewer, Material material, List<MaterialAttachmentInput> attachments) {
        for (MaterialAttachmentInput attachment : attachments) {
            MaterialFile entity = newMaterialFile(viewer, material, attachment);
            materialFileRepository.save(entity);
        }
    }

    @Override
    public void reconcileAttachments(AppUser viewer, Material material, List<MaterialAttachmentInput> attachments) {
        Map<Long, MaterialFile> remainingById = materialFileRepository.findByMaterialId(material.getId()).stream()
            .collect(Collectors.toMap(
                MaterialFile::getId,
                Function.identity(),
                (left, right) -> left,
                LinkedHashMap::new
            ));

        for (MaterialAttachmentInput attachment : attachments) {
            MaterialFile entity;
            if (attachment.id() == null) {
                entity = newMaterialFile(viewer, material, attachment);
            } else {
                entity = remainingById.remove(attachment.id());
                if (entity == null) {
                    throw new AppException(
                        ErrorReason.C002,
                        "Attachment " + attachment.id() + " does not belong to material " + material.getId() + "."
                    );
                }
                applyAttachment(viewer, entity, attachment);
            }

            materialFileRepository.save(entity);
        }

        if (!remainingById.isEmpty()) {
            materialFileRepository.deleteAll(remainingById.values());
        }
    }

    @Override
    public MaterialOpenAiUploadRecord updateMaterialFileOpenAIUpload(Long fileId, MaterialOpenAiUploadInput input) {
        MaterialFile file = materialFileRepository.findById(fileId).orElse(null);
        if (file == null) {
            return null;
        }

        String openaiFileId = textValueNormalizer.trimmed(input.openaiFileId());
        if (openaiFileId.isBlank()) {
            openaiFileId = null;
        }

        file.setOpenaiFileId(openaiFileId);
        file.setOpenaiFilePurpose(textValueNormalizer.raw(input.openaiFilePurpose()));
        file.setOpenaiFileStatus(textValueNormalizer.raw(input.openaiFileStatus()));
        file.setOpenaiFileError(textValueNormalizer.raw(input.openaiFileError()));
        if (openaiFileId != null) {
            file.setOpenaiUploadedAt(currentTime.utcDateTime());
        }

        materialFileRepository.save(file);
        return materialMapper.toOpenAiUploadRecord(file);
    }

    @Override
    public void deleteStorageKeysQuietly(List<String> storageKeys) {
        if (storageKeys == null || storageKeys.isEmpty()) {
            return;
        }
        List<String> uniqueKeys = storageKeys.stream()
            .filter(key -> key != null && !key.isBlank())
            .distinct()
            .toList();
        if (uniqueKeys.isEmpty()) {
            return;
        }
        try {
            storageService.deleteObjects(uniqueKeys);
        } catch (RuntimeException ignored) {
            // Match the Next.js route behavior: storage cleanup failures are logged but non-fatal.
        }
    }

    @Override
    public List<String> collectStorageKeys(Long materialId) {
        return materialFileQuerySupport.collectStorageKeys(materialId);
    }

    @Override
    public List<String> findRemovedStorageKeys(
        List<String> existingStorageKeys,
        List<MaterialAttachmentInput> attachments
    ) {
        return materialFileQuerySupport.findRemovedStorageKeys(existingStorageKeys, attachments);
    }

    @Override
    public void deleteByMaterialId(Long materialId) {
        materialFileRepository.deleteByMaterial_Id(materialId);
    }

    @Override
    public List<MaterialAttachmentInput> findAttachmentsForMaterials(List<Long> materialIds) {
        return materialFileQuerySupport.findAttachmentsForMaterials(materialIds);
    }

    @Override
    public List<MaterialFile> findByMaterialId(Long materialId) {
        return materialFileQuerySupport.findByMaterialId(materialId);
    }

    @Override
    public List<MaterialFile> findByMaterialIdOrderByCreatedAtAsc(Long materialId) {
        return materialFileQuerySupport.findByMaterialIdOrderByCreatedAtAsc(materialId);
    }

    @Override
    public List<MaterialFile> findByMaterialIdsOrderByCreatedAtAsc(Collection<Long> materialIds) {
        return materialFileQuerySupport.findByMaterialIdsOrderByCreatedAtAsc(materialIds);
    }

    @Override
    public List<MaterialFileSummaryProjection> findSummariesByMaterialIds(Collection<Long> materialIds) {
        if (materialIds == null || materialIds.isEmpty()) {
            return List.of();
        }
        return materialFileRepository.findSummariesByMaterialIdIn(materialIds);
    }

    /**
     * Creates a new material file entity from a normalized attachment input.
     */
    MaterialFile newMaterialFile(AppUser viewer, Material material, MaterialAttachmentInput attachment) {
        String storageKey = textValueNormalizer.raw(attachment.storageKey());
        if (!storageKey.isBlank()) {
            storageService.confirmUpload(viewer, storageKey);
        }
        MaterialFileKind kind = resolveKind(attachment.kind());
        return materialMapper.toNewMaterialFile(
            material,
            kind,
            textValueNormalizer.raw(attachment.originalName()),
            textValueNormalizer.raw(attachment.storageKey()),
            textValueNormalizer.raw(attachment.mimeType()),
            attachment.sizeBytes(),
            blankToNull(attachment.openaiFileId()),
            textValueNormalizer.raw(attachment.openaiFilePurpose()),
            textValueNormalizer.raw(attachment.openaiFileStatus()),
            textValueNormalizer.raw(attachment.openaiFileError()),
            attachment.openaiUploadedAt(),
            currentTime.utcDateTime()
        );
    }

    /**
     * Applies a normalized attachment input to an existing material file entity.
     */
    void applyAttachment(AppUser viewer, MaterialFile file, MaterialAttachmentInput attachment) {
        String storageKey = textValueNormalizer.raw(attachment.storageKey());
        if (!storageKey.isBlank() && !storageKey.equals(file.getStorageKey())) {
            storageService.confirmUpload(viewer, storageKey);
        }
        materialMapper.updateMaterialFile(
            file,
            resolveKind(attachment.kind()),
            textValueNormalizer.raw(attachment.originalName()),
            textValueNormalizer.raw(attachment.storageKey()),
            textValueNormalizer.raw(attachment.mimeType()),
            attachment.sizeBytes(),
            blankToNull(attachment.openaiFileId()),
            textValueNormalizer.raw(attachment.openaiFilePurpose()),
            textValueNormalizer.raw(attachment.openaiFileStatus()),
            textValueNormalizer.raw(attachment.openaiFileError()),
            attachment.openaiUploadedAt()
        );
    }

    /**
     * Resolves the material file kind reference for an attachment kind code.
     */
    MaterialFileKind resolveKind(String value) {
        String code = textValueNormalizer.firstNonBlankTrimmed(value, MaterialFileKindCode.FILE);
        Long kindId = dictionaryLookupService.materialFileKindId(code);
        return materialMapper.toMaterialFileKindReference(kindId);
    }

    /**
     * Converts blank OpenAI file identifiers to null for persistence.
     */
    String blankToNull(String value) {
        String normalized = textValueNormalizer.trimmed(value);
        return normalized.isBlank() ? null : normalized;
    }
}
