package com.aidigital.aionboarding.external.storage;

import com.aidigital.aionboarding.external.storage.models.ObjectMetadataRecord;

import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Application-facing object storage adapter (S3-compatible).
 *
 * <p>Non-2xx responses, timeouts, and SDK failures are mapped to
 * {@link StorageExternalException}.
 */
public interface StorageClient {

    /**
     * Creates a presigned URL for uploading an object via HTTP PUT.
     *
     * @param storageKey object key within the configured bucket
     * @param contentType MIME type for the upload
     * @param expiresIn URL lifetime
     * @return presigned PUT URL
     */
    String presignPut(String storageKey, String contentType, Duration expiresIn);

    /**
     * Creates a presigned URL for downloading an object via HTTP GET.
     *
     * @param storageKey object key within the configured bucket
     * @param expiresIn URL lifetime
     * @return presigned GET URL
     */
    String presignGet(String storageKey, Duration expiresIn);

    /**
     * Uploads object bytes server-side.
     *
     * @param storageKey object key within the configured bucket
     * @param content object bytes
     * @param contentType MIME type (defaults to {@code application/octet-stream} when blank)
     */
    void putObject(String storageKey, byte[] content, String contentType);

    /**
     * Uploads an object by streaming its bytes, without buffering the full content in memory.
     *
     * @param storageKey object key within the configured bucket
     * @param content object content stream; the caller retains ownership and must close it
     * @param contentLength exact number of bytes {@code content} will yield
     * @param contentType MIME type (defaults to {@code application/octet-stream} when blank)
     */
    void putObjectStreaming(String storageKey, InputStream content, long contentLength, String contentType);

    /**
     * Looks up the real size and content type of an already-stored object, without downloading it.
     *
     * @param storageKey object key within the configured bucket
     * @return metadata when the object exists, otherwise {@link Optional#empty()}
     */
    Optional<ObjectMetadataRecord> headObject(String storageKey);

    /**
     * Downloads an object into memory, aborting before or during the read if it exceeds
     * {@code maxSizeBytes} instead of buffering an unbounded amount of untrusted content.
     *
     * @param storageKey   object key within the configured bucket
     * @param maxSizeBytes maximum number of bytes to buffer
     * @return object bytes
     * @throws StorageExternalException when the object's declared or actual size exceeds {@code maxSizeBytes}
     */
    byte[] getObjectBuffer(String storageKey, long maxSizeBytes);

    /**
     * Deletes one or more objects. Blank keys are ignored; duplicates are de-duplicated.
     *
     * @param storageKeys keys to delete
     */
    void deleteObjects(List<String> storageKeys);
}
