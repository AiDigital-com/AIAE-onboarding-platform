package com.aidigital.aionboarding.external.storage.impl;

import com.aidigital.aionboarding.external.storage.StorageClient;
import com.aidigital.aionboarding.external.storage.StorageExternalException;
import com.aidigital.aionboarding.external.storage.models.ObjectMetadataRecord;

import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * No-op storage client used when object storage is disabled or not configured.
 */
public class StubStorageClient implements StorageClient {

    private static final String MESSAGE =
        "Object storage is not configured. Set app.external.storage.enabled=true "
            + "and provide bucket credentials, or use a local stub profile.";

    @Override
    public String presignPut(String storageKey, String contentType, Duration expiresIn) {
        throw new StorageExternalException(MESSAGE);
    }

    @Override
    public String presignGet(String storageKey, Duration expiresIn) {
        throw new StorageExternalException(MESSAGE);
    }

    @Override
    public void putObject(String storageKey, byte[] content, String contentType) {
        throw new StorageExternalException(MESSAGE);
    }

    @Override
    public void putObjectStreaming(String storageKey, InputStream content, long contentLength, String contentType) {
        throw new StorageExternalException(MESSAGE);
    }

    @Override
    public Optional<ObjectMetadataRecord> headObject(String storageKey) {
        throw new StorageExternalException(MESSAGE);
    }

    @Override
    public byte[] getObjectBuffer(String storageKey, long maxSizeBytes) {
        throw new StorageExternalException(MESSAGE);
    }

    @Override
    public void deleteObjects(List<String> storageKeys) {
        throw new StorageExternalException(MESSAGE);
    }
}
