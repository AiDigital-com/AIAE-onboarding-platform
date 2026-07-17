package com.aidigital.aionboarding.external.link.model;

/**
 * Reason an outbound link fetch was blocked by the SSRF-resistant client's own policy,
 * as opposed to an ordinary network failure (unreachable host, non-2xx status, malformed
 * response). Values mirror {@code SsrfBlockReason} in the {@code service} module one-to-one
 * so the caller can translate without the two modules depending on each other.
 */
public enum LinkFetchFailureReason {

	DISALLOWED_SCHEME("disallowed_scheme"),
	USERINFO_PRESENT("userinfo_present"),
	DISALLOWED_PORT("disallowed_port"),
	PRIVATE_ADDRESS("private_address"),
	REDIRECT_LIMIT_EXCEEDED("redirect_limit_exceeded"),
	RESPONSE_TOO_LARGE("response_too_large"),
	UNSUPPORTED_CONTENT_TYPE("unsupported_content_type"),
	TIMEOUT("timeout");

	private final String value;

	LinkFetchFailureReason(String value) {
		this.value = value;
	}

	/**
	 * Returns the stable string value used for logging and metrics.
	 *
	 * @return the stable string value
	 */
	public String value() {
		return value;
	}
}
