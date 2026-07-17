package com.aidigital.aionboarding.external.link.model;

/**
 * Outcome of an SSRF-resistant link fetch. Exactly one of {@code body} or {@code errorMessage}
 * is meaningful depending on {@code success}; {@code securityBlockReason} is non-null only when
 * the fetch was refused by the client's own outbound-security policy rather than failing for an
 * ordinary network reason (unreachable host, non-2xx status, timeout not attributable to policy).
 *
 * @param success            whether a response body was successfully retrieved
 * @param body               the response body, bounded in size; blank when {@code success} is false
 * @param contentType        the response {@code Content-Type}, blank when {@code success} is false
 * @param errorMessage       a human-readable failure reason; blank when {@code success} is true
 * @param securityBlockReason the specific policy reason this fetch was blocked, or {@code null}
 *                            when the failure (or success) was not a security-policy block
 */
public record LinkFetchResult(
    boolean success,
    String body,
    String contentType,
    String errorMessage,
    LinkFetchFailureReason securityBlockReason
) {

    /**
     * Builds a successful result.
     *
     * @param body        the fetched response body
     * @param contentType the response {@code Content-Type}
     * @return a successful result
     */
    public static LinkFetchResult success(String body, String contentType) {
        return new LinkFetchResult(true, body, contentType, "", null);
    }

    /**
     * Builds a result for a fetch refused by outbound-security policy.
     *
     * @param reason  the specific policy reason
     * @param message a human-readable failure reason
     * @return a security-blocked result
     */
    public static LinkFetchResult securityBlocked(LinkFetchFailureReason reason, String message) {
        return new LinkFetchResult(false, "", "", message, reason);
    }

    /**
     * Builds a result for an ordinary, non-security-related fetch failure.
     *
     * @param message a human-readable failure reason
     * @return a failed result with no security-block reason
     */
    public static LinkFetchResult failure(String message) {
        return new LinkFetchResult(false, "", "", message, null);
    }
}
