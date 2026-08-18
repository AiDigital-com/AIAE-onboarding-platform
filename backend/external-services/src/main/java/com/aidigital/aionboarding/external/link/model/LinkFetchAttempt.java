package com.aidigital.aionboarding.external.link.model;

import java.net.URI;

/**
 * Outcome of a single HTTP round trip within a link fetch's redirect loop: either a terminal
 * {@link LinkFetchResult}, or a redirect target to re-validate and follow next.
 *
 * @param result     the terminal result, or {@code null} when {@code redirectTo} is set
 * @param redirectTo the next hop to follow, or {@code null} when {@code result} is terminal
 */
public record LinkFetchAttempt(LinkFetchResult result, URI redirectTo) {

	/**
	 * Builds a terminal attempt outcome.
	 *
	 * @param result the terminal result
	 */
	public LinkFetchAttempt(LinkFetchResult result) {
		this(result, null);
	}

	/**
	 * Builds a redirect attempt outcome.
	 *
	 * @param redirectTo the next hop to follow
	 */
	public LinkFetchAttempt(URI redirectTo) {
		this(null, redirectTo);
	}
}
