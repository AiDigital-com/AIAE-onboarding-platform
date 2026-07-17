package com.aidigital.aionboarding.external.link.support;

import java.net.Inet6Address;
import java.net.InetAddress;

/**
 * Classifies whether a resolved {@link InetAddress} is a public, internet-routable address that
 * an outbound link fetch may connect to. Pure and side-effect free so it can be exhaustively
 * unit-tested against literal addresses without any real DNS resolution or socket I/O.
 */
public class OutboundAddressValidator {

	/**
	 * Returns whether every address in {@code addresses} is public. Rejects the whole set if
	 * any single address is not public, rather than filtering and continuing with the rest —
	 * an attacker-controlled DNS answer could otherwise mix a public "trust anchor" address with
	 * a private target and rely on the client trying the next address on failure.
	 *
	 * @param addresses resolved addresses for one hostname
	 * @return {@code true} only if every address is public
	 */
	public boolean allPublic(InetAddress[] addresses) {
		if (addresses == null || addresses.length == 0) {
			return false;
		}
		for (InetAddress address : addresses) {
			if (!isPublic(address)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns whether a single resolved address is public and internet-routable.
	 *
	 * @param address the resolved address to classify
	 * @return {@code true} if the address is public
	 */
	public boolean isPublic(InetAddress address) {
		if (address.isAnyLocalAddress()
				|| address.isLoopbackAddress()
				|| address.isLinkLocalAddress()
				|| address.isSiteLocalAddress()
				|| address.isMulticastAddress()) {
			return false;
		}
		return !isIpv6UniqueLocalAddress(address);
	}

	/**
	 * Detects IPv6 Unique Local Addresses ({@code fc00::/7}, RFC 4193) — a private-network-only
	 * range that {@link InetAddress#isSiteLocalAddress()} does not cover, since that method only
	 * recognizes the older, deprecated {@code fec0::/10} site-local prefix.
	 *
	 * @param address the resolved address to check
	 * @return {@code true} if this is an IPv6 ULA
	 */
	boolean isIpv6UniqueLocalAddress(InetAddress address) {
		if (!(address instanceof Inet6Address)) {
			return false;
		}
		byte[] bytes = address.getAddress();
		return (bytes[0] & 0xFE) == 0xFC;
	}
}
