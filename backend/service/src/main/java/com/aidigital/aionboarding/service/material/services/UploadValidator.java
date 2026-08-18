package com.aidigital.aionboarding.service.material.services;

import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Validates uploaded file metadata for lesson file uploads (D-08).
 * Enforces that the file name is non-blank and the size is positive. This is a service-layer
 * validator, so it never accepts a {@code MultipartFile} — the only web-API type a service
 * imported before this split; the caller (a controller, in the {@code application} module)
 * extracts the plain filename/content-type/size before calling {@link #validate}.
 * Content-type is validated by
 * {@link com.aidigital.aionboarding.service.storage.StorageService} against the upload
 * purpose's allowlist before the object is written.
 */
@Component
@RequiredArgsConstructor
public class UploadValidator {

	/**
	 * Holds the validated metadata for an upload.
	 *
	 * @param originalName original client-supplied filename; never blank
	 * @param mimeType     MIME type reported by the client; may be {@code null}
	 * @param sizeBytes    file size in bytes; always positive
	 */
	public record UploadValidationRecord(String originalName, String mimeType, long sizeBytes) {

	}

	/**
	 * Validates the given upload metadata and returns it as a typed record.
	 *
	 * @param originalName client-supplied file name
	 * @param mimeType     MIME type reported by the client; may be {@code null}
	 * @param sizeBytes    file size in bytes, as reported by the transport layer
	 * @return {@link UploadValidationRecord} containing validated metadata
	 * @throws AppException with {@link ErrorReason#C002} when the file name is blank or
	 *                      missing, or the size is not positive
	 */
	public UploadValidationRecord validate(String originalName, String mimeType, long sizeBytes) {
		if (originalName == null || originalName.isBlank()) {
			throw new AppException(ErrorReason.C002, "file name is required");
		}
		if (sizeBytes <= 0) {
			throw new AppException(ErrorReason.C002, "file size must be greater than 0");
		}
		return new UploadValidationRecord(originalName, mimeType, sizeBytes);
	}

	/**
	 * Validates a presigned-upload request's declared metadata before a URL is issued.
	 *
	 * @param fileName    client-declared file name
	 * @param contentType client-declared MIME type
	 * @param sizeBytes   client-declared file size in bytes, or {@code null}
	 * @throws AppException with {@link ErrorReason#C002} when the file name is blank or missing,
	 *                       the content type is blank or missing, or the size is missing or not
	 *                       positive
	 */
	public void validatePresignRequest(String fileName, String contentType, Long sizeBytes) {
		if (fileName == null || fileName.isBlank()) {
			throw new AppException(ErrorReason.C002, "fileName is required");
		}
		if (contentType == null || contentType.isBlank()) {
			throw new AppException(ErrorReason.C002, "contentType is required");
		}
		if (sizeBytes == null || sizeBytes <= 0) {
			throw new AppException(ErrorReason.C002, "size must be greater than 0");
		}
	}
}
