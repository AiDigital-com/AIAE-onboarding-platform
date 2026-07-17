package com.aidigital.aionboarding.external.heygen.impl;

import com.aidigital.aionboarding.external.heygen.HeyGenExternalException;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StubHeyGenClientTest {

	@Test
	void shouldThrowOnCreateTeacherVideoTest() {
		// Given:
		StubHeyGenClient client = new StubHeyGenClient();
		String prompt = Instancio.create(String.class);

		// When / Then:
		assertThatThrownBy(() -> client.createTeacherVideo(prompt))
				.isInstanceOf(HeyGenExternalException.class)
				.hasMessageContaining("HeyGen is not configured");
	}

	@Test
	void shouldThrowOnGetVideoStatusTest() {
		// Given:
		StubHeyGenClient client = new StubHeyGenClient();
		String videoId = Instancio.create(String.class);

		// When / Then:
		assertThatThrownBy(() -> client.getVideoStatus(videoId))
				.isInstanceOf(HeyGenExternalException.class)
				.hasMessageContaining("HeyGen is not configured");
	}
}
