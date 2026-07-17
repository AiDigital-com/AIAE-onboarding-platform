package com.aidigital.aionboarding.support;

import com.aidigital.aionboarding.api.v1.model.AvatarUploadResponseV1;
import com.aidigital.aionboarding.api.v1.model.FilePreviewItemV1;
import com.aidigital.aionboarding.api.v1.model.FilePreviewResponseV1;
import com.aidigital.aionboarding.api.v1.model.FilePreviewsResponseV1;
import com.aidigital.aionboarding.api.v1.model.OkIdResponseV1;
import com.aidigital.aionboarding.api.v1.model.OkResponseV1;
import com.aidigital.aionboarding.api.v1.model.UploadUrlResponseV1;
import com.aidigital.aionboarding.service.storage.models.FilePreviewRecord;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Centralised factory for simple generated API response DTOs.
 * Controllers must not construct *V1 types inline — they delegate here instead.
 */
@Component
public class ApiResponses {

    /**
     * Builds the shared "ok" acknowledgement response.
     *
     * @return an {@link OkResponseV1} with {@code ok} set to {@code true}
     */
    public OkResponseV1 ok() {
        return new OkResponseV1().ok(true);
    }

    /**
     * Builds an "ok" acknowledgement response carrying the affected entity's id.
     *
     * @param id the affected entity's id
     * @return an {@link OkIdResponseV1} with {@code ok} set to {@code true} and the given id
     */
    public OkIdResponseV1 okId(Long id) {
        return new OkIdResponseV1().ok(true).id(id);
    }

    /**
     * Builds an upload-url response describing a presigned upload target.
     *
     * @param uploadUrl the presigned URL clients should upload to
     * @param storageKey the storage key the uploaded object will be stored under
     * @return an {@link UploadUrlResponseV1} carrying both values
     */
    public UploadUrlResponseV1 uploadUrl(String uploadUrl, String storageKey) {
        return new UploadUrlResponseV1()
                .uploadUrl(uploadUrl)
                .storageKey(storageKey);
    }

    /**
     * Builds an avatar-upload response carrying the stored avatar's storage key.
     *
     * @param storageKey the storage key the uploaded avatar was stored under
     * @return an {@link AvatarUploadResponseV1} carrying the storage key
     */
    public AvatarUploadResponseV1 avatarUpload(String storageKey) {
        return new AvatarUploadResponseV1().storageKey(storageKey);
    }

    /**
     * Builds a file-preview response carrying a presigned preview URL.
     *
     * @param previewUrl the presigned URL clients should use to preview the file
     * @return a {@link FilePreviewResponseV1} carrying the preview URL
     */
    public FilePreviewResponseV1 filePreview(String previewUrl) {
        return new FilePreviewResponseV1().previewUrl(previewUrl);
    }

    /**
     * Builds a batch file-previews response carrying one signed preview URL per resolved key.
     *
     * @param previews resolved storage-key/preview-URL pairs
     * @return a {@link FilePreviewsResponseV1} carrying one {@link FilePreviewItemV1} per entry
     */
    public FilePreviewsResponseV1 filePreviews(List<FilePreviewRecord> previews) {
        List<FilePreviewItemV1> items = previews.stream()
            .map(preview -> new FilePreviewItemV1().storageKey(preview.storageKey()).previewUrl(preview.previewUrl()))
            .toList();
        return new FilePreviewsResponseV1().previews(items);
    }
}
