package com.aidigital.aionboarding.external.heygen;

/**
 * Runtime exception signalling a failure when calling the HeyGen API.
 */
public class HeyGenExternalException extends RuntimeException {

    private final int httpStatus;
    private final String responseBody;

    public HeyGenExternalException(String message) {
        super(message);
        this.httpStatus = -1;
        this.responseBody = "";
    }

    public HeyGenExternalException(String message, int httpStatus, String responseBody) {
        super(message);
        this.httpStatus = httpStatus;
        this.responseBody = responseBody;
    }

    public HeyGenExternalException(String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = -1;
        this.responseBody = "";
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
