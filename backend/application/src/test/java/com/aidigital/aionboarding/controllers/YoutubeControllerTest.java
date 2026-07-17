package com.aidigital.aionboarding.controllers;

import com.aidigital.aionboarding.api.v1.model.YoutubeOembedResponseV1;
import com.aidigital.aionboarding.external.youtube.YoutubeClient;
import com.aidigital.aionboarding.external.youtube.model.YoutubeOEmbedMetadata;
import com.aidigital.aionboarding.mappers.youtube.YoutubeApiMapper;
import com.aidigital.aionboarding.service.common.security.AppUser;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class YoutubeControllerTest {

	@Mock
	private CurrentUserSupport currentUser;
	@Mock
	private YoutubeClient youtubeClient;
	@Mock
	private YoutubeApiMapper youtubeApiMapper;

	@InjectMocks
	private YoutubeController controller;

	@Test
	void shouldReturnOembedResponseTest() {
		// Given:
		AppUser viewer = new AppUser(1L, "clerk-1", "user@test.com", "User", "member", "User", null, null, null);
		YoutubeOEmbedMetadata metadata = Instancio.create(YoutubeOEmbedMetadata.class);
		YoutubeOembedResponseV1 expectedBody = Instancio.create(YoutubeOembedResponseV1.class);
		when(currentUser.requireUser()).thenReturn(viewer);
		when(youtubeClient.fetchOembed("https://youtube.com/watch?v=1")).thenReturn(metadata);
		when(youtubeApiMapper.toYoutubeOembedResponseV1(metadata)).thenReturn(expectedBody);

		// When:
		ResponseEntity<YoutubeOembedResponseV1> response = controller.getYoutubeOembed("https://youtube.com/watch?v=1");

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
	}
}
