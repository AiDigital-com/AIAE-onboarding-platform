package com.aidigital.aionboarding.external.storage.models;

/**
 * Real (server-observed) size and content type of an object already stored in the bucket.
 *
 * @param sizeBytes   actual object size as reported by storage
 * @param contentType actual content type as reported by storage
 */
public record ObjectMetadataRecord(long sizeBytes, String contentType) {

}
