package com.aidigital.aionboarding.support;

import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.material.services.UploadValidator;
import com.aidigital.aionboarding.service.storage.StorageService;
import com.aidigital.aionboarding.service.storage.enums.UploadPurpose;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * Centralises the {@code MultipartFile}-handling boilerplate — presence checks and the checked
 * {@link IOException} from reading upload bytes — that API controllers may not express as
 * branches or {@code try/catch} blocks. This is the only place in the {@code application} module
 * that owns that translation; {@code MultipartFile} itself must never cross into {@code service}.
 */
@Component
@RequiredArgsConstructor
public class MultipartFileUploadSupport {

    private final UploadValidator uploadValidator;
    private final StorageService storageService;

    /**
     * Rejects a missing or empty upload with a caller-supplied message.
     *
     * @param file    the submitted multipart file, possibly {@code null}
     * @param message the message carried by the thrown exception
     * @throws AppException with {@link ErrorReason#C002} when the file is missing or empty
     */
    public void requireNonEmpty(MultipartFile file, String message) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorReason.C002, message);
        }
    }

    /**
     * Rejects a missing, empty, unnamed, or zero-length upload using the generic file messages,
     * then validates the remaining metadata through {@link UploadValidator}.
     *
     * @param file the submitted multipart file, possibly {@code null}
     * @throws AppException with {@link ErrorReason#C002} when the file is missing, empty,
     *                       unnamed, or non-positive in size
     */
    public void requireValidFile(MultipartFile file) {
        requireNonEmpty(file, "file is required");
        uploadValidator.validate(file.getOriginalFilename(), file.getContentType(), file.getSize());
    }

    /**
     * Streams a multipart upload's bytes into object storage, using the file's own reported
     * name, content type, and size.
     *
     * @param owner   authenticated owner of the resulting object
     * @param purpose upload purpose controlling the storage policy
     * @param file    the submitted multipart file
     * @return the storage key the uploaded object was written to
     * @throws AppException with {@link ErrorReason#C000} when the upload stream cannot be read
     */
    public String putStreaming(AppUser owner, UploadPurpose purpose, MultipartFile file) {
        try (InputStream content = file.getInputStream()) {
            return storageService.putObjectStreaming(owner, purpose, content, file.getSize(),
                    file.getOriginalFilename(), file.getContentType());
        } catch (IOException ex) {
            throw new AppException(ErrorReason.C000, ex.getMessage());
        }
    }

    /**
     * Streams a multipart upload's bytes into object storage, using previously validated
     * metadata rather than the file's own (possibly unvalidated) values.
     *
     * @param owner   authenticated owner of the resulting object
     * @param purpose upload purpose controlling the storage policy
     * @param file    the submitted multipart file
     * @param meta    metadata already validated by {@link UploadValidator}
     * @return the storage key the uploaded object was written to
     * @throws AppException with {@link ErrorReason#C000} when the upload stream cannot be read
     */
    public String putStreaming(
            AppUser owner,
            UploadPurpose purpose,
            MultipartFile file,
            UploadValidator.UploadValidationRecord meta
    ) {
        try (InputStream content = file.getInputStream()) {
            return storageService.putObjectStreaming(owner, purpose, content, meta.sizeBytes(),
                    meta.originalName(), meta.mimeType());
        } catch (IOException ex) {
            throw new AppException(ErrorReason.C000, ex.getMessage());
        }
    }

    /**
     * Buffers a multipart upload's bytes into memory and stores them in one call, defaulting a
     * blank filename to the given fallback.
     *
     * @param owner        authenticated owner of the resulting object
     * @param purpose      upload purpose controlling the storage policy
     * @param file         the submitted multipart file
     * @param fallbackName name used when the file reports no original filename
     * @return the storage key the uploaded object was written to
     * @throws AppException with {@link ErrorReason#C000} when the upload bytes cannot be read
     */
    public String putBuffered(AppUser owner, UploadPurpose purpose, MultipartFile file, String fallbackName) {
        try {
            return storageService.putObject(owner, purpose, file.getBytes(),
                    file.getOriginalFilename() == null ? fallbackName : file.getOriginalFilename(),
                    file.getContentType());
        } catch (IOException ex) {
            throw new AppException(ErrorReason.C000, ex.getMessage());
        }
    }
}
