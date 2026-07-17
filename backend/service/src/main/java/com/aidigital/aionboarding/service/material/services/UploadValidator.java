package com.aidigital.aionboarding.service.material.services;

import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Validates uploaded multipart files for lesson file uploads (D-08).
 * Enforces that the file is non-empty, has a non-blank filename, and has a positive size.
 * Content-type is extracted as reported by the client;
 * {@link com.aidigital.aionboarding.service.storage.StorageService}
 * enforces it against the upload purpose's allowlist before the object is written.
 */
@Component
@RequiredArgsConstructor
public class UploadValidator {

	/**
	 * Holds the validated metadata extracted from a successfully validated {@link MultipartFile}.
	 *
	 * @param originalName original client-supplied filename; never blank
	 * @param mimeType     MIME type reported by the client; may be {@code null}
	 * @param sizeBytes    file size in bytes; always positive
	 */
	public record UploadValidationRecord(String originalName, String mimeType, long sizeBytes) {

	}

	/**
	 * Validates the given multipart file and returns its metadata.
	 *
	 * @param file the uploaded file to validate
	 * @return {@link UploadValidationRecord} containing validated metadata
	 * @throws AppException with {@link ErrorReason#C002} when file is null, empty,
	 *                      has a blank filename, or has zero size
	 */
	public UploadValidationRecord validate(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new AppException(ErrorReason.C002, "file is required");
		}
		String originalName = file.getOriginalFilename();
		if (originalName == null || originalName.isBlank()) {
			throw new AppException(ErrorReason.C002, "file name is required");
		}
		long sizeBytes = file.getSize();
		if (sizeBytes <= 0) {
			throw new AppException(ErrorReason.C002, "file size must be greater than 0");
		}
		String mimeType = file.getContentType();
		return new UploadValidationRecord(originalName, mimeType, sizeBytes);
	}
}
