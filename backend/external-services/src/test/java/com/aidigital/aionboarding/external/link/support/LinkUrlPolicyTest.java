package com.aidigital.aionboarding.external.link.support;

import com.aidigital.aionboarding.external.link.model.LinkFetchFailureReason;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class LinkUrlPolicyTest {

	private final LinkUrlPolicy policy = new LinkUrlPolicy();

	@Test
	void validateShouldAllowAnOrdinaryHttpsUrlWithNoExplicitPortTest() {
		// Given-When-Then:
		assertThat(policy.validate(URI.create("https://example.com/docs"))).isNull();
	}

	@Test
	void validateShouldAllowAnExplicitDefaultPortForItsSchemeTest() {
		// Given-When-Then:
		assertThat(policy.validate(URI.create("http://example.com:80/docs"))).isNull();
		assertThat(policy.validate(URI.create("https://example.com:443/docs"))).isNull();
	}

	@Test
	void validateShouldRejectAnUnapprovedExplicitPortTest() {
		// Given-When-Then:
		assertThat(policy.validate(URI.create("http://example.com:8080/docs")))
				.isEqualTo(LinkFetchFailureReason.DISALLOWED_PORT);
		assertThat(policy.validate(URI.create("https://example.com:8443/docs")))
				.isEqualTo(LinkFetchFailureReason.DISALLOWED_PORT);
	}

	@Test
	void validateShouldRejectNonHttpSchemesTest() {
		// Given-When-Then:
		assertThat(policy.validate(URI.create("ftp://example.com/file")))
				.isEqualTo(LinkFetchFailureReason.DISALLOWED_SCHEME);
		assertThat(policy.validate(URI.create("file:///etc/passwd")))
				.isEqualTo(LinkFetchFailureReason.DISALLOWED_SCHEME);
		assertThat(policy.validate(URI.create("javascript:alert(1)")))
				.isEqualTo(LinkFetchFailureReason.DISALLOWED_SCHEME);
	}

	@Test
	void validateShouldRejectUserinfoInTheUrlTest() {
		// Given-When-Then: userinfo could smuggle credentials or be used to confuse a naive parser.
		assertThat(policy.validate(URI.create("http://user:pass@example.com/docs")))
				.isEqualTo(LinkFetchFailureReason.USERINFO_PRESENT);
	}

	@Test
	void validateShouldRejectAUrlWithNoHostTest() throws Exception {
		// Given-When-Then:
		assertThat(policy.validate(new URI("http", null, "", -1, "/path", null, null)))
				.isEqualTo(LinkFetchFailureReason.DISALLOWED_SCHEME);
	}

}
