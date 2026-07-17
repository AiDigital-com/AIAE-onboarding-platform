package com.aidigital.aionboarding.external.storage;

/**
 * Runtime exception signalling a failure when calling object storage.
 */
public class StorageExternalException extends RuntimeException {

    /**
     * Constructs an exception with a human-readable message.
     *
     * @param message description of the failure
     */
    public StorageExternalException(String message) {
        super(message);
    }

    /**
     * Constructs an exception wrapping an underlying cause.
     *
     * @param message description of the failure
     * @param cause underlying exception
     */
    public StorageExternalException(String message, Throwable cause) {
        super(message, cause);
    }
}
