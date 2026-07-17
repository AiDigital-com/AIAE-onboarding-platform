package com.aidigital.aionboarding.external.link.support;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Resolves a hostname to its candidate addresses. Extracted as an injectable seam so tests can
 * fake DNS answers (including deliberately private/loopback ones) without depending on real
 * network resolution.
 */
@FunctionalInterface
public interface HostnameResolver {

	/**
	 * Resolves every address a hostname currently answers to.
	 *
	 * @param host the hostname to resolve
	 * @return all resolved addresses
	 * @throws UnknownHostException if the hostname cannot be resolved
	 */
	InetAddress[] resolveAll(String host) throws UnknownHostException;
}
