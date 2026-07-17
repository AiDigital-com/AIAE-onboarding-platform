package com.aidigital.aionboarding.external.link.support;

import com.aidigital.aionboarding.external.link.model.LinkFetchFailureReason;

/**
 * Signals that a link fetch was refused by the outbound-security policy (disallowed scheme,
 * userinfo, port, non-public resolved address, or exceeded redirect cap), as opposed to an
 * ordinary network failure.
 */
public class LinkFetchBlockedException extends RuntimeException {

	private final LinkFetchFailureReason reason;

	public LinkFetchBlockedException(LinkFetchFailureReason reason, String message) {
		super(message);
		this.reason = reason;
	}

	/**
	 * Returns the specific policy reason this fetch was blocked.
	 *
	 * @return the block reason
	 */
	public LinkFetchFailureReason reason() {
		return reason;
	}
}
