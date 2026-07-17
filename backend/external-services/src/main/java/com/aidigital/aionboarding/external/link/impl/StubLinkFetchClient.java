package com.aidigital.aionboarding.external.link.impl;

import com.aidigital.aionboarding.external.link.LinkFetchClient;
import com.aidigital.aionboarding.external.link.model.LinkFetchResult;

/**
 * Stub link-fetch client used when the integration is explicitly disabled.
 */
public class StubLinkFetchClient implements LinkFetchClient {

    private static final String MESSAGE =
        "Link fetching is disabled. Set app.external.link-fetch.enabled=true.";

    @Override
    public LinkFetchResult fetch(String url) {
        return LinkFetchResult.failure(MESSAGE);
    }
}
