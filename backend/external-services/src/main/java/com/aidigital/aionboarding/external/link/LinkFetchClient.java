package com.aidigital.aionboarding.external.link;

import com.aidigital.aionboarding.external.link.model.LinkFetchResult;

/**
 * Application-facing adapter for fetching a user-submitted URL's raw response body, resistant
 * to server-side request forgery: only public HTTP/HTTPS addresses are ever connected to, and
 * every redirect hop is re-validated the same way as the initial request.
 */
public interface LinkFetchClient {

    /**
     * Fetches the given URL's response body, enforcing the outbound-security policy on the
     * initial URL and on every redirect hop.
     *
     * @param url the caller-submitted URL to fetch
     * @return the fetch outcome; never throws for ordinary or policy-blocked failures
     */
    LinkFetchResult fetch(String url);
}
