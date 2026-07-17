package com.aidigital.aionboarding.external.youtube.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class YoutubeTranscriptResultTest {

	@Test
	void shouldBeAvailableWhenErrorIsNullTest() {
		// Given:
		YoutubeTranscriptResult result = new YoutubeTranscriptResult("id", List.of(), null);

		// When:
		boolean available = result.isAvailable();

		// Then:
		assertThat(available).isTrue();
	}

	@Test
	void shouldBeAvailableWhenErrorIsBlankTest() {
		// Given:
		YoutubeTranscriptResult result = new YoutubeTranscriptResult("id", List.of(), "  ");

		// When:
		boolean available = result.isAvailable();

		// Then:
		assertThat(available).isTrue();
	}

	@Test
	void shouldNotBeAvailableWhenErrorIsPresentTest() {
		// Given:
		YoutubeTranscriptResult result = new YoutubeTranscriptResult("id", List.of(), "failed");

		// When:
		boolean available = result.isAvailable();

		// Then:
		assertThat(available).isFalse();
	}
}
