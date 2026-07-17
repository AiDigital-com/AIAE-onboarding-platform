package com.aidigital.aionboarding.external.link.support;

import com.aidigital.aionboarding.external.link.model.LinkFetchFailureReason;

import java.net.URI;

/**
 * Validates the static, address-independent shape of a candidate link-fetch URL: scheme,
 * userinfo, and port. Applied to both the initial URL and every redirect target before that
 * target is ever resolved or connected to.
 */
public class LinkUrlPolicy {

    /**
     * Validates a candidate URL, returning the specific block reason on failure.
     *
     * @param uri the parsed candidate URI
     * @return the reason this URI is rejected, or {@code null} if it passes
     */
    public LinkFetchFailureReason validate(URI uri) {
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            return LinkFetchFailureReason.DISALLOWED_SCHEME;
        }
        if (uri.getRawUserInfo() != null) {
            return LinkFetchFailureReason.USERINFO_PRESENT;
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            return LinkFetchFailureReason.DISALLOWED_SCHEME;
        }
        int defaultPort = "https".equalsIgnoreCase(scheme) ? 443 : 80;
        int effectivePort = uri.getPort() == -1 ? defaultPort : uri.getPort();
        if (effectivePort != defaultPort) {
            return LinkFetchFailureReason.DISALLOWED_PORT;
        }
        return null;
    }
}
