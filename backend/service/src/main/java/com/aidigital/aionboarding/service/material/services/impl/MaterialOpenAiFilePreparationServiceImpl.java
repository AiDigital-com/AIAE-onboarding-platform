package com.aidigital.aionboarding.service.material.services.impl;

import com.aidigital.aionboarding.external.openai.OpenAiClient;
import com.aidigital.aionboarding.external.openai.OpenAiExternalException;
import com.aidigital.aionboarding.external.openai.model.OpenAiFileInput;
import com.aidigital.aionboarding.external.openai.model.OpenAiFileUploadResponse;
import com.aidigital.aionboarding.service.material.models.MaterialAttachmentInput;
import com.aidigital.aionboarding.service.material.models.MaterialOpenAiUploadInput;
import com.aidigital.aionboarding.service.material.services.MaterialFileService;
import com.aidigital.aionboarding.service.material.services.MaterialOpenAiFilePreparationService;
import com.aidigital.aionboarding.service.storage.StorageService;
import com.aidigital.aionboarding.service.storage.enums.UploadPurpose;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Uploads or reuses compatible material file attachments via the OpenAI Files API, persists
 * the upload outcome to the {@code material_files} OpenAI columns, and returns a typed
 * {@link OpenAiFileInput} list for the generation request (D-05, D-06, D-07).
 */
@Service
@RequiredArgsConstructor
public class MaterialOpenAiFilePreparationServiceImpl implements MaterialOpenAiFilePreparationService {

    private static final Logger LOG = LoggerFactory.getLogger(MaterialOpenAiFilePreparationServiceImpl.class);

    /** Supported MIME type for PDF documents. */
    private static final String MIME_PDF = "application/pdf";

    /** MIME prefix for all image subtypes (jpeg, png, gif, webp, etc.). */
    private static final String MIME_IMAGE_PREFIX = "image/";

    /** Supported MIME type for plain text documents. */
    private static final String MIME_TEXT_PLAIN = "text/plain";

    /** Full set of supported exact MIME types (non-prefix). */
    private static final Set<String> SUPPORTED_EXACT_MIME_TYPES = Set.of(
        MIME_PDF,
        MIME_TEXT_PLAIN
    );

    /** OpenAI file purpose used for material attachments. */
    private static final String FILE_PURPOSE = "user_data";

    /** Upload status value indicating a successful upload. */
    private static final String STATUS_UPLOADED = "uploaded";

    /** Upload status value indicating a failed upload. */
    private static final String STATUS_ERROR = "error";

    private final ObjectProvider<OpenAiClient> openAiClientProvider;
    private final MaterialFileService materialFileService;
    private final StorageService storageService;

    @Override
    public List<OpenAiFileInput> prepareFileInputs(List<Long> materialIds) {
        if (materialIds == null || materialIds.isEmpty()) {
            return List.of();
        }
        OpenAiClient client = findClient();
        if (client == null) {
            LOG.warn("OpenAI client is not configured — skipping file preparation for {} material ID(s)", materialIds.size());
            return List.of();
        }

        List<MaterialAttachmentInput> attachments = materialFileService.findAttachmentsForMaterials(materialIds);
        List<OpenAiFileInput> result = new ArrayList<>();

        for (MaterialAttachmentInput attachment : attachments) {
            if (!isCompatible(attachment)) {
                continue;
            }
            String fileId = attachment.openaiFileId();
            if (fileId != null && !fileId.isBlank() && STATUS_UPLOADED.equals(attachment.openaiFileStatus())) {
                result.add(new OpenAiFileInput("input_file", fileId));
                continue;
            }
            fileId = uploadAndPersist(attachment, client);
            if (fileId != null) {
                result.add(new OpenAiFileInput("input_file", fileId));
            }
        }
        return result;
    }

    /**
     * Fetches the attachment bytes from storage, uploads them via the OpenAI Files API, and
     * persists the outcome. Returns the OpenAI file ID on success, or {@code null} on failure
     * (failure is persisted and must not abort preparation of the remaining attachments — D-06).
     *
     * @param attachment attachment to upload
     * @param client     the resolved {@link OpenAiClient} to use for the upload
     * @return the OpenAI file ID on success, {@code null} on failure
     */
    String uploadAndPersist(MaterialAttachmentInput attachment, OpenAiClient client) {
        try {
            byte[] bytes = storageService.getObjectBuffer(attachment.storageKey(), UploadPurpose.MATERIAL_UPLOAD.maxSizeBytes());
            OpenAiFileUploadResponse uploadResponse = client.uploadFile(bytes, attachment.originalName(), FILE_PURPOSE);
            materialFileService.updateMaterialFileOpenAIUpload(attachment.id(),
                new MaterialOpenAiUploadInput(uploadResponse.id(), FILE_PURPOSE, STATUS_UPLOADED, ""));
            return uploadResponse.id();
        } catch (Exception ex) {
            LOG.warn("OpenAI file upload failed for material file {}: {}", attachment.id(), ex.getMessage());
            materialFileService.updateMaterialFileOpenAIUpload(attachment.id(),
                new MaterialOpenAiUploadInput(null, FILE_PURPOSE, STATUS_ERROR, ex.getMessage()));
            return null;
        }
    }

    /**
     * Returns {@code true} when the attachment has a supported MIME type, a non-blank storage key,
     * and a non-blank original file name. Unsupported or incomplete attachments are skipped without
     * any upload or persistence attempt.
     *
     * @param attachment candidate attachment
     * @return {@code true} if the attachment should be uploaded or reused
     */
    boolean isCompatible(MaterialAttachmentInput attachment) {
        if (attachment.storageKey() == null || attachment.storageKey().isBlank()) {
            return false;
        }
        if (attachment.originalName() == null || attachment.originalName().isBlank()) {
            return false;
        }
        String mime = attachment.mimeType();
        if (mime == null || mime.isBlank()) {
            return false;
        }
        return SUPPORTED_EXACT_MIME_TYPES.contains(mime) || mime.startsWith(MIME_IMAGE_PREFIX);
    }

    /**
     * Returns the available {@link OpenAiClient}, or {@code null} when the API key is not
     * configured. Callers treat {@code null} as "no file inputs" (degrade gracefully instead
     * of throwing — mirrors the {@code LessonGenServiceImpl} pattern).
     *
     * @return the OpenAI client, or {@code null} when unavailable
     */
    OpenAiClient findClient() {
        return openAiClientProvider.getIfAvailable();
    }
}
