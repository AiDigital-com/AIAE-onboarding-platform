package com.aidigital.aionboarding.service.material.services;

import com.aidigital.aionboarding.domain.material.entities.Material;
import com.aidigital.aionboarding.domain.material.entities.MaterialFile;
import com.aidigital.aionboarding.domain.material.repositories.MaterialFileSummaryProjection;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.material.models.MaterialAttachmentInput;
import com.aidigital.aionboarding.service.material.models.MaterialOpenAiUploadInput;
import com.aidigital.aionboarding.service.material.models.MaterialOpenAiUploadRecord;

import java.util.Collection;
import java.util.List;

/**
 * Persists material file attachments, manages OpenAI upload metadata, and cleans up storage objects.
 */
public interface MaterialFileService {

    /**
     * Persists file attachment rows for the given material.
     *
     * @param viewer authenticated user the uploads must have been issued to
     * @param material parent material entity
     * @param attachments normalized attachment inputs to persist
     */
    void saveAttachments(AppUser viewer, Material material, List<MaterialAttachmentInput> attachments);

    /**
     * Reconciles persisted attachments with an update payload while retaining existing row identifiers.
     *
     * @param viewer authenticated user the uploads must have been issued to
     * @param material parent material entity
     * @param attachments complete normalized attachment list from the update request
     * @throws com.aidigital.aionboarding.service.common.error.AppException when an attachment id is invalid
     */
    void reconcileAttachments(AppUser viewer, Material material, List<MaterialAttachmentInput> attachments);

    /**
     * Updates OpenAI upload metadata for a material file row.
     *
     * @param fileId material file identifier
     * @param input OpenAI upload fields to store
     * @return updated upload record, or {@code null} when the file row does not exist
     */
    MaterialOpenAiUploadRecord updateMaterialFileOpenAIUpload(Long fileId, MaterialOpenAiUploadInput input);

    /**
     * Deletes storage objects for the given keys without failing the surrounding transaction.
     *
     * @param storageKeys object storage keys to remove
     */
    void deleteStorageKeysQuietly(List<String> storageKeys);

    /**
     * Returns non-blank storage keys currently associated with a material.
     *
     * @param materialId material identifier
     * @return storage keys attached to the material
     */
    List<String> collectStorageKeys(Long materialId);

    /**
     * Returns storage keys present before an update but absent from the new attachment list.
     *
     * @param existingStorageKeys storage keys captured before related rows were replaced
     * @param attachments new attachment inputs
     * @return storage keys that should be removed from object storage
     */
    List<String> findRemovedStorageKeys(List<String> existingStorageKeys, List<MaterialAttachmentInput> attachments);

    /**
     * Deletes all file rows for the given material.
     *
     * @param materialId material identifier
     */
    void deleteByMaterialId(Long materialId);

    /**
     * Returns the persisted file attachments (mapped to {@link MaterialAttachmentInput}) for all given
     * material IDs, preserving per-material {@code createdAt} order. An empty or null input returns an
     * empty list.
     *
     * @param materialIds material identifiers to query
     * @return list of attachment inputs, in per-material creation order
     */
    List<MaterialAttachmentInput> findAttachmentsForMaterials(List<Long> materialIds);

    /**
     * Returns all file rows for the given material, joined with their kind.
     *
     * @param materialId material identifier
     * @return file attachments for the material, unordered
     */
    List<MaterialFile> findByMaterialId(Long materialId);

    /**
     * Returns all file rows for the given material, ordered by creation time ascending.
     *
     * @param materialId material identifier
     * @return file attachments for the material, oldest first
     */
    List<MaterialFile> findByMaterialIdOrderByCreatedAtAsc(Long materialId);

    /**
     * Returns file rows for exactly the given materials, ordered by creation time ascending.
     *
     * @param materialIds material identifiers to load rows for
     * @return file attachments for the given materials, oldest first
     */
    List<MaterialFile> findByMaterialIdsOrderByCreatedAtAsc(Collection<Long> materialIds);

    /**
     * Returns bounded attachment summary projections for the given materials, omitting the
     * OpenAI file-upload internals a Library search card does not need.
     *
     * @param materialIds material identifiers to load summaries for
     * @return attachment summaries for the given materials, oldest first
     */
    List<MaterialFileSummaryProjection> findSummariesByMaterialIds(Collection<Long> materialIds);
}
