package com.aidigital.aionboarding.service.storage;

import com.aidigital.aionboarding.domain.storage.entities.PendingUpload;
import com.aidigital.aionboarding.external.storage.StorageClient;
import com.aidigital.aionboarding.external.storage.config.StorageProperties;
import com.aidigital.aionboarding.external.storage.models.ObjectMetadataRecord;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.observability.SecurityMetrics;
import com.aidigital.aionboarding.service.common.observability.enums.UploadRejectionReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.storage.enums.UploadPurpose;
import com.aidigital.aionboarding.service.storage.models.FilePreviewRecord;
import com.aidigital.aionboarding.service.storage.services.entity.PendingUploadEntityService;
import com.aidigital.aionboarding.service.user.services.entity.UserEntityService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StorageService {

	private static final Logger LOG = LoggerFactory.getLogger(StorageService.class);
	private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
	private static final String INVALID_UPLOAD_REFERENCE = "Invalid or expired upload reference.";

	/**
	 * Bounds each abandoned-upload sweep so a large backlog cannot turn one job run into an
	 * unbounded batch of S3 deletes and DB writes.
	 */
	private static final int CLEANUP_BATCH_LIMIT = 100;

	/**
	 * Bounds a batch preview call so a caller bypassing the OpenAPI request-size validation
	 * cannot force an unbounded number of signing operations in one request.
	 */
	private static final int MAX_BATCH_PREVIEW_KEYS = 100;

	private final StorageClient storageClient;
	private final PendingUploadEntityService pendingUploadEntityService;
	private final UserEntityService userEntityService;
	private final StorageProperties properties;
	private final CurrentTime currentTime;
	private final SecurityMetrics securityMetrics;

	public record PresignedUpload(String uploadUrl, String storageKey) {

	}

	/**
	 * Issues a presigned PUT URL for a new object and records a {@link PendingUpload} row so a
	 * later {@link #confirmUpload} call can verify the registering caller is the same user the
	 * URL was issued to, and that the object was actually uploaded before any entity persists
	 * this storage key.
	 *
	 * @param owner       authenticated user the upload will belong to
	 * @param purpose     upload category, fixing the allowed size and content-type policy
	 * @param fileName    original file name, used only to build a readable key suffix
	 * @param contentType declared MIME type; blank defaults to {@code application/octet-stream}
	 * @param sizeBytes   declared size in bytes; must be positive and within the purpose's cap
	 * @return presigned upload URL and the storage key the caller must PUT to
	 * @throws AppException C002 when size or content type violate the purpose's policy
	 */
	@Transactional
	public PresignedUpload presignPut(AppUser owner, UploadPurpose purpose, String fileName, String contentType,
									  long sizeBytes) {
		String resolvedContentType = resolveContentType(contentType);
		validateUploadPolicy(purpose, resolvedContentType, sizeBytes);

		String storageKey = "uploads/" + UUID.randomUUID() + "/" + sanitize(fileName);
		Duration expiry = Duration.ofSeconds(properties.getPresignPutExpiresSeconds());
		String url = storageClient.presignPut(storageKey, resolvedContentType, expiry);

		LocalDateTime now = currentTime.utcDateTime();
		PendingUpload pendingUpload = new PendingUpload();
		pendingUpload.setStorageKey(storageKey);
		pendingUpload.setOwnerUser(userEntityService.getReference(owner.internalId()));
		pendingUpload.setExpectedContentType(resolvedContentType);
		pendingUpload.setExpectedSizeBytes(sizeBytes);
		pendingUpload.setConfirmed(false);
		pendingUpload.setCreatedAt(now);
		pendingUpload.setExpiresAt(now.plusSeconds(properties.getPresignPutExpiresSeconds()));
		pendingUploadEntityService.save(pendingUpload);

		return new PresignedUpload(url, storageKey);
	}

	/**
	 * Validates and consumes a pending upload before its storage key may be attached to a
	 * lesson/material entity: the key must have been issued to {@code owner}, must not have
	 * already been confirmed or expired, and the object must actually exist in storage with the
	 * declared size.
	 *
	 * @param owner      authenticated user attempting to register the upload
	 * @param storageKey storage key returned by an earlier {@link #presignPut} call
	 * @throws AppException C002 when the reference is missing, not owned by {@code owner},
	 *                      already confirmed, expired, or the object never finished uploading
	 */
	@Transactional
	public void confirmUpload(AppUser owner, String storageKey) {
		if (storageKey == null || storageKey.isBlank()) {
			throw new AppException(ErrorReason.C002, "storageKey is required.");
		}

		PendingUpload pendingUpload = pendingUploadEntityService.findByStorageKey(storageKey)
				.orElseThrow(() -> rejectUpload(UploadRejectionReason.NOT_FOUND));

		boolean ownedByCaller = pendingUpload.getOwnerUser() != null
				&& Objects.equals(pendingUpload.getOwnerUser().getId(), owner.internalId());
		boolean expired = pendingUpload.getExpiresAt().isBefore(currentTime.utcDateTime());

		if (!ownedByCaller) {
			throw rejectUpload(UploadRejectionReason.OWNERSHIP_MISMATCH);
		}
		if (pendingUpload.isConfirmed()) {
			throw rejectUpload(UploadRejectionReason.ALREADY_CONFIRMED);
		}
		if (expired) {
			throw rejectUpload(UploadRejectionReason.EXPIRED);
		}

		ObjectMetadataRecord metadata = storageClient.headObject(storageKey)
				.orElseThrow(() -> new AppException(ErrorReason.C002, "Upload did not complete."));

		if (metadata.sizeBytes() != pendingUpload.getExpectedSizeBytes()) {
			// Left unconfirmed rather than deleted here: deleting now would mean an S3 network
			// call inside this transaction. The abandoned-upload sweep reclaims it once expired.
			throw rejectUpload(UploadRejectionReason.SIZE_MISMATCH);
		}

		// Atomic conditional update rather than read-then-save: two requests racing to confirm
		// the same key could both pass the isConfirmed() check above before either commits, and
		// both would otherwise attach the same object to two different entities.
		if (!pendingUploadEntityService.markConfirmedIfUnconfirmed(storageKey)) {
			throw rejectUpload(UploadRejectionReason.ALREADY_CONFIRMED);
		}
	}

	/**
	 * Verifies that an already-confirmed storage key belongs to {@code owner}, without consuming
	 * it, for callers that re-reference a previously uploaded object on a mutable field (e.g. a
	 * profile avatar) rather than newly attaching it to an entity for the first time.
	 *
	 * @param owner      authenticated user attempting to reference the key
	 * @param storageKey storage key to verify
	 * @throws AppException C002 when the reference is missing or not owned by {@code owner}
	 */
	@Transactional(readOnly = true)
	public void requireOwnership(AppUser owner, String storageKey) {
		if (storageKey == null || storageKey.isBlank()) {
			throw new AppException(ErrorReason.C002, "storageKey is required.");
		}

		PendingUpload pendingUpload = pendingUploadEntityService.findByStorageKey(storageKey)
				.orElseThrow(() -> rejectUpload(UploadRejectionReason.NOT_FOUND));

		boolean ownedByCaller = pendingUpload.getOwnerUser() != null
				&& Objects.equals(pendingUpload.getOwnerUser().getId(), owner.internalId());
		if (!ownedByCaller) {
			throw rejectUpload(UploadRejectionReason.OWNERSHIP_MISMATCH);
		}
	}

	/**
	 * Deletes storage objects and tracking rows for pending uploads that expired without ever
	 * being confirmed, bounded to {@link #CLEANUP_BATCH_LIMIT} per call.
	 *
	 * @return number of abandoned uploads cleaned up in this call
	 */
	@Transactional
	public int cleanupAbandonedUploads() {
		List<PendingUpload> expired = pendingUploadEntityService.findExpiredUnconfirmed(
				currentTime.utcDateTime(), PageRequest.of(0, CLEANUP_BATCH_LIMIT));
		if (expired.isEmpty()) {
			return 0;
		}

		List<String> keys = expired.stream().map(PendingUpload::getStorageKey).toList();
		pendingUploadEntityService.deleteAll(expired);

		// Deferred to after commit so the S3 network round trip never holds this transaction's
		// connection open, matching MaterialPersistenceService's storage-cleanup pattern.
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				try {
					storageClient.deleteObjects(keys);
				} catch (RuntimeException e) {
					LOG.warn("Abandoned upload storage cleanup failed (non-fatal): {}", e.getMessage());
				}
			}
		});
		return expired.size();
	}

	/**
	 * Issues a presigned GET URL for downloading an existing object.
	 *
	 * @param storageKey object storage key
	 * @return signed download URL, valid for the configured preview lifetime
	 */
	public String presignGet(String storageKey) {
		Duration expiry = Duration.ofSeconds(properties.getPresignGetExpiresSeconds());
		return storageClient.presignGet(storageKey, expiry);
	}

	/**
	 * Issues presigned GET URLs for multiple existing objects in one call, so a caller resolving
	 * many storage keys at once (e.g. a grid of cards) does not need one round trip per key.
	 *
	 * @param storageKeys object storage keys; blanks are dropped and duplicates removed, and the
	 *                    result is capped to {@value #MAX_BATCH_PREVIEW_KEYS} entries even if the caller bypassed
	 *                    the OpenAPI request-size validation
	 * @return one {@link FilePreviewRecord} per resolved key, in the order first seen
	 */
	public List<FilePreviewRecord> presignGet(List<String> storageKeys) {
		if (storageKeys == null) {
			return List.of();
		}

		return storageKeys.stream()
				.filter(key -> key != null && !key.isBlank())
				.distinct()
				.limit(MAX_BATCH_PREVIEW_KEYS)
				.map(key -> new FilePreviewRecord(key, presignGet(key)))
				.toList();
	}

	/**
	 * Uploads file bytes already buffered in memory and records an already-confirmed
	 * {@link PendingUpload} row so the resulting key has the same traceable owner as a
	 * presigned-and-confirmed upload. Prefer {@link #putObjectStreaming} for request-body uploads
	 * so the servlet's multipart bytes are not duplicated into a second in-memory buffer.
	 *
	 * @param owner       authenticated user the upload will belong to
	 * @param purpose     upload category, fixing the allowed size and content-type policy
	 * @param bytes       object content
	 * @param fileName    original file name, used only to build a readable key suffix
	 * @param contentType declared MIME type
	 * @return generated storage key
	 * @throws AppException C002 when size or content type violate the purpose's policy
	 */
	@Transactional
	public String putObject(AppUser owner, UploadPurpose purpose, byte[] bytes, String fileName, String contentType) {
		String resolvedContentType = resolveContentType(contentType);
		validateUploadPolicy(purpose, resolvedContentType, bytes.length);
		String storageKey = "uploads/" + UUID.randomUUID() + "/" + sanitize(fileName);
		storageClient.putObject(storageKey, bytes, resolvedContentType);
		registerConfirmedUpload(owner, storageKey, resolvedContentType, bytes.length);
		return storageKey;
	}

	/**
	 * Uploads a file by streaming its bytes, without buffering the full content in memory, and
	 * records an already-confirmed {@link PendingUpload} row so the resulting key has the same
	 * traceable owner as a presigned-and-confirmed upload.
	 *
	 * @param owner         authenticated user the upload will belong to
	 * @param purpose       upload category, fixing the allowed size and content-type policy
	 * @param content       object content stream; the caller retains ownership and must close it
	 * @param contentLength exact number of bytes {@code content} will yield
	 * @param fileName      original file name, used only to build a readable key suffix
	 * @param contentType   declared MIME type
	 * @return generated storage key
	 * @throws AppException C002 when size or content type violate the purpose's policy
	 */
	@Transactional
	public String putObjectStreaming(
			AppUser owner, UploadPurpose purpose, InputStream content, long contentLength, String fileName,
			String contentType
	) {
		String resolvedContentType = resolveContentType(contentType);
		validateUploadPolicy(purpose, resolvedContentType, contentLength);
		String storageKey = "uploads/" + UUID.randomUUID() + "/" + sanitize(fileName);
		storageClient.putObjectStreaming(storageKey, content, contentLength, resolvedContentType);
		registerConfirmedUpload(owner, storageKey, resolvedContentType, contentLength);
		return storageKey;
	}

	/**
	 * Downloads an object into memory, aborting before or during the read if it exceeds
	 * {@code maxSizeBytes} instead of buffering an unbounded amount of untrusted content.
	 *
	 * @param storageKey   object storage key
	 * @param maxSizeBytes maximum number of bytes to buffer
	 * @return object bytes
	 */
	public byte[] getObjectBuffer(String storageKey, long maxSizeBytes) {
		return storageClient.getObjectBuffer(storageKey, maxSizeBytes);
	}

	/**
	 * Deletes one or more objects.
	 *
	 * @param storageKeys keys to delete
	 */
	public void deleteObjects(List<String> storageKeys) {
		storageClient.deleteObjects(storageKeys);
	}

	/**
	 * Rejects an upload/reference attempt for the given reason, recording the security counter
	 * and returning the exception to throw.
	 *
	 * @param reason fixed, low-cardinality rejection reason
	 * @return the exception to throw for this rejection
	 */
	AppException rejectUpload(UploadRejectionReason reason) {
		securityMetrics.uploadRejected(reason);
		return new AppException(ErrorReason.C002, INVALID_UPLOAD_REFERENCE);
	}

	/**
	 * Validates a declared size and content type against a purpose's policy, recording a
	 * security counter and throwing when either is violated. The effective size cap is the
	 * lesser of the purpose's own cap and the configured global maximum, so a misconfigured
	 * purpose can never exceed the operator-controlled ceiling.
	 *
	 * @param purpose     upload category, fixing the allowed size and content-type policy
	 * @param contentType resolved (non-blank) declared MIME type
	 * @param sizeBytes   declared size in bytes
	 * @throws AppException C002 when size is not positive, exceeds the effective cap, or the
	 *                      content type is not in the purpose's allowlist
	 */
	void validateUploadPolicy(UploadPurpose purpose, String contentType, long sizeBytes) {
		if (sizeBytes <= 0) {
			throw new AppException(ErrorReason.C002, "size must be greater than 0.");
		}
		long effectiveMaxSizeBytes = Math.min(purpose.maxSizeBytes(), properties.getMaxUploadSizeBytes());
		if (sizeBytes > effectiveMaxSizeBytes) {
			securityMetrics.uploadRejected(UploadRejectionReason.SIZE_EXCEEDED);
			throw new AppException(ErrorReason.C002, "size must not exceed " + effectiveMaxSizeBytes + " bytes for " +
					"this upload type.");
		}
		if (!purpose.isContentTypeAllowed(contentType)) {
			securityMetrics.uploadRejected(UploadRejectionReason.MIME_NOT_ALLOWED);
			throw new AppException(ErrorReason.C002, "Content type '" + contentType + "' is not allowed for this " +
					"upload type.");
		}
	}

	/**
	 * Persists an already-confirmed {@link PendingUpload} row for an object the backend itself
	 * just wrote (direct multipart upload), so it carries the same traceable owner record a
	 * presigned-and-confirmed upload would have.
	 *
	 * @param owner       authenticated user the upload belongs to
	 * @param storageKey  storage key the object was written to
	 * @param contentType resolved declared MIME type
	 * @param sizeBytes   actual object size in bytes
	 */
	void registerConfirmedUpload(AppUser owner, String storageKey, String contentType, long sizeBytes) {
		LocalDateTime now = currentTime.utcDateTime();
		PendingUpload pendingUpload = new PendingUpload();
		pendingUpload.setStorageKey(storageKey);
		pendingUpload.setOwnerUser(userEntityService.getReference(owner.internalId()));
		pendingUpload.setExpectedContentType(contentType);
		pendingUpload.setExpectedSizeBytes(sizeBytes);
		pendingUpload.setConfirmed(true);
		pendingUpload.setCreatedAt(now);
		pendingUpload.setExpiresAt(now.plusSeconds(properties.getPresignPutExpiresSeconds()));
		pendingUploadEntityService.save(pendingUpload);
	}

	/**
	 * Normalizes a declared content type, defaulting blank values.
	 *
	 * @param contentType declared MIME type, possibly blank
	 * @return {@code contentType} unchanged, or {@value #DEFAULT_CONTENT_TYPE} when blank
	 */
	String resolveContentType(String contentType) {
		return contentType == null || contentType.isBlank() ? DEFAULT_CONTENT_TYPE : contentType;
	}

	/**
	 * Strips characters unsafe for a storage key from a file name.
	 *
	 * @param fileName original file name, possibly null
	 * @return sanitized file name suffix
	 */
	String sanitize(String fileName) {
		return fileName == null ? "file" : fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
	}
}
