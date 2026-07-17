package com.aidigital.aionboarding.external.heygen.impl;

import com.aidigital.aionboarding.external.common.http.PooledRestClientFactory;
import com.aidigital.aionboarding.external.common.http.config.PooledHttpClientProperties;
import com.aidigital.aionboarding.external.heygen.HeyGenExternalException;
import com.aidigital.aionboarding.external.heygen.config.HeyGenProperties;
import com.aidigital.aionboarding.external.heygen.model.HeyGenTeacherVideoResult;
import com.aidigital.aionboarding.external.heygen.model.HeyGenVideoStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zalando.logbook.Logbook;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HeyGenClientImplTest {

	private static String baseUrl(MockWebServer server) {
		return server.url("/").toString().replaceAll("/$", "");
	}

	@Nested
	class CreateTeacherVideo {

		@Test
		void shouldReturnVideoResultOnSuccessTest() throws IOException {
			// Given:
			MockWebServer server = new MockWebServer();
			server.start();
			PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
			httpProps.setConnectTimeout(Duration.ofSeconds(5));
			httpProps.setResponseTimeout(Duration.ofSeconds(10));
			httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
			httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
			httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
			PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
					new SimpleMeterRegistry());
			HeyGenProperties properties = new HeyGenProperties();
			properties.setBaseUrl(baseUrl(server));
			properties.setApiKey("test-api-key");
			properties.setAvatarId("avatar-1");
			properties.setVoiceId("voice-1");
			HeyGenClientImpl client = new HeyGenClientImpl(properties, factory);
			server.enqueue(new MockResponse()
					.setBody("""
							{"data":{"session_id":"sess-1","video_id":"vid-1","status":"generating"}}
							""")
					.setHeader("Content-Type", "application/json"));

			try {
				// When:
				HeyGenTeacherVideoResult result = client.createTeacherVideo("Make a video about Java");

				// Then:
				assertThat(result.provider()).isEqualTo("heygen");
				assertThat(result.prompt()).isEqualTo("Make a video about Java");
				assertThat(result.sessionId()).isEqualTo("sess-1");
				assertThat(result.videoId()).isEqualTo("vid-1");
				assertThat(result.status()).isEqualTo("generating");
			} finally {
				factory.destroy();
				server.shutdown();
			}
		}

		@Test
		void shouldThrowForNullPromptTest() {
			// Given:
			PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
			httpProps.setConnectTimeout(Duration.ofSeconds(5));
			httpProps.setResponseTimeout(Duration.ofSeconds(10));
			httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
			httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
			httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
			PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
					new SimpleMeterRegistry());
			HeyGenProperties properties = new HeyGenProperties();
			properties.setBaseUrl("http://localhost:12345");
			properties.setApiKey("test-api-key");
			properties.setAvatarId("avatar-1");
			properties.setVoiceId("voice-1");
			HeyGenClientImpl client = new HeyGenClientImpl(properties, factory);

			try {
				// When / Then:
				assertThatThrownBy(() -> client.createTeacherVideo(null))
						.isInstanceOf(HeyGenExternalException.class)
						.hasMessageContaining("HeyGen prompt is required");
			} finally {
				factory.destroy();
			}
		}

		@Test
		void shouldThrowForBlankPromptTest() {
			// Given:
			PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
			httpProps.setConnectTimeout(Duration.ofSeconds(5));
			httpProps.setResponseTimeout(Duration.ofSeconds(10));
			httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
			httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
			httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
			PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
					new SimpleMeterRegistry());
			HeyGenProperties properties = new HeyGenProperties();
			properties.setBaseUrl("http://localhost:12345");
			properties.setApiKey("test-api-key");
			properties.setAvatarId("avatar-1");
			properties.setVoiceId("voice-1");
			HeyGenClientImpl client = new HeyGenClientImpl(properties, factory);

			try {
				// When / Then:
				assertThatThrownBy(() -> client.createTeacherVideo("  "))
						.isInstanceOf(HeyGenExternalException.class)
						.hasMessageContaining("HeyGen prompt is required");
			} finally {
				factory.destroy();
			}
		}
	}

	@Nested
	class GetVideoStatus {

		@Test
		void shouldReturnVideoStatusOnSuccessTest() throws IOException {
			// Given:
			MockWebServer server = new MockWebServer();
			server.start();
			PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
			httpProps.setConnectTimeout(Duration.ofSeconds(5));
			httpProps.setResponseTimeout(Duration.ofSeconds(10));
			httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
			httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
			httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
			PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
					new SimpleMeterRegistry());
			HeyGenProperties properties = new HeyGenProperties();
			properties.setBaseUrl(baseUrl(server));
			properties.setApiKey("test-api-key");
			properties.setAvatarId("avatar-1");
			properties.setVoiceId("voice-1");
			HeyGenClientImpl client = new HeyGenClientImpl(properties, factory);
			server.enqueue(new MockResponse()
					.setBody("""
							{"data":{"id":"vid-1","status":"completed","video_url":"https://cdn.heygen.com/v.mp4",
							"thumbnail_url":"https://cdn.heygen.com/t.jpg","duration":15.5}}
							""")
					.setHeader("Content-Type", "application/json"));

			try {
				// When:
				HeyGenVideoStatus result = client.getVideoStatus("vid-1");

				// Then:
				assertThat(result.id()).isEqualTo("vid-1");
				assertThat(result.status()).isEqualTo("completed");
				assertThat(result.videoUrl()).isEqualTo("https://cdn.heygen.com/v.mp4");
				assertThat(result.duration()).isEqualTo(15.5);
			} finally {
				factory.destroy();
				server.shutdown();
			}
		}

		@Test
		void shouldThrowForNullVideoIdTest() {
			// Given:
			PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
			httpProps.setConnectTimeout(Duration.ofSeconds(5));
			httpProps.setResponseTimeout(Duration.ofSeconds(10));
			httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
			httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
			httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
			PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
					new SimpleMeterRegistry());
			HeyGenProperties properties = new HeyGenProperties();
			properties.setBaseUrl("http://localhost:12345");
			properties.setApiKey("test-api-key");
			properties.setAvatarId("avatar-1");
			properties.setVoiceId("voice-1");
			HeyGenClientImpl client = new HeyGenClientImpl(properties, factory);

			try {
				// When / Then:
				assertThatThrownBy(() -> client.getVideoStatus(null))
						.isInstanceOf(HeyGenExternalException.class)
						.hasMessageContaining("HeyGen video id is required");
			} finally {
				factory.destroy();
			}
		}

		@Test
		void shouldThrowForBlankVideoIdTest() {
			// Given:
			PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
			httpProps.setConnectTimeout(Duration.ofSeconds(5));
			httpProps.setResponseTimeout(Duration.ofSeconds(10));
			httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
			httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
			httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
			PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
					new SimpleMeterRegistry());
			HeyGenProperties properties = new HeyGenProperties();
			properties.setBaseUrl("http://localhost:12345");
			properties.setApiKey("test-api-key");
			properties.setAvatarId("avatar-1");
			properties.setVoiceId("voice-1");
			HeyGenClientImpl client = new HeyGenClientImpl(properties, factory);

			try {
				// When / Then:
				assertThatThrownBy(() -> client.getVideoStatus("  "))
						.isInstanceOf(HeyGenExternalException.class)
						.hasMessageContaining("HeyGen video id is required");
			} finally {
				factory.destroy();
			}
		}

		@Test
		void shouldHandleResponseWithDefaultStatusTest() throws IOException {
			// Given:
			MockWebServer server = new MockWebServer();
			server.start();
			PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
			httpProps.setConnectTimeout(Duration.ofSeconds(5));
			httpProps.setResponseTimeout(Duration.ofSeconds(10));
			httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
			httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
			httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
			PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
					new SimpleMeterRegistry());
			HeyGenProperties properties = new HeyGenProperties();
			properties.setBaseUrl(baseUrl(server));
			properties.setApiKey("test-api-key");
			properties.setAvatarId("avatar-1");
			properties.setVoiceId("voice-1");
			HeyGenClientImpl client = new HeyGenClientImpl(properties, factory);
			server.enqueue(new MockResponse()
					.setBody("{}")
					.setHeader("Content-Type", "application/json"));

			try {
				// When:
				HeyGenVideoStatus result = client.getVideoStatus("vid-1");

				// Then:
				assertThat(result.id()).isEqualTo("vid-1");
				assertThat(result.status()).isEqualTo("unknown");
			} finally {
				factory.destroy();
				server.shutdown();
			}
		}

		@Test
		void shouldHandleEmptyDataResponseTest() throws IOException {
			// Given:
			MockWebServer server = new MockWebServer();
			server.start();
			PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
			httpProps.setConnectTimeout(Duration.ofSeconds(5));
			httpProps.setResponseTimeout(Duration.ofSeconds(10));
			httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
			httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
			httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
			PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
					new SimpleMeterRegistry());
			HeyGenProperties properties = new HeyGenProperties();
			properties.setBaseUrl(baseUrl(server));
			properties.setApiKey("test-api-key");
			properties.setAvatarId("avatar-1");
			properties.setVoiceId("voice-1");
			HeyGenClientImpl client = new HeyGenClientImpl(properties, factory);
			server.enqueue(new MockResponse()
					.setBody("{\"data\":{}}")
					.setHeader("Content-Type", "application/json"));

			try {
				// When:
				HeyGenVideoStatus result = client.getVideoStatus("vid-1");

				// Then:
				assertThat(result.id()).isEqualTo("vid-1");
			} finally {
				factory.destroy();
				server.shutdown();
			}
		}
	}

	@Nested
	class CreateTeacherVideoWithOptions {

		@Test
		void shouldHandleResponseWithoutSessionIdTest() throws IOException {
			// Given:
			MockWebServer server = new MockWebServer();
			server.start();
			PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
			httpProps.setConnectTimeout(Duration.ofSeconds(5));
			httpProps.setResponseTimeout(Duration.ofSeconds(10));
			httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
			httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
			httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
			PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
					new SimpleMeterRegistry());
			HeyGenProperties properties = new HeyGenProperties();
			properties.setBaseUrl(baseUrl(server));
			properties.setApiKey("test-api-key");
			properties.setAvatarId("");
			properties.setVoiceId("");
			HeyGenClientImpl client = new HeyGenClientImpl(properties, factory);
			server.enqueue(new MockResponse()
					.setBody("{\"data\":{\"video_id\":\"vid-1\",\"status\":\"generating\"}}")
					.setHeader("Content-Type", "application/json"));

			try {
				// When:
				HeyGenTeacherVideoResult result = client.createTeacherVideo("test prompt");

				// Then:
				assertThat(result.videoId()).isEqualTo("vid-1");
				assertThat(result.sessionId()).isEmpty();
			} finally {
				factory.destroy();
				server.shutdown();
			}
		}

		@Test
		void shouldHandleErrorResponseTest() throws IOException {
			// Given:
			MockWebServer server = new MockWebServer();
			server.start();
			PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
			httpProps.setConnectTimeout(Duration.ofSeconds(5));
			httpProps.setResponseTimeout(Duration.ofSeconds(10));
			httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
			httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
			httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
			PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
					new SimpleMeterRegistry());
			HeyGenProperties properties = new HeyGenProperties();
			properties.setBaseUrl(baseUrl(server));
			properties.setApiKey("test-api-key");
			properties.setAvatarId("avatar-1");
			properties.setVoiceId("voice-1");
			HeyGenClientImpl client = new HeyGenClientImpl(properties, factory);
			server.enqueue(new MockResponse()
					.setResponseCode(500)
					.setBody("{\"error\":{\"message\":\"Server error\"}}")
					.setHeader("Content-Type", "application/json"));

			try {
				// When / Then:
				assertThatThrownBy(() -> client.createTeacherVideo("test prompt"))
						.isInstanceOf(HeyGenExternalException.class);
			} finally {
				factory.destroy();
				server.shutdown();
			}
		}

		@Test
		void shouldHandleMessageFieldInErrorResponseTest() throws IOException {
			// Given:
			MockWebServer server = new MockWebServer();
			server.start();
			PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
			httpProps.setConnectTimeout(Duration.ofSeconds(5));
			httpProps.setResponseTimeout(Duration.ofSeconds(10));
			httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
			httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
			httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
			PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
					new SimpleMeterRegistry());
			HeyGenProperties properties = new HeyGenProperties();
			properties.setBaseUrl(baseUrl(server));
			properties.setApiKey("test-api-key");
			properties.setAvatarId("avatar-1");
			properties.setVoiceId("voice-1");
			HeyGenClientImpl client = new HeyGenClientImpl(properties, factory);
			server.enqueue(new MockResponse()
					.setResponseCode(400)
					.setBody("{\"message\":\"Invalid request\"}")
					.setHeader("Content-Type", "application/json"));

			try {
				// When / Then:
				assertThatThrownBy(() -> client.createTeacherVideo("test prompt"))
						.isInstanceOf(HeyGenExternalException.class);
			} finally {
				factory.destroy();
				server.shutdown();
			}
		}
	}

	@Nested
	class GetVideoStatusErrors {

		@Test
		void shouldThrowOnHttpErrorTest() throws IOException {
			// Given:
			MockWebServer server = new MockWebServer();
			server.start();
			PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
			httpProps.setConnectTimeout(Duration.ofSeconds(5));
			httpProps.setResponseTimeout(Duration.ofSeconds(10));
			httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
			httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
			httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
			PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
					new SimpleMeterRegistry());
			HeyGenProperties properties = new HeyGenProperties();
			properties.setBaseUrl(baseUrl(server));
			properties.setApiKey("test-api-key");
			properties.setAvatarId("avatar-1");
			properties.setVoiceId("voice-1");
			HeyGenClientImpl client = new HeyGenClientImpl(properties, factory);
			server.enqueue(new MockResponse()
					.setResponseCode(404)
					.setBody("{\"error\":{\"message\":\"Video not found\"}}")
					.setHeader("Content-Type", "application/json"));

			try {
				// When / Then:
				assertThatThrownBy(() -> client.getVideoStatus("vid-1"))
						.isInstanceOf(HeyGenExternalException.class);
			} finally {
				factory.destroy();
				server.shutdown();
			}
		}
	}
}
