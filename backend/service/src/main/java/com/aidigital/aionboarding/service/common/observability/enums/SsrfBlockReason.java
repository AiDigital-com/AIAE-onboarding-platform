package com.aidigital.aionboarding.service.common.observability.enums;

/**
 * Low-cardinality reason an outbound link fetch was refused before or during the request, used
 * only as a {@link SecurityMetrics} tag — never logged alongside the destination URL itself.
 */
public enum SsrfBlockReason {
	DISALLOWED_SCHEME("disallowed_scheme"),
	USERINFO_PRESENT("userinfo_present"),
	DISALLOWED_PORT("disallowed_port"),
	PRIVATE_ADDRESS("private_address"),
	REDIRECT_LIMIT_EXCEEDED("redirect_limit_exceeded"),
	RESPONSE_TOO_LARGE("response_too_large"),
	UNSUPPORTED_CONTENT_TYPE("unsupported_content_type"),
	TIMEOUT("timeout");

	private final String value;

	SsrfBlockReason(String value) {
		this.value = value;
	}

	/**
	 * Returns the metric tag value for this reason.
	 *
	 * @return reason value
	 */
	public String value() {
		return value;
	}
}
