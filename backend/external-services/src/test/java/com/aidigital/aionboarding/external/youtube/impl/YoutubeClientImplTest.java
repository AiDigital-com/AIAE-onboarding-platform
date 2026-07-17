package com.aidigital.aionboarding.external.youtube.impl;

import com.aidigital.aionboarding.external.common.http.PooledRestClientFactory;
import com.aidigital.aionboarding.external.common.http.config.PooledHttpClientProperties;
import com.aidigital.aionboarding.external.youtube.YoutubeExternalException;
import com.aidigital.aionboarding.external.youtube.config.YoutubeProperties;
import com.aidigital.aionboarding.external.youtube.model.YoutubeOEmbedMetadata;
import com.aidigital.aionboarding.external.youtube.model.YoutubeTranscriptResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.zalando.logbook.Logbook;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings({"rawtypes", "unchecked"})
class YoutubeClientImplTest {

	private static String baseUrl(MockWebServer server) {
		return server.url("/").toString().replaceAll("/$", "");
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static RestClient mockOembedClient(JsonNode body) {
		RestClient oembedClient = mock(RestClient.class);
		RestClient.RequestHeadersUriSpec getSpec = mock(RestClient.RequestHeadersUriSpec.class);
		RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
		RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
		when(oembedClient.get()).thenReturn(getSpec);
		when(getSpec.uri(any(URI.class))).thenReturn(headersSpec);
		when(headersSpec.header(eq("Accept"), eq("application/json"))).thenReturn(headersSpec);
		when(headersSpec.header(eq("User-Agent"), anyString())).thenReturn(headersSpec);
		when(headersSpec.retrieve()).thenReturn(responseSpec);
		when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
		when(responseSpec.body(JsonNode.class)).thenReturn(body);
		return oembedClient;
	}

	private static PooledRestClientFactory mockYoutubeFactory(RestClient oembedClient) {
		PooledRestClientFactory factory = mock(PooledRestClientFactory.class);
		when(factory.createClient(eq("youtube-oembed"), eq("https://www.youtube.com"))).thenReturn(oembedClient);
		when(factory.createClient(eq("youtube"), eq("https://www.youtube.com"))).thenReturn(mock(RestClient.class));
		return factory;
	}

	@Nested
	class FetchOembed {

		@Test
		void shouldReturnErrorForInvalidUrlTest() {
			// Given:
			PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
			httpProps.setConnectTimeout(Duration.ofSeconds(5));
			httpProps.setResponseTimeout(Duration.ofSeconds(10));
			httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
			httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
			httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
			PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
					new SimpleMeterRegistry());
			YoutubeProperties properties = new YoutubeProperties();
			properties.setEnabled(true);
			properties.setTranscriptLang("en");
			properties.setTranscriptDebug(true);
			YoutubeClientImpl client = new YoutubeClientImpl(properties, factory);

			try {
				// When:
				YoutubeOEmbedMetadata result = client.fetchOembed("not-a-valid-url");

				// Then:
				assertThat(result.title()).isEmpty();
				assertThat(result.error()).isNotEmpty();
			} finally {
				factory.destroy();
			}
		}

		@Test
		void shouldReturnErrorForNullUrlTest() {
			// Given:
			PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
			httpProps.setConnectTimeout(Duration.ofSeconds(5));
			httpProps.setResponseTimeout(Duration.ofSeconds(10));
			httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
			httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
			httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
			PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
					new SimpleMeterRegistry());
			YoutubeProperties properties = new YoutubeProperties();
			properties.setEnabled(true);
			properties.setTranscriptLang("en");
			properties.setTranscriptDebug(true);
			YoutubeClientImpl client = new YoutubeClientImpl(properties, factory);

			try {
				// When:
				YoutubeOEmbedMetadata result = client.fetchOembed(null);

				// Then:
				assertThat(result.error()).isEqualTo("A valid YouTube URL is required.");
			} finally {
				factory.destroy();
			}
		}

		@Test
		void shouldReturnErrorForBlankUrlTest() {
			// Given:
			PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
			httpProps.setConnectTimeout(Duration.ofSeconds(5));
			httpProps.setResponseTimeout(Duration.ofSeconds(10));
			httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
			httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
			httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
			PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
					new SimpleMeterRegistry());
			YoutubeProperties properties = new YoutubeProperties();
			properties.setEnabled(true);
			properties.setTranscriptLang("en");
			properties.setTranscriptDebug(true);
			YoutubeClientImpl client = new YoutubeClientImpl(properties, factory);

