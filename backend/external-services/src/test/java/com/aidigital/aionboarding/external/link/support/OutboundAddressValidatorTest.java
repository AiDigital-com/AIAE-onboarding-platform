package com.aidigital.aionboarding.external.link.support;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.assertj.core.api.Assertions.assertThat;

class OutboundAddressValidatorTest {

	private final OutboundAddressValidator validator = new OutboundAddressValidator();

	@Test
	void isPublicShouldRejectIpv4LoopbackTest() throws UnknownHostException {
		// Given-When-Then:
		assertThat(validator.isPublic(InetAddress.getByName("127.0.0.1"))).isFalse();
	}

	@Test
	void isPublicShouldRejectIpv6LoopbackTest() throws UnknownHostException {
		// Given-When-Then:
		assertThat(validator.isPublic(InetAddress.getByName("::1"))).isFalse();
	}

	@Test
	void isPublicShouldRejectIpv4MappedIpv6LoopbackTest() throws UnknownHostException {
		// Given: a well-known SSRF bypass attempt — an IPv4-mapped IPv6 literal for loopback.
		assertThat(validator.isPublic(InetAddress.getByName("::ffff:127.0.0.1"))).isFalse();
	}

	@Test
	void isPublicShouldRejectIpv4LinkLocalIncludingCloudMetadataAddressTest() throws UnknownHostException {
		// Given-When-Then: 169.254.169.254 is the well-known AWS/GCP/Azure metadata address.
		assertThat(validator.isPublic(InetAddress.getByName("169.254.169.254"))).isFalse();
		assertThat(validator.isPublic(InetAddress.getByName("169.254.1.1"))).isFalse();
	}

	@Test
	void isPublicShouldRejectIpv6LinkLocalTest() throws UnknownHostException {
		// Given-When-Then:
		assertThat(validator.isPublic(InetAddress.getByName("fe80::1"))).isFalse();
	}

	@Test
	void isPublicShouldRejectRfc1918PrivateRangesTest() throws UnknownHostException {
		// Given-When-Then:
		assertThat(validator.isPublic(InetAddress.getByName("10.0.0.1"))).isFalse();
		assertThat(validator.isPublic(InetAddress.getByName("172.16.0.1"))).isFalse();
		assertThat(validator.isPublic(InetAddress.getByName("192.168.1.1"))).isFalse();
	}

	@Test
	void isPublicShouldRejectMulticastTest() throws UnknownHostException {
		// Given-When-Then:
		assertThat(validator.isPublic(InetAddress.getByName("224.0.0.1"))).isFalse();
	}

	@Test
	void isPublicShouldRejectAnyLocalAddressTest() throws UnknownHostException {
		// Given-When-Then:
		assertThat(validator.isPublic(InetAddress.getByName("0.0.0.0"))).isFalse();
	}

	@Test
	void isPublicShouldRejectIpv6UniqueLocalAddressesTest() throws UnknownHostException {
		// Given: fc00::/7 (RFC 4193) is private-network-only but InetAddress.isSiteLocalAddress()
		// only recognizes the older, deprecated fec0::/10 prefix — this is the gap this method fixes.
		assertThat(validator.isPublic(InetAddress.getByName("fc00::1"))).isFalse();
		assertThat(validator.isPublic(InetAddress.getByName("fd12:3456:789a:1::1"))).isFalse();
	}

	@Test
	void isPublicShouldAcceptOrdinaryPublicIpv4AndIpv6AddressesTest() throws UnknownHostException {
		// Given-When-Then:
		assertThat(validator.isPublic(InetAddress.getByName("8.8.8.8"))).isTrue();
		assertThat(validator.isPublic(InetAddress.getByName("1.1.1.1"))).isTrue();
		assertThat(validator.isPublic(InetAddress.getByName("2606:4700:4700::1111"))).isTrue();
	}

	@Test
	void allPublicShouldReturnFalseWhenAnySingleAddressIsPrivateTest() throws UnknownHostException {
		// Given: a mix of one public and one private address — the whole set must be rejected,
		// since a client could otherwise fail over to the private address on connect.
		InetAddress[] mixed = {InetAddress.getByName("8.8.8.8"), InetAddress.getByName("10.0.0.5")};

		// When-Then:
		assertThat(validator.allPublic(mixed)).isFalse();
	}

	@Test
	void allPublicShouldReturnTrueOnlyWhenEveryAddressIsPublicTest() throws UnknownHostException {
		// Given:
		InetAddress[] allPublic = {InetAddress.getByName("8.8.8.8"), InetAddress.getByName("1.1.1.1")};

		// When-Then:
		assertThat(validator.allPublic(allPublic)).isTrue();
	}

	@Test
	void allPublicShouldReturnFalseForNullOrEmptyAddressesTest() {
		// Given-When-Then:
		assertThat(validator.allPublic(null)).isFalse();
		assertThat(validator.allPublic(new InetAddress[0])).isFalse();
	}
}
