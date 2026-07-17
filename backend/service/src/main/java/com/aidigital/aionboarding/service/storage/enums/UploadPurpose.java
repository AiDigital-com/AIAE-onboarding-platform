package com.aidigital.aionboarding.service.storage.enums;

import java.util.Set;

/**
 * Fixes the maximum size and allowed content types for one class of upload, so a presigned or
 * direct upload can be validated against a narrow, purpose-specific policy instead of a single
 * global size cap with no content-type restriction at all.
 */
public enum UploadPurpose {

	/**
	 * Profile avatar images.
	 */
	AVATAR(2L * 1024 * 1024, Set.of("image/jpeg", "image/png", "image/webp", "image/gif"), Set.of()),

	/**
	 * Material cover images and file/document attachments.
	 */
	MATERIAL_UPLOAD(20L * 1024 * 1024, Set.of("application/pdf", "text/plain"), Set.of("image/")),

	/**
	 * Lesson content assets: cover/body images, PDF/text attachments, and video assets.
	 */
	LESSON_ASSET(300L * 1024 * 1024, Set.of("application/pdf", "text/plain"), Set.of("image/", "video/"));

	private final long maxSizeBytes;
	private final Set<String> allowedExactContentTypes;
	private final Set<String> allowedContentTypePrefixes;

	UploadPurpose(long maxSizeBytes, Set<String> allowedExactContentTypes, Set<String> allowedContentTypePrefixes) {
		this.maxSizeBytes = maxSizeBytes;
		this.allowedExactContentTypes = allowedExactContentTypes;
		this.allowedContentTypePrefixes = allowedContentTypePrefixes;
	}

	/**
	 * Returns the maximum allowed object size in bytes for this purpose.
	 *
	 * @return maximum size in bytes
	 */
	public long maxSizeBytes() {
		return maxSizeBytes;
	}

	/**
	 * Checks whether a declared content type is permitted for this purpose, matching exactly or
	 * by an allowed prefix (e.g. {@code image/}).
	 *
	 * @param contentType declared MIME type, possibly {@code null}
	 * @return {@code true} when the content type is allowed
	 */
	public boolean isContentTypeAllowed(String contentType) {
		if (contentType == null || contentType.isBlank()) {
			return false;
		}
		String normalized = contentType.trim().toLowerCase();
		if (allowedExactContentTypes.contains(normalized)) {
			return true;
		}
		return allowedContentTypePrefixes.stream().anyMatch(normalized::startsWith);
	}
}
