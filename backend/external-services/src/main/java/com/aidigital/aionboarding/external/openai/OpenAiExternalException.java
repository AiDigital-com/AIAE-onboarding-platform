package com.aidigital.aionboarding.external.openai;

/**
 * Runtime exception signalling a failure when calling the OpenAI API.
 *
 * <p>Covers non-2xx HTTP responses, network timeouts, and malformed response JSON.
 * The API key is never included in the message or cause.
 */
public class OpenAiExternalException extends RuntimeException {

    private final int httpStatus;
    private final String responseBody;
    private final boolean timeout;

    /**
     * Constructs an exception from a non-2xx HTTP response.
     *
     * @param message     human-readable description (must not contain the API key)
     * @param httpStatus  HTTP response status code
     * @param responseBody raw response body (may be empty)
     */
    public OpenAiExternalException(String message, int httpStatus, String responseBody) {
        super(message);
        this.httpStatus = httpStatus;
        this.responseBody = responseBody;
        this.timeout = false;
    }

    /**
     * Constructs an exception from a network or parse failure.
     *
     * @param message human-readable description
     * @param cause   underlying exception
     */
    public OpenAiExternalException(String message, Throwable cause) {
        this(message, cause, false);
    }

    /**
     * Constructs an exception from a network or parse failure, recording whether it was a
     * timeout so callers can distinguish "provider too slow" from "provider rejected us".
     *
     * @param message human-readable description
     * @param cause   underlying exception
     * @param timeout {@code true} when the call exceeded the configured response timeout
     */
    public OpenAiExternalException(String message, Throwable cause, boolean timeout) {
        super(message, cause);
        this.httpStatus = -1;
        this.responseBody = "";
        this.timeout = timeout;
    }

    /**
     * Returns whether this failure was a connect/read timeout rather than a provider response.
     *
     * @return {@code true} when the call timed out
     */
    public boolean isTimeout() {
        return timeout;
    }

    /**
     * Returns the HTTP status code from the OpenAI API response, or {@code -1}
     * when the error occurred before a response was received.
     *
     * @return HTTP status code or {@code -1}
     */
    public int getHttpStatus() {
        return httpStatus;
    }

    /**
     * Returns the raw response body from the OpenAI API.
     * Empty when the error occurred before a response body was available.
     *
     * @return response body, never {@code null}
     */
    public String getResponseBody() {
        return responseBody;
    }
}