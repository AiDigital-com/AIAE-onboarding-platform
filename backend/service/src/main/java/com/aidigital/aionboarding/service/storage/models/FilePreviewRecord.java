package com.aidigital.aionboarding.service.storage.models;

/**
 * A resolved signed preview URL for one storage key, returned by a batch
 * {@link com.aidigital.aionboarding.service.storage.StorageService#presignGet(java.util.List)} call.
 *
 * @param storageKey original storage key
 * @param previewUrl signed preview URL for that key
 */
public record FilePreviewRecord(String storageKey, String previewUrl) { }
