package com.aidigital.aionboarding.external.link.support;

import com.aidigital.aionboarding.external.link.model.LinkFetchFailureReason;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PinnedDnsResolverTest {

	@Test
	void resolveShouldReturnTheResolvedAddressesWhenAllArePublicTest() throws Exception {
		// Given: a fake resolver standing in for real DNS, returning only public addresses.
		InetAddress[] publicAddresses = {InetAddress.getByName("8.8.8.8")};
		PinnedDnsResolver resolver = new PinnedDnsResolver(
				host -> publicAddresses, new OutboundAddressValidator());

		// When:
		InetAddress[] result = resolver.resolve("example.com");

		// Then:
		assertThat(result).isEqualTo(publicAddresses);
	}

	@Test
	void resolveShouldThrowLinkFetchBlockedExceptionWhenTheResolvedAddressIsPrivateTest() throws Exception {
		// Given: a fake resolver simulating DNS answering with a loopback address (SSRF payload).
		PinnedDnsResolver resolver = new PinnedDnsResolver(
				host -> new InetAddress[]{InetAddress.getByName("127.0.0.1")}, new OutboundAddressValidator());

		// When-Then:
		assertThatThrownBy(() -> resolver.resolve("attacker-controlled.example.com"))
				.isInstanceOf(LinkFetchBlockedException.class)
				.satisfies(ex -> assertThat(((LinkFetchBlockedException) ex).reason())
						.isEqualTo(LinkFetchFailureReason.PRIVATE_ADDRESS));
	}

	@Test
	void resolveShouldThrowWhenAnyOneOfMultipleResolvedAddressesIsPrivateTest() throws Exception {
		// Given: DNS rebinding style answer mixing a public and a private address.
		PinnedDnsResolver resolver = new PinnedDnsResolver(
				host -> new InetAddress[]{InetAddress.getByName("8.8.8.8"), InetAddress.getByName("10.0.0.5")},
				new OutboundAddressValidator());

		// When-Then:
		assertThatThrownBy(() -> resolver.resolve("mixed.example.com"))
				.isInstanceOf(LinkFetchBlockedException.class);
	}
}