			try {
				// When:
				YoutubeOEmbedMetadata result = client.fetchOembed("  ");

				// Then:
				assertThat(result.error()).isEqualTo("A valid YouTube URL is required.");
			} finally {
				factory.destroy();
			}
		}

		@Test
		void shouldRejectNonYoutubeUrlTest() {
			// Given:
			PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
			httpProps.setConnectTimeout(Duration.ofSeconds(5));
			httpProps.setResponseTimeout(Duration.ofSeconds(10));
			httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
			httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
			httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
			PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
					new SimpleMeterRegistry());
			YoutubeProperties properties = new YoutubeProperties();
			properties.setEnabled(true);
			properties.setTranscriptLang("en");
			properties.setTranscriptDebug(true);
			YoutubeClientImpl client = new YoutubeClientImpl(properties, factory);

			try {
				// When:
				YoutubeOEmbedMetadata result = client.fetchOembed("https://vimeo.com/123");

				// Then:
				assertThat(result.error()).isEqualTo("A valid YouTube URL is required.");
			} finally {
				factory.destroy();
			}
		}

		@Test
		void shouldRejectUrlWithInvalidHostTest() {
			// Given:
			PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
			httpProps.setConnectTimeout(Duration.ofSeconds(5));
			httpProps.setResponseTimeout(Duration.ofSeconds(10));
			httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
			httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
			httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
			PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
					new SimpleMeterRegistry());
			YoutubeProperties properties = new YoutubeProperties();
			properties.setEnabled(true);
			properties.setTranscriptLang("en");
			properties.setTranscriptDebug(true);
			YoutubeClientImpl client = new YoutubeClientImpl(properties, factory);

			try {
				// When:
				YoutubeOEmbedMetadata result = client.fetchOembed("http://.com");

				// Then:
				assertThat(result.error()).isEqualTo("A valid YouTube URL is required.");
			} finally {
				factory.destroy();
			}
		}
	}

	@Nested
	class FetchTranscript {

		@Test
		void shouldReturnErrorForNullVideoIdTest() {
			// Given:
			PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
			httpProps.setConnectTimeout(Duration.ofSeconds(5));
			httpProps.setResponseTimeout(Duration.ofSeconds(10));
			httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
			httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
			httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
			PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
					new SimpleMeterRegistry());
			YoutubeProperties properties = new YoutubeProperties();
			properties.setEnabled(true);
			properties.setTranscriptLang("en");
			properties.setTranscriptDebug(true);
			YoutubeClientImpl client = new YoutubeClientImpl(properties, factory);

			try {
				// When:
				YoutubeTranscriptResult result = client.fetchTranscript(null);

				// Then:
				assertThat(result.error()).isNotEmpty();
			} finally {
				factory.destroy();
			}
		}

		@Test
		void shouldReturnErrorForBlankVideoIdTest() {
			// Given:
			PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
			httpProps.setConnectTimeout(Duration.ofSeconds(5));
			httpProps.setResponseTimeout(Duration.ofSeconds(10));
			httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
			httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
			httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
			PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
					new SimpleMeterRegistry());
			YoutubeProperties properties = new YoutubeProperties();
			properties.setEnabled(true);
			properties.setTranscriptLang("en");
			properties.setTranscriptDebug(true);
			YoutubeClientImpl client = new YoutubeClientImpl(properties, factory);

			try {
				// When:
				YoutubeTranscriptResult result = client.fetchTranscript("  ");

				// Then:
				assertThat(result.error()).isNotEmpty();
			} finally {
				factory.destroy();
			}
		}

		@Test
		void shouldReturnErrorWhenNoCaptionTracksFoundTest() throws IOException {
			// Given:
			MockWebServer server = new MockWebServer();
			server.start();
			PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
			httpProps.setConnectTimeout(Duration.ofSeconds(5));
			httpProps.setResponseTimeout(Duration.ofSeconds(10));
			httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
			httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
			httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
			PooledRestClientFactory realFactory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
					new SimpleMeterRegistry());
			RestClient restClient = realFactory.createClient("youtube-test", baseUrl(server));
			PooledRestClientFactory factory = mock(PooledRestClientFactory.class);
			when(factory.createClient(anyString(), eq("https://www.youtube.com"))).thenReturn(restClient);
			YoutubeProperties properties = new YoutubeProperties();
			properties.setEnabled(true);
			properties.setTranscriptLang("en");
			properties.setTranscriptDebug(true);
			YoutubeClientImpl client = new YoutubeClientImpl(properties, factory);
			server.enqueue(new MockResponse()
					.setBody("<html><body>No captions</body></html>"));

			try {
				// When:
				YoutubeTranscriptResult result = client.fetchTranscript("test123");

				// Then:
				assertThat(result.error()).contains("No caption tracks found");
			} finally {
				realFactory.destroy();
				server.shutdown();
			}
		}

		@Test
		void shouldReturnErrorWhenWatchHtmlIsNullTest() throws IOException {
			// Given:
			MockWebServer server = new MockWebServer();
			server.start();
			PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
			httpProps.setConnectTimeout(Duration.ofSeconds(5));
			httpProps.setResponseTimeout(Duration.ofSeconds(10));
			httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
			httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
			httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
			PooledRestClientFactory realFactory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
					new SimpleMeterRegistry());
			RestClient restClient = realFactory.createClient("youtube-test", baseUrl(server));
			PooledRestClientFactory factory = mock(PooledRestClientFactory.class);
			when(factory.createClient(anyString(), eq("https://www.youtube.com"))).thenReturn(restClient);
			YoutubeProperties properties = new YoutubeProperties();
			properties.setEnabled(true);
			properties.setTranscriptLang("en");
			properties.setTranscriptDebug(true);
			YoutubeClientImpl client = new YoutubeClientImpl(properties, factory);
			server.enqueue(new MockResponse().setBody(""));

			try {
				// When:
				YoutubeTranscriptResult result = client.fetchTranscript("test123");

				// Then:
				assertThat(result.error()).isNotEmpty();
			} finally {
				realFactory.destroy();
				server.shutdown();
			}
		}

		@Test
		void shouldHandleHttpErrorOnTranscriptTest() throws IOException {
			// Given:
			MockWebServer server = new MockWebServer();
			server.start();
			PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
			httpProps.setConnectTimeout(Duration.ofSeconds(5));
			httpProps.setResponseTimeout(Duration.ofSeconds(10));
			httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
			httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
			httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
			PooledRestClientFactory realFactory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
					new SimpleMeterRegistry());
			RestClient restClient = realFactory.createClient("youtube-test", baseUrl(server));
			PooledRestClientFactory factory = mock(PooledRestClientFactory.class);
			when(factory.createClient(anyString(), eq("https://www.youtube.com"))).thenReturn(restClient);
			YoutubeProperties properties = new YoutubeProperties();
			properties.setEnabled(true);
			properties.setTranscriptLang("en");
			properties.setTranscriptDebug(true);
			YoutubeClientImpl client = new YoutubeClientImpl(properties, factory);
			server.enqueue(new MockResponse().setResponseCode(500).setBody("Server Error"));

			try {
				// When:
				YoutubeTranscriptResult result = client.fetchTranscript("test123");

				// Then:
				assertThat(result.error()).isNotEmpty();
			} finally {
				realFactory.destroy();
				server.shutdown();
			}
		}
	}

	@Nested
	class TranscriptResultFormat {

		@Test
		void shouldIncludeVideoIdInTranscriptResultTest() throws IOException {
			// Given:
			MockWebServer server = new MockWebServer();
			server.start();
			PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
			httpProps.setConnectTimeout(Duration.ofSeconds(5));
			httpProps.setResponseTimeout(Duration.ofSeconds(10));
			httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
			httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
			httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
			PooledRestClientFactory realFactory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
					new SimpleMeterRegistry());
			RestClient restClient = realFactory.createClient("youtube-test", baseUrl(server));
			PooledRestClientFactory factory = mock(PooledRestClientFactory.class);
			when(factory.createClient(anyString(), eq("https://www.youtube.com"))).thenReturn(restClient);
			YoutubeProperties properties = new YoutubeProperties();
			properties.setEnabled(true);
			properties.setTranscriptLang("en");
			properties.setTranscriptDebug(true);
			YoutubeClientImpl client = new YoutubeClientImpl(properties, factory);
			server.enqueue(new MockResponse()
					.setBody("<html><body>No captions</body></html>"));

			try {
				// When:
				YoutubeTranscriptResult result = client.fetchTranscript("abc123");

				// Then:
				assertThat(result.videoId()).isEqualTo("abc123");
			} finally {
				realFactory.destroy();
				server.shutdown();
			}
		}

		@Test
		void shouldReturnEmptySegmentsOnTranscriptErrorTest() {
			// Given:
			PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
			httpProps.setConnectTimeout(Duration.ofSeconds(5));
			httpProps.setResponseTimeout(Duration.ofSeconds(10));
			httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
			httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
			httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
			PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
					new SimpleMeterRegistry());
			YoutubeProperties properties = new YoutubeProperties();
			properties.setEnabled(true);
			properties.setTranscriptLang("en");
			properties.setTranscriptDebug(true);
			YoutubeClientImpl client = new YoutubeClientImpl(properties, factory);

			try {
				// When:
				YoutubeTranscriptResult result = client.fetchTranscript("");

				// Then:
				assertThat(result.segments()).isEmpty();
				assertThat(result.error()).isNotEmpty();
			} finally {
				factory.destroy();
			}
		}
	}

	@Nested
	class FetchOembedSuccess {

		@Test
		void shouldFetchOembedMetadataTest() throws IOException {
			// Given:
			JsonNode data = new ObjectMapper().readTree("""
					{"title":"Video Title","author_name":"Author Name",
					"author_url":"https://author.example","thumbnail_url":"https://thumb.example",
					"thumbnail_width":1280,"thumbnail_height":720,"provider_name":"YouTube"}
					""");
			PooledRestClientFactory factory = mockYoutubeFactory(mockOembedClient(data));
			YoutubeProperties properties = new YoutubeProperties();
			properties.setEnabled(true);
			properties.setTranscriptLang("en");
			properties.setTranscriptDebug(true);
			YoutubeClientImpl client = new YoutubeClientImpl(properties, factory);

			// When:
			YoutubeOEmbedMetadata result = client.fetchOembed("https://www.youtube.com/watch?v=abc123");

			// Then:
			assertThat(result.title()).isEqualTo("Video Title");
			assertThat(result.authorName()).isEqualTo("Author Name");
			assertThat(result.authorUrl()).isEqualTo("https://author.example");
			assertThat(result.thumbnailUrl()).isEqualTo("https://thumb.example");
			assertThat(result.thumbnailWidth()).isEqualTo(1280);
			assertThat(result.thumbnailHeight()).isEqualTo(720);
			assertThat(result.providerName()).isEqualTo("YouTube");
			assertThat(result.error()).isEmpty();
		}

		@Test
		void shouldUseDefaultProviderNameWhenBlankTest() throws IOException {
			// Given:
			JsonNode data = new ObjectMapper().readTree("{\"title\":\"Title\",\"provider_name\":\"\"}");
			PooledRestClientFactory factory = mockYoutubeFactory(mockOembedClient(data));
			YoutubeProperties properties = new YoutubeProperties();
			properties.setEnabled(true);
			properties.setTranscriptLang("en");
			properties.setTranscriptDebug(true);
			YoutubeClientImpl client = new YoutubeClientImpl(properties, factory);

			// When:
			YoutubeOEmbedMetadata result = client.fetchOembed("https://www.youtube.com/watch?v=abc123");

			// Then:
			assertThat(result.providerName()).isEqualTo("YouTube");
		}

		@Test
		void shouldHandleOembedErrorResponseWithMessageTest() {
			// Given:
			RestClient oembedClient = mockOembedClient(null);
			RestClient.ResponseSpec responseSpec = oembedClient.get()
					.uri(URI.create("https://www.youtube.com/oembed"))
					.header("Accept", "application/json")
					.header("User-Agent", "test")
					.retrieve();
			doThrow(new YoutubeExternalException("Video unavailable")).when(responseSpec)
					.onStatus(any(), any());
			PooledRestClientFactory factory = mockYoutubeFactory(oembedClient);
			YoutubeProperties properties = new YoutubeProperties();
			properties.setEnabled(true);
			properties.setTranscriptLang("en");
			properties.setTranscriptDebug(true);
			YoutubeClientImpl client = new YoutubeClientImpl(properties, factory);

			// When:
			YoutubeOEmbedMetadata result = client.fetchOembed("https://www.youtube.com/watch?v=abc123");

			// Then:
			assertThat(result.error()).isEqualTo("Video unavailable");
		}

		@Test
		void shouldHandleOembedErrorResponseWithoutMessageTest() {
			// Given:
			RestClient oembedClient = mockOembedClient(null);
			RestClient.ResponseSpec responseSpec = oembedClient.get()
					.uri(URI.create("https://www.youtube.com/oembed"))
					.header("Accept", "application/json")
					.header("User-Agent", "test")
					.retrieve();
			doThrow(new YoutubeExternalException("Failed to load YouTube metadata.")).when(responseSpec)
					.onStatus(any(), any());
			PooledRestClientFactory factory = mockYoutubeFactory(oembedClient);
			YoutubeProperties properties = new YoutubeProperties();
			properties.setEnabled(true);
			properties.setTranscriptLang("en");
			properties.setTranscriptDebug(true);
			YoutubeClientImpl client = new YoutubeClientImpl(properties, factory);

			// When:
			YoutubeOEmbedMetadata result = client.fetchOembed("https://www.youtube.com/watch?v=abc123");

			// Then:
			assertThat(result.error()).isEqualTo("Failed to load YouTube metadata.");
		}

		@Test
		void shouldHandleOembedRestClientExceptionTest() {
			// Given:
			RestClient oembedClient = mock(RestClient.class);
			when(oembedClient.get()).thenThrow(new RestClientException("connection failed"));
			PooledRestClientFactory factory = mockYoutubeFactory(oembedClient);
			YoutubeProperties properties = new YoutubeProperties();
			properties.setEnabled(true);
			properties.setTranscriptLang("en");
			properties.setTranscriptDebug(true);
			YoutubeClientImpl client = new YoutubeClientImpl(properties, factory);

			// When:
			YoutubeOEmbedMetadata result = client.fetchOembed("https://www.youtube.com/watch?v=abc123");

			// Then:
			assertThat(result.error()).isEqualTo("connection failed");
		}

		@Test
		void shouldAcceptYouTuBeUrlTest() throws IOException {
			// Given:
			JsonNode data = new ObjectMapper().readTree("{\"title\":\"Short link\"}");
			PooledRestClientFactory factory = mockYoutubeFactory(mockOembedClient(data));
			YoutubeProperties properties = new YoutubeProperties();
			properties.setEnabled(true);
			properties.setTranscriptLang("en");
			properties.setTranscriptDebug(true);
			YoutubeClientImpl client = new YoutubeClientImpl(properties, factory);

			// When:
			YoutubeOEmbedMetadata result = client.fetchOembed("https://youtu.be/abc123");

			// Then:
			assertThat(result.title()).isEqualTo("Short link");
			assertThat(result.error()).isEmpty();
		}
	}

	@Nested
	class FetchTranscriptSuccess {

		@Test
		void shouldFetchXmlTranscriptTest() throws IOException, InterruptedException {
			// Given:
			MockWebServer server = new MockWebServer();
			server.start();
			PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
			httpProps.setConnectTimeout(Duration.ofSeconds(5));
			httpProps.setResponseTimeout(Duration.ofSeconds(10));
			httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
			httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
			httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
			PooledRestClientFactory realFactory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
					new SimpleMeterRegistry());
			RestClient restClient = realFactory.createClient("youtube-test", baseUrl(server));
			PooledRestClientFactory factory = mock(PooledRestClientFactory.class);
			when(factory.createClient(anyString(), eq("https://www.youtube.com"))).thenReturn(restClient);
			YoutubeProperties properties = new YoutubeProperties();
			properties.setEnabled(true);
			properties.setTranscriptLang("en");
			properties.setTranscriptDebug(true);
			YoutubeClientImpl client = new YoutubeClientImpl(properties, factory);
			String captionBaseUrl = baseUrl(server) + "/caption";
			server.enqueue(new MockResponse()
					.setBody("<html>\"captionTracks\":[{\"baseUrl\":\"" + captionBaseUrl + "?lang=en\","
							+ "\"languageCode\":\"en\"}]</html>"));
			server.enqueue(new MockResponse()
					.setBody("""
							<?xml version="1.0"?>
							<transcript>
							  <text start="1.5">&lt;b&gt;Hello&lt;/b&gt;world</text>
							  <text start="3.0">Second line</text>
							</transcript>
							"""));

			try {
				// When:
				YoutubeTranscriptResult result = client.fetchTranscript("abc123");

				// Then:
				assertThat(result.videoId()).isEqualTo("abc123");
				assertThat(result.error()).isEmpty();
				assertThat(result.segments()).hasSize(2);
				assertThat(result.segments().get(0).text()).isEqualTo("Hello world");
				assertThat(result.segments().get(1).offsetSeconds()).isEqualTo(3.0);
				assertThat(server.takeRequest().getPath()).startsWith("/watch?v=abc123");
				assertThat(server.takeRequest().getPath()).isEqualTo("/caption?lang=en&fmt=vtt");
			} finally {
				realFactory.destroy();
				server.shutdown();
			}
		}

		@Test
		void shouldFetchJsonTranscriptTest() throws IOException {
			// Given:
			MockWebServer server = new MockWebServer();
			server.start();
			PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
			httpProps.setConnectTimeout(Duration.ofSeconds(5));
			httpProps.setResponseTimeout(Duration.ofSeconds(10));
			httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
			httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
			httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
			PooledRestClientFactory realFactory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
					new SimpleMeterRegistry());
			RestClient restClient = realFactory.createClient("youtube-test", baseUrl(server));
			PooledRestClientFactory factory = mock(PooledRestClientFactory.class);
			when(factory.createClient(anyString(), eq("https://www.youtube.com"))).thenReturn(restClient);
			YoutubeProperties properties = new YoutubeProperties();
			properties.setEnabled(true);
			properties.setTranscriptLang("en");
			properties.setTranscriptDebug(true);
			YoutubeClientImpl client = new YoutubeClientImpl(properties, factory);
			String captionBaseUrl = baseUrl(server) + "/caption";
			server.enqueue(new MockResponse()
					.setBody("<html>\"captionTracks\":[{\"baseUrl\":\"" + captionBaseUrl + "?lang=en\","
							+ "\"languageCode\":\"en\"}]</html>"));
			server.enqueue(new MockResponse()
					.setHeader("Content-Type", "application/json")
					.setBody("""
							{"events":[
							  {"tStartMs":1000,"segs":[{"utf8":"Hello "},{"utf8":"world"}]},
							  {"tStartMs":2500,"segs":[{"utf8":"  "}]},
							  {"tStartMs":4000,"segs":[{"utf8":"Next"}]}
							]}
							"""));

			try {
				// When:
				YoutubeTranscriptResult result = client.fetchTranscript("abc123");

				// Then:
				assertThat(result.error()).isEmpty();
				assertThat(result.segments()).hasSize(2);
				assertThat(result.segments().get(0).text()).isEqualTo("Hello world");
				assertThat(result.segments().get(0).offsetSeconds()).isEqualTo(1.0);
				assertThat(result.segments().get(1).text()).isEqualTo("Next");
			} finally {
				realFactory.destroy();
				server.shutdown();
			}
		}

		@Test
		void shouldFetchVttTranscriptTest() throws IOException {
			// Given:
			MockWebServer server = new MockWebServer();
			server.start();
			PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
			httpProps.setConnectTimeout(Duration.ofSeconds(5));
			httpProps.setResponseTimeout(Duration.ofSeconds(10));
			httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
			httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
			httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
			PooledRestClientFactory realFactory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
					new SimpleMeterRegistry());
			RestClient restClient = realFactory.createClient("youtube-test", baseUrl(server));
			PooledRestClientFactory factory = mock(PooledRestClientFactory.class);
			when(factory.createClient(anyString(), eq("https://www.youtube.com"))).thenReturn(restClient);
			YoutubeProperties properties = new YoutubeProperties();
			properties.setEnabled(true);
			properties.setTranscriptLang("en");
			properties.setTranscriptDebug(true);
			YoutubeClientImpl client = new YoutubeClientImpl(properties, factory);
			String captionBaseUrl = baseUrl(server) + "/caption";
			server.enqueue(new MockResponse()
					.setBody("<html>\"captionTracks\":[{\"baseUrl\":\"" + captionBaseUrl + "?lang=en\","
							+ "\"languageCode\":\"en\"}]</html>"));
			server.enqueue(new MockResponse()
					.setBody("""
							WEBVTT

							00:00.500 --> 00:02.000
							Hello

							1
							00:01:05.500 --> 00:01:07.000
							World
							"""));

			try {
				// When:
				YoutubeTranscriptResult result = client.fetchTranscript("abc123");

				// Then:
				assertThat(result.error()).isEmpty();
				assertThat(result.segments()).hasSize(2);
				assertThat(result.segments().get(0).text()).isEqualTo("Hello");
				assertThat(result.segments().get(0).offsetSeconds()).isEqualTo(0.5);
				assertThat(result.segments().get(1).text()).isEqualTo("World");
				assertThat(result.segments().get(1).offsetSeconds()).isEqualTo(65.5);
			} finally {
				realFactory.destroy();
				server.shutdown();
			}
		}

		@Test
		void shouldPreferConfiguredLanguageTrackTest() throws IOException, InterruptedException {
			// Given:
			MockWebServer server = new MockWebServer();
			server.start();
			PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
			httpProps.setConnectTimeout(Duration.ofSeconds(5));
			httpProps.setResponseTimeout(Duration.ofSeconds(10));
			httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
			httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
			httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
			PooledRestClientFactory realFactory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
					new SimpleMeterRegistry());
			RestClient restClient = realFactory.createClient("youtube-test", baseUrl(server));
			PooledRestClientFactory factory = mock(PooledRestClientFactory.class);
			when(factory.createClient(anyString(), eq("https://www.youtube.com"))).thenReturn(restClient);
			YoutubeProperties properties = new YoutubeProperties();
			properties.setEnabled(true);
			properties.setTranscriptLang("es");
			properties.setTranscriptDebug(true);
			YoutubeClientImpl client = new YoutubeClientImpl(properties, factory);
			String captionBaseUrl = baseUrl(server) + "/caption";
			server.enqueue(new MockResponse()
					.setBody("<html>\"captionTracks\":[{\"baseUrl\":\"" + captionBaseUrl + "?lang=en\","
							+ "\"languageCode\":\"en\"},{\"baseUrl\":\"" + captionBaseUrl + "?lang=es\","
							+ "\"languageCode\":\"es\"}]</html>"));
			server.enqueue(new MockResponse().setBody("<text start=\"1.0\">Hola</text>"));

			try {
				// When:
				YoutubeTranscriptResult result = client.fetchTranscript("abc123");

				// Then:
				assertThat(result.error()).isEmpty();
				assertThat(result.segments()).hasSize(1);
				assertThat(result.segments().get(0).text()).isEqualTo("Hola");
				assertThat(server.getRequestCount()).isEqualTo(2);
				server.takeRequest();
				assertThat(server.takeRequest().getPath()).isEqualTo("/caption?lang=es&fmt=vtt");
			} finally {
				realFactory.destroy();
				server.shutdown();
			}
		}

		@Test
		void shouldSortTracksWhenNoPreferredLanguageTest() throws IOException, InterruptedException {
			// Given:
			MockWebServer server = new MockWebServer();
			server.start();
			PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
			httpProps.setConnectTimeout(Duration.ofSeconds(5));
			httpProps.setResponseTimeout(Duration.ofSeconds(10));
			httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
			httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
			httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
			PooledRestClientFactory realFactory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
					new SimpleMeterRegistry());
			RestClient restClient = realFactory.createClient("youtube-test", baseUrl(server));
			PooledRestClientFactory factory = mock(PooledRestClientFactory.class);
			when(factory.createClient(anyString(), eq("https://www.youtube.com"))).thenReturn(restClient);
			YoutubeProperties properties = new YoutubeProperties();
			properties.setEnabled(true);
			properties.setTranscriptLang("");
			properties.setTranscriptDebug(true);
			YoutubeClientImpl client = new YoutubeClientImpl(properties, factory);
			String captionBaseUrl = baseUrl(server) + "/caption";
			server.enqueue(new MockResponse()
					.setBody("<html>\"captionTracks\":[{\"baseUrl\":\"" + captionBaseUrl + "?lang=es\","
							+ "\"languageCode\":\"es\"},{\"baseUrl\":\"" + captionBaseUrl + "?lang=de\","
							+ "\"languageCode\":\"de\"}]</html>"));
			server.enqueue(new MockResponse().setBody("<text start=\"1.0\">Hallo</text>"));

			try {
				// When:
				YoutubeTranscriptResult result = client.fetchTranscript("abc123");

				// Then:
				assertThat(result.error()).isEmpty();
				assertThat(result.segments()).hasSize(1);
				assertThat(result.segments().get(0).text()).isEqualTo("Hallo");
				assertThat(server.getRequestCount()).isEqualTo(2);
				server.takeRequest();
				assertThat(server.takeRequest().getPath()).isEqualTo("/caption?lang=de&fmt=vtt");
			} finally {
				realFactory.destroy();
				server.shutdown();
			}
		}

		@Test
		void shouldReturnErrorWhenCaptionTracksAreEmptyTest() throws IOException {
			// Given:
			MockWebServer server = new MockWebServer();
			server.start();
			PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
			httpProps.setConnectTimeout(Duration.ofSeconds(5));
			httpProps.setResponseTimeout(Duration.ofSeconds(10));
			httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
			httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
			httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
			PooledRestClientFactory realFactory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
					new SimpleMeterRegistry());
			RestClient restClient = realFactory.createClient("youtube-test", baseUrl(server));
			PooledRestClientFactory factory = mock(PooledRestClientFactory.class);
			when(factory.createClient(anyString(), eq("https://www.youtube.com"))).thenReturn(restClient);
			YoutubeProperties properties = new YoutubeProperties();
			properties.setEnabled(true);
			properties.setTranscriptLang("en");
			properties.setTranscriptDebug(true);
			YoutubeClientImpl client = new YoutubeClientImpl(properties, factory);
			server.enqueue(new MockResponse()
					.setBody("<html>\"captionTracks\":[{\"languageCode\":\"en\"}]</html>"));

			try {
				// When:
				YoutubeTranscriptResult result = client.fetchTranscript("abc123");

				// Then:
				assertThat(result.error()).contains("No caption tracks found");
			} finally {
				realFactory.destroy();
				server.shutdown();
			}
		}

		@Test
		void shouldReturnErrorWhenTranscriptIsEmptyTest() throws IOException {
			// Given:
			MockWebServer server = new MockWebServer();
			server.start();
			PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
			httpProps.setConnectTimeout(Duration.ofSeconds(5));
			httpProps.setResponseTimeout(Duration.ofSeconds(10));
			httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
			httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
			httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
			PooledRestClientFactory realFactory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
					new SimpleMeterRegistry());
			RestClient restClient = realFactory.createClient("youtube-test", baseUrl(server));
			PooledRestClientFactory factory = mock(PooledRestClientFactory.class);
			when(factory.createClient(anyString(), eq("https://www.youtube.com"))).thenReturn(restClient);
			YoutubeProperties properties = new YoutubeProperties();
			properties.setEnabled(true);
			properties.setTranscriptLang("en");
			properties.setTranscriptDebug(true);
			YoutubeClientImpl client = new YoutubeClientImpl(properties, factory);
			String captionBaseUrl = baseUrl(server) + "/caption";
			server.enqueue(new MockResponse()
					.setBody("<html>\"captionTracks\":[{\"baseUrl\":\"" + captionBaseUrl + "?lang=en\","
							+ "\"languageCode\":\"en\"}]</html>"));
			server.enqueue(new MockResponse().setBody("<transcript></transcript>"));

			try {
				// When:
				YoutubeTranscriptResult result = client.fetchTranscript("abc123");

				// Then:
				assertThat(result.error()).contains("Transcript was empty");
			} finally {
				realFactory.destroy();
				server.shutdown();
			}
		}

		@Test
		void shouldReturnErrorWhenTimedTextRequestFailsTest() throws IOException {
			// Given:
			MockWebServer server = new MockWebServer();
			server.start();
			PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
			httpProps.setConnectTimeout(Duration.ofSeconds(5));
			httpProps.setResponseTimeout(Duration.ofSeconds(10));
			httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
			httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
			httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
			PooledRestClientFactory realFactory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
					new SimpleMeterRegistry());
			RestClient restClient = realFactory.createClient("youtube-test", baseUrl(server));
			PooledRestClientFactory factory = mock(PooledRestClientFactory.class);
			when(factory.createClient(anyString(), eq("https://www.youtube.com"))).thenReturn(restClient);
			YoutubeProperties properties = new YoutubeProperties();
			properties.setEnabled(true);
			properties.setTranscriptLang("en");
			properties.setTranscriptDebug(true);
			YoutubeClientImpl client = new YoutubeClientImpl(properties, factory);
			String captionBaseUrl = baseUrl(server) + "/caption";
			server.enqueue(new MockResponse()
					.setBody("<html>\"captionTracks\":[{\"baseUrl\":\"" + captionBaseUrl + "?lang=en\","
							+ "\"languageCode\":\"en\"}]</html>"));
			server.enqueue(new MockResponse().setResponseCode(500).setBody("Server Error"));

			try {
				// When:
				YoutubeTranscriptResult result = client.fetchTranscript("abc123");

				// Then:
				assertThat(result.error()).isNotEmpty();
			} finally {
				realFactory.destroy();
				server.shutdown();
			}
		}
	}

}
