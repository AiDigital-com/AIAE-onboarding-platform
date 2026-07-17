package com.aidigital.aionboarding.external.youtube;

/**
 * Runtime exception signalling a failure when calling YouTube services.
 */
public class YoutubeExternalException extends RuntimeException {

    public YoutubeExternalException(String message) {
        super(message);
    }

    public YoutubeExternalException(String message, Throwable cause) {
        super(message, cause);
    }
}
