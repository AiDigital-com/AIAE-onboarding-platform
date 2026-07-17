package com.aidigital.aionboarding.external.youtube.impl;

import com.aidigital.aionboarding.external.youtube.model.YoutubeOEmbedMetadata;
import com.aidigital.aionboarding.external.youtube.model.YoutubeTranscriptResult;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StubYoutubeClientTest {

	@Test
	void shouldReturnErrorMetadataOnFetchOembedTest() {
		// Given:
		StubYoutubeClient client = new StubYoutubeClient();
		String url = Instancio.create(String.class);

		// When:
		YoutubeOEmbedMetadata result = client.fetchOembed(url);

		// Then:
		assertThat(result.title()).isEmpty();
		assertThat(result.authorName()).isEmpty();
		assertThat(result.authorUrl()).isEmpty();
		assertThat(result.thumbnailUrl()).isEmpty();
		assertThat(result.thumbnailWidth()).isNull();
		assertThat(result.thumbnailHeight()).isNull();
		assertThat(result.providerName()).isEmpty();
		assertThat(result.error()).isEqualTo(
				"YouTube integration is disabled. Set app.external.youtube.enabled=true.");
	}

	@Test
	void shouldReturnErrorResultOnFetchTranscriptTest() {
		// Given:
		StubYoutubeClient client = new StubYoutubeClient();
		String videoId = Instancio.create(String.class);

		// When:
		YoutubeTranscriptResult result = client.fetchTranscript(videoId);

		// Then:
		assertThat(result.videoId()).isEqualTo(videoId);
		assertThat(result.segments()).isEmpty();
		assertThat(result.error()).isEqualTo(
				"YouTube integration is disabled. Set app.external.youtube.enabled=true.");
	}
}
