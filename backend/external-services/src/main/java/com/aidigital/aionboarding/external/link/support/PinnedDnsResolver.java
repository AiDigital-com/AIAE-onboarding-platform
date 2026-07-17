package com.aidigital.aionboarding.external.link.support;

import com.aidigital.aionboarding.external.link.model.LinkFetchFailureReason;
import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.SystemDefaultDnsResolver;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Resolves and validates a hostname's addresses in a single atomic call, so the exact addresses
 * Apache HttpClient5 connects to are the same ones just validated — closing the gap a DNS
 * rebinding attack would otherwise exploit between a separate validate-then-resolve step.
 * Invoked by HttpClient5 itself at actual connection time for every request, including each
 * manually re-issued redirect hop, so every hop is re-validated with no extra plumbing.
 */
public class PinnedDnsResolver implements DnsResolver {

    private final HostnameResolver hostnameResolver;
    private final OutboundAddressValidator addressValidator;

    /**
     * Constructs a resolver backed by the real JVM DNS resolver and the real address policy.
     */
    public PinnedDnsResolver() {
        this(new SystemDefaultDnsResolver()::resolve, new OutboundAddressValidator());
    }

    /**
     * Constructs a resolver with injectable collaborators, for tests that fake DNS answers or
     * relax the address policy to exercise a local test server.
     *
     * @param hostnameResolver resolves a hostname to candidate addresses
     * @param addressValidator classifies whether resolved addresses are public
     */
    public PinnedDnsResolver(HostnameResolver hostnameResolver, OutboundAddressValidator addressValidator) {
        this.hostnameResolver = hostnameResolver;
        this.addressValidator = addressValidator;
    }

    @Override
    public InetAddress[] resolve(String host) throws UnknownHostException {
        InetAddress[] addresses = hostnameResolver.resolveAll(host);
        if (!addressValidator.allPublic(addresses)) {
            throw new LinkFetchBlockedException(
                LinkFetchFailureReason.PRIVATE_ADDRESS, "Resolved address is not publicly routable: " + host);
        }
        return addresses;
    }

    @Override
    public String resolveCanonicalHostname(String host) throws UnknownHostException {
        return new SystemDefaultDnsResolver().resolveCanonicalHostname(host);
    }
}
