package com.aidigital.aionboarding.mappers.youtube;

import com.aidigital.aionboarding.api.v1.model.YoutubeOembedResponseV1;
import com.aidigital.aionboarding.external.youtube.model.YoutubeOEmbedMetadata;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class YoutubeApiMapperImplTest {

	private final YoutubeApiMapperImpl youtubeApiMapperImpl = new YoutubeApiMapperImpl();

	@Test
	void shouldToYoutubeOembedResponseV1YoutubeOEmbedMetadataTest() {
		// Given:
		YoutubeOEmbedMetadata metadata = Instancio.create(YoutubeOEmbedMetadata.class);

		// When:
		YoutubeOembedResponseV1 actualResult = youtubeApiMapperImpl.toYoutubeOembedResponseV1(metadata);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToYoutubeOembedResponseV1YoutubeOEmbedMetadataWithNullTest() {
		// Given:
		YoutubeOEmbedMetadata metadata = null;

		// When:
		YoutubeOembedResponseV1 actualResult = youtubeApiMapperImpl.toYoutubeOembedResponseV1(metadata);

		// Then:
		assertThat(actualResult).isNull();
	}

}