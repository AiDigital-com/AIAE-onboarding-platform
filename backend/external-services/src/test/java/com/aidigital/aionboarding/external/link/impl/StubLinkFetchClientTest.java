package com.aidigital.aionboarding.external.link.impl;

import com.aidigital.aionboarding.external.link.model.LinkFetchResult;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StubLinkFetchClientTest {

	@Test
	void shouldReturnFailureResultTest() {
		// Given:
		StubLinkFetchClient client = new StubLinkFetchClient();
		String url = Instancio.create(String.class);

		// When:
		LinkFetchResult result = client.fetch(url);

		// Then:
		assertThat(result.success()).isFalse();
		assertThat(result.body()).isEmpty();
		assertThat(result.contentType()).isEmpty();
		assertThat(result.errorMessage()).isEqualTo(
				"Link fetching is disabled. Set app.external.link-fetch.enabled=true.");
		assertThat(result.securityBlockReason()).isNull();
	}
}
