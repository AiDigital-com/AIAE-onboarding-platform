package com.aidigital.aionboarding.external.openai.impl;

import com.aidigital.aionboarding.external.common.http.PooledRestClientFactory;
import com.aidigital.aionboarding.external.common.http.config.PooledHttpClientProperties;
import com.aidigital.aionboarding.external.openai.OpenAiExternalException;
import com.aidigital.aionboarding.external.openai.config.OpenAiProperties;
import com.aidigital.aionboarding.external.openai.model.OpenAiContentPart;
import com.aidigital.aionboarding.external.openai.model.OpenAiFileInput;
import com.aidigital.aionboarding.external.openai.model.OpenAiFileUploadResponse;
import com.aidigital.aionboarding.external.openai.model.OpenAiOutputItem;
import com.aidigital.aionboarding.external.openai.model.OpenAiResponsesResponse;
import com.aidigital.aionboarding.external.openai.model.OpenAiResponsesResult;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.zalando.logbook.Logbook;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.instancio.Select.field;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

class OpenAiClientImplTest {

	private static final String VALID_RESPONSES_BODY = """
			{
			  "id": "resp-test-001",
			  "usage": {"input_tokens": 11, "output_tokens": 7, "total_tokens": 18},
			  "output": [
			    {
			      "type": "message",
			      "content": [
			        {"type": "output_text", "text": "Hello from GPT"}
			      ]
			    }
			  ]
			}
			""";

	private static String baseUrl(MockWebServer server) {
		return server.url("/").toString().replaceAll("/$", "");
	}

	@Test
	void shouldSendPostToResponsesEndpointByDefaultTest() throws InterruptedException, IOException {
		// Given:
		MockWebServer server = new MockWebServer();
		server.start();
		OpenAiProperties properties = new OpenAiProperties();
		properties.setEnabled(true);
		properties.setBaseUrl(baseUrl(server));
		properties.setApiKey("sk-test-secret-key");
		properties.setModel("gpt-4o");
		properties.setMaxTokens(256);
		PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
		httpProps.setConnectTimeout(Duration.ofSeconds(5));
		httpProps.setResponseTimeout(Duration.ofSeconds(10));
		httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
		httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
		httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
		PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
				new SimpleMeterRegistry());
		server.enqueue(new MockResponse()
				.setResponseCode(200)
				.setHeader("Content-Type", "application/json")
				.setBody(VALID_RESPONSES_BODY));
		OpenAiClientImpl spy = Mockito.spy(new OpenAiClientImpl(properties, factory));
		doReturn("stubbed").when(spy).extractResponsesText(any());

		try {
			// When:
			spy.complete("sys", "msg");

			// Then:
			RecordedRequest request = server.takeRequest();
			assertThat(request.getMethod()).isEqualTo("POST");
			assertThat(request.getPath()).isEqualTo("/v1/responses");
		} finally {
			factory.destroy();
			server.shutdown();
		}
	}

	@Test
	void shouldSendBearerAuthorizationHeaderTest() throws InterruptedException, IOException {
		// Given:
		MockWebServer server = new MockWebServer();
		server.start();
		OpenAiProperties properties = new OpenAiProperties();
		properties.setEnabled(true);
		properties.setBaseUrl(baseUrl(server));
		properties.setApiKey("sk-test-secret-key");
		properties.setModel("gpt-4o");
		properties.setMaxTokens(256);
		PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
		httpProps.setConnectTimeout(Duration.ofSeconds(5));
		httpProps.setResponseTimeout(Duration.ofSeconds(10));
		httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
		httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
		httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
		PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
				new SimpleMeterRegistry());
		server.enqueue(new MockResponse()
				.setResponseCode(200)
				.setHeader("Content-Type", "application/json")
				.setBody(VALID_RESPONSES_BODY));
		OpenAiClientImpl spy = Mockito.spy(new OpenAiClientImpl(properties, factory));
		doReturn("stubbed").when(spy).extractResponsesText(any());

		try {
			// When:
			spy.complete("sys", "msg");

			// Then:
			RecordedRequest request = server.takeRequest();
			assertThat(request.getHeader("Authorization")).isEqualTo("Bearer sk-test-secret-key");
		} finally {
			factory.destroy();
			server.shutdown();
		}
	}

	@Test
	void shouldSendResponsesRequestBodyTest() throws InterruptedException, IOException {
		// Given:
		MockWebServer server = new MockWebServer();
		server.start();
		OpenAiProperties properties = new OpenAiProperties();
		properties.setEnabled(true);
		properties.setBaseUrl(baseUrl(server));
		properties.setApiKey("sk-test-secret-key");
		properties.setModel("gpt-4o");
		properties.setMaxTokens(256);
		PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
		httpProps.setConnectTimeout(Duration.ofSeconds(5));
		httpProps.setResponseTimeout(Duration.ofSeconds(10));
		httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
		httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
		httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
		PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
				new SimpleMeterRegistry());
		server.enqueue(new MockResponse()
				.setResponseCode(200)
				.setHeader("Content-Type", "application/json")
				.setBody(VALID_RESPONSES_BODY));
		OpenAiClientImpl spy = Mockito.spy(new OpenAiClientImpl(properties, factory));
		doReturn("stubbed").when(spy).extractResponsesText(any());

		try {
			// When:
			spy.complete("You are a helper", "What is 2+2?");

			// Then:
			String body = server.takeRequest().getBody().readUtf8();
			assertThat(body).contains("\"model\":\"gpt-4o\"");
			assertThat(body).contains("\"instructions\":");
			assertThat(body).doesNotContain("\"max_output_tokens\"");
			assertThat(body).doesNotContain("\"role\":\"system\"");
			assertThat(body).doesNotContain("\"role\":\"user\"");
		} finally {
			factory.destroy();
			server.shutdown();
		}
	}

	@Test
	void shouldThrowOpenAiExternalExceptionOnHttp4xxTest() throws IOException {
		// Given:
		MockWebServer server = new MockWebServer();
		server.start();
		OpenAiProperties properties = new OpenAiProperties();
		properties.setEnabled(true);
		properties.setBaseUrl(baseUrl(server));
		properties.setApiKey("sk-test-secret-key");
		properties.setModel("gpt-4o");
		properties.setMaxTokens(256);
		PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
		httpProps.setConnectTimeout(Duration.ofSeconds(5));
		httpProps.setResponseTimeout(Duration.ofSeconds(10));
		httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
		httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
		httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
		PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
				new SimpleMeterRegistry());
		server.enqueue(new MockResponse()
				.setResponseCode(401)
				.setHeader("Content-Type", "application/json")
				.setBody("{\"error\":{\"message\":\"Invalid API key\"}}"));
		OpenAiClientImpl client = new OpenAiClientImpl(properties, factory);

		try {
			// When / Then:
			assertThatThrownBy(() -> client.complete("sys", "msg"))
					.isInstanceOf(OpenAiExternalException.class)
					.satisfies(ex -> assertThat(((OpenAiExternalException) ex).getHttpStatus()).isEqualTo(401));
		} finally {
			factory.destroy();
			server.shutdown();
		}
	}

	@Test
	void shouldExtractTextFromResponsesOutputTest() throws IOException {
		// Given:
		OpenAiProperties properties = new OpenAiProperties();
		properties.setBaseUrl("http://localhost:12345");
		properties.setApiKey("sk-test-secret-key");
		properties.setModel("gpt-4o");
		PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
		httpProps.setConnectTimeout(Duration.ofSeconds(5));
		httpProps.setResponseTimeout(Duration.ofSeconds(10));
		httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
		httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
		httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
		PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
				new SimpleMeterRegistry());
		OpenAiClientImpl client = new OpenAiClientImpl(properties, factory);
		OpenAiResponsesResponse response = Instancio.of(OpenAiResponsesResponse.class)
				.set(field("id"), null)
				.set(field("usage"), null)
				.set(field("output"), List.of(
						Instancio.of(OpenAiOutputItem.class)
								.set(field("type"), "message")
								.set(field("content"), List.of(
										Instancio.of(OpenAiContentPart.class)
												.set(field("type"), "output_text")
												.set(field("text"), "Hello world")
												.create()))
								.create()))
				.create();

		try {
			// When:
			String result = client.extractResponsesText(response);

			// Then:
			assertThat(result).isEqualTo("Hello world");
		} finally {
			factory.destroy();
		}
	}

	@Test
	void shouldThrowWhenResponsesOutputEmptyTest() throws IOException {
		// Given:
		OpenAiProperties properties = new OpenAiProperties();
		properties.setBaseUrl("http://localhost:12345");
		properties.setApiKey("sk-test-secret-key");
		properties.setModel("gpt-4o");
		PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
		httpProps.setConnectTimeout(Duration.ofSeconds(5));
		httpProps.setResponseTimeout(Duration.ofSeconds(10));
		httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
		httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
		httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
		PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
				new SimpleMeterRegistry());
		OpenAiClientImpl client = new OpenAiClientImpl(properties, factory);
		OpenAiResponsesResponse response = Instancio.of(OpenAiResponsesResponse.class)
				.set(field("id"), null)
				.set(field("usage"), null)
				.set(field("output"), List.of())
				.create();

		try {
			// When / Then:
			assertThatThrownBy(() -> client.extractResponsesText(response))
					.isInstanceOf(OpenAiExternalException.class)
					.hasMessageContaining("empty output");
		} finally {
			factory.destroy();
		}
	}

	@Test
	void shouldReturnTypedResultWithResponseIdAndUsageTest() throws IOException {
		// Given:
		MockWebServer server = new MockWebServer();
		server.start();
		OpenAiProperties properties = new OpenAiProperties();
		properties.setEnabled(true);
		properties.setBaseUrl(baseUrl(server));
		properties.setApiKey("sk-test-secret-key");
		properties.setModel("gpt-4o");
		properties.setMaxTokens(256);
		PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
		httpProps.setConnectTimeout(Duration.ofSeconds(5));
		httpProps.setResponseTimeout(Duration.ofSeconds(10));
		httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
		httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
		httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
		PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
				new SimpleMeterRegistry());
		server.enqueue(new MockResponse()
				.setResponseCode(200)
				.setHeader("Content-Type", "application/json")
				.setBody(VALID_RESPONSES_BODY));
		OpenAiClientImpl client = new OpenAiClientImpl(properties, factory);

		try {
			// When:
			OpenAiResponsesResult result = client.createResponse("inst", "msg", "gpt-4o");

			// Then:
			assertThat(result.responseId()).isEqualTo("resp-test-001");
			assertThat(result.usage().totalTokens()).isEqualTo(18);
		} finally {
			factory.destroy();
			server.shutdown();
		}
	}

	@Test
	void shouldThrowWhenResponseBodyIsNullTest() throws IOException {
		// Given:
		MockWebServer server = new MockWebServer();
		server.start();
		OpenAiProperties properties = new OpenAiProperties();
		properties.setEnabled(true);
		properties.setBaseUrl(baseUrl(server));
		properties.setApiKey("sk-test-secret-key");
		properties.setModel("gpt-4o");
		properties.setMaxTokens(256);
		PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
		httpProps.setConnectTimeout(Duration.ofSeconds(5));
		httpProps.setResponseTimeout(Duration.ofSeconds(10));
		httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
		httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
		httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
		PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
				new SimpleMeterRegistry());
		server.enqueue(new MockResponse()
				.setResponseCode(200)
				.setHeader("Content-Type", "application/json")
				.setBody("null"));
		OpenAiClientImpl client = new OpenAiClientImpl(properties, factory);

		try {
			// When / Then:
			assertThatThrownBy(() -> client.createResponse("inst", "msg", "gpt-4o"))
					.isInstanceOf(OpenAiExternalException.class)
					.hasMessageContaining("null response body");
		} finally {
			factory.destroy();
			server.shutdown();
		}
	}

	@Test
	void shouldSendFileInputItemInStructuredInputTest() throws InterruptedException, IOException {
		// Given:
		MockWebServer server = new MockWebServer();
		server.start();
		OpenAiProperties properties = new OpenAiProperties();
		properties.setEnabled(true);
		properties.setBaseUrl(baseUrl(server));
		properties.setApiKey("sk-test-secret-key");
		properties.setModel("gpt-4o");
		properties.setMaxTokens(256);
		PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
		httpProps.setConnectTimeout(Duration.ofSeconds(5));
		httpProps.setResponseTimeout(Duration.ofSeconds(10));
		httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
		httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
		httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
		PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
				new SimpleMeterRegistry());
		server.enqueue(new MockResponse()
				.setResponseCode(200)
				.setHeader("Content-Type", "application/json")
				.setBody(VALID_RESPONSES_BODY));
		OpenAiClientImpl spy = Mockito.spy(new OpenAiClientImpl(properties, factory));
		doReturn("stubbed").when(spy).extractResponsesText(any());
		OpenAiFileInput fileInput = Instancio.of(OpenAiFileInput.class)
				.set(field("type"), "input_file")
				.set(field("fileId"), "file-abc")
				.create();

		try {
			// When:
			spy.createResponse("instructions", "userMsg", "gpt-4o", List.of(fileInput));

			// Then:
			String body = server.takeRequest().getBody().readUtf8();
			assertThat(body).contains("\"type\":\"input_file\"");
			assertThat(body).contains("\"file_id\":\"file-abc\"");
			assertThat(body).contains("\"input_text\"");
		} finally {
			factory.destroy();
			server.shutdown();
		}
	}

	@Test
	void shouldSendPromptCacheKeyWhenProvidedTest() throws InterruptedException, IOException {
		// Given:
		MockWebServer server = new MockWebServer();
		server.start();
		OpenAiProperties properties = new OpenAiProperties();
		properties.setEnabled(true);
		properties.setBaseUrl(baseUrl(server));
		properties.setApiKey("sk-test-secret-key");
		properties.setModel("gpt-4o");
		properties.setMaxTokens(256);
		PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
		httpProps.setConnectTimeout(Duration.ofSeconds(5));
		httpProps.setResponseTimeout(Duration.ofSeconds(10));
		httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
		httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
		httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
		PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
				new SimpleMeterRegistry());
		server.enqueue(new MockResponse()
				.setResponseCode(200)
				.setHeader("Content-Type", "application/json")
				.setBody(VALID_RESPONSES_BODY));
		OpenAiClientImpl spy = Mockito.spy(new OpenAiClientImpl(properties, factory));
		doReturn("stubbed").when(spy).extractResponsesText(any());

		try {
			// When:
			spy.createResponse("instructions", "userMsg", "gpt-4o", "lesson-cache-key", List.of());

			// Then:
			String body = server.takeRequest().getBody().readUtf8();
			assertThat(body).contains("\"prompt_cache_key\":\"lesson-cache-key\"");
		} finally {
			factory.destroy();
			server.shutdown();
		}
	}

	@Test
	void shouldSendPromptCacheKeyWithFileInputsTest() throws InterruptedException, IOException {
		// Given:
		MockWebServer server = new MockWebServer();
		server.start();
		OpenAiProperties properties = new OpenAiProperties();
		properties.setEnabled(true);
		properties.setBaseUrl(baseUrl(server));
		properties.setApiKey("sk-test-secret-key");
		properties.setModel("gpt-4o");
		properties.setMaxTokens(256);
		PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
		httpProps.setConnectTimeout(Duration.ofSeconds(5));
		httpProps.setResponseTimeout(Duration.ofSeconds(10));
		httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
		httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
		httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
		PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
				new SimpleMeterRegistry());
		server.enqueue(new MockResponse()
				.setResponseCode(200)
				.setHeader("Content-Type", "application/json")
				.setBody(VALID_RESPONSES_BODY));
		OpenAiClientImpl spy = Mockito.spy(new OpenAiClientImpl(properties, factory));
		doReturn("stubbed").when(spy).extractResponsesText(any());
		OpenAiFileInput fileInput = Instancio.of(OpenAiFileInput.class)
				.set(field("type"), "input_file")
				.set(field("fileId"), "file-abc")
				.create();

		try {
			// When:
			spy.createResponse("instructions", "userMsg", "gpt-4o", "lesson-cache-key", List.of(fileInput));

			// Then:
			String body = server.takeRequest().getBody().readUtf8();
			assertThat(body).contains("\"prompt_cache_key\":\"lesson-cache-key\"");
			assertThat(body).contains("\"type\":\"input_file\"");
			assertThat(body).contains("\"file_id\":\"file-abc\"");
		} finally {
			factory.destroy();
			server.shutdown();
		}
	}

	@Test
	void shouldOmitPromptCacheKeyWhenNotProvidedTest() throws InterruptedException, IOException {
		// Given:
		MockWebServer server = new MockWebServer();
		server.start();
		OpenAiProperties properties = new OpenAiProperties();
		properties.setEnabled(true);
		properties.setBaseUrl(baseUrl(server));
		properties.setApiKey("sk-test-secret-key");
		properties.setModel("gpt-4o");
		properties.setMaxTokens(256);
		PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
		httpProps.setConnectTimeout(Duration.ofSeconds(5));
		httpProps.setResponseTimeout(Duration.ofSeconds(10));
		httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
		httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
		httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
		PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
				new SimpleMeterRegistry());
		server.enqueue(new MockResponse()
				.setResponseCode(200)
				.setHeader("Content-Type", "application/json")
				.setBody(VALID_RESPONSES_BODY));
		OpenAiClientImpl spy = Mockito.spy(new OpenAiClientImpl(properties, factory));
		doReturn("stubbed").when(spy).extractResponsesText(any());
		OpenAiFileInput fileInput = Instancio.of(OpenAiFileInput.class)
				.set(field("type"), "input_file")
				.set(field("fileId"), "file-abc")
				.create();

		try {
			// When:
			spy.createResponse("instructions", "userMsg", "gpt-4o", List.of(fileInput));

			// Then:
			String body = server.takeRequest().getBody().readUtf8();
			assertThat(body).doesNotContain("\"prompt_cache_key\"");
		} finally {
			factory.destroy();
			server.shutdown();
		}
	}

	@Test
	void shouldSendStoreAndPreviousResponseIdWhenProvidedTest() throws InterruptedException, IOException {
		// Given:
		MockWebServer server = new MockWebServer();
		server.start();
		OpenAiProperties properties = new OpenAiProperties();
		properties.setEnabled(true);
		properties.setBaseUrl(baseUrl(server));
		properties.setApiKey("sk-test-secret-key");
		properties.setModel("gpt-4o");
		properties.setMaxTokens(256);
		PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
		httpProps.setConnectTimeout(Duration.ofSeconds(5));
		httpProps.setResponseTimeout(Duration.ofSeconds(10));
		httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
		httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
		httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
		PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
				new SimpleMeterRegistry());
		server.enqueue(new MockResponse()
				.setResponseCode(200)
				.setHeader("Content-Type", "application/json")
				.setBody(VALID_RESPONSES_BODY));
		OpenAiClientImpl spy = Mockito.spy(new OpenAiClientImpl(properties, factory));
		doReturn("stubbed").when(spy).extractResponsesText(any());

		try {
			// When:
			spy.createResponse("instructions", "userMsg", "gpt-4o", "lesson-cache-key", List.of(),
					Boolean.TRUE, "resp-previous-1");

			// Then:
			String body = server.takeRequest().getBody().readUtf8();
			assertThat(body).contains("\"store\":true");
			assertThat(body).contains("\"previous_response_id\":\"resp-previous-1\"");
		} finally {
			factory.destroy();
			server.shutdown();
		}
	}

	@Test
	void shouldOmitStoreAndPreviousResponseIdWhenNotProvidedTest() throws InterruptedException, IOException {
		// Given:
		MockWebServer server = new MockWebServer();
		server.start();
		OpenAiProperties properties = new OpenAiProperties();
		properties.setEnabled(true);
		properties.setBaseUrl(baseUrl(server));
		properties.setApiKey("sk-test-secret-key");
		properties.setModel("gpt-4o");
		properties.setMaxTokens(256);
		PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
		httpProps.setConnectTimeout(Duration.ofSeconds(5));
		httpProps.setResponseTimeout(Duration.ofSeconds(10));
		httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
		httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
		httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
		PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
				new SimpleMeterRegistry());
		server.enqueue(new MockResponse()
				.setResponseCode(200)
				.setHeader("Content-Type", "application/json")
				.setBody(VALID_RESPONSES_BODY));
		OpenAiClientImpl spy = Mockito.spy(new OpenAiClientImpl(properties, factory));
		doReturn("stubbed").when(spy).extractResponsesText(any());

		try {
			// When:
			spy.createResponse("instructions", "userMsg", "gpt-4o", "lesson-cache-key", List.of(), null, null);

			// Then:
			String body = server.takeRequest().getBody().readUtf8();
			assertThat(body).doesNotContain("\"store\"");
			assertThat(body).doesNotContain("\"previous_response_id\"");
		} finally {
			factory.destroy();
			server.shutdown();
		}
	}

	@Test
	void shouldReturnResponseIdWhenCompletingTest() throws IOException {
		// Given:
		MockWebServer server = new MockWebServer();
		server.start();
		OpenAiProperties properties = new OpenAiProperties();
		properties.setEnabled(true);
		properties.setBaseUrl(baseUrl(server));
		properties.setApiKey("sk-test-secret-key");
		properties.setModel("gpt-4o");
		properties.setMaxTokens(256);
		PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
		httpProps.setConnectTimeout(Duration.ofSeconds(5));
		httpProps.setResponseTimeout(Duration.ofSeconds(10));
		httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
		httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
		httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
		PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
				new SimpleMeterRegistry());
		server.enqueue(new MockResponse()
				.setResponseCode(200)
				.setHeader("Content-Type", "application/json")
				.setBody(VALID_RESPONSES_BODY));

		try {
			// When:
			OpenAiResponsesResult result = new OpenAiClientImpl(properties, factory).complete("sys", "user");

			// Then:
			assertThat(result.responseId()).isEqualTo("resp-test-001");
		} finally {
			factory.destroy();
			server.shutdown();
		}
	}

	@Test
	void shouldThrowOn5xxErrorWhenCompletingTest() throws IOException {
		// Given:
		MockWebServer server = new MockWebServer();
		server.start();
		OpenAiProperties properties = new OpenAiProperties();
		properties.setEnabled(true);
		properties.setBaseUrl(baseUrl(server));
		properties.setApiKey("sk-test-secret-key");
		properties.setModel("gpt-4o");
		properties.setMaxTokens(256);
		PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
		httpProps.setConnectTimeout(Duration.ofSeconds(5));
		httpProps.setResponseTimeout(Duration.ofSeconds(10));
		httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
		httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
		httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
		PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
				new SimpleMeterRegistry());
		server.enqueue(new MockResponse()
				.setResponseCode(503)
				.setHeader("Content-Type", "application/json")
				.setBody("Service Unavailable"));

		try {
			// When / Then:
			assertThatThrownBy(() -> new OpenAiClientImpl(properties, factory).complete("sys", "msg"))
					.isInstanceOf(OpenAiExternalException.class);
		} finally {
			factory.destroy();
			server.shutdown();
		}
	}

	@Test
	void shouldReturnFileUploadResponseTest() throws IOException {
		// Given:
		MockWebServer server = new MockWebServer();
		server.start();
		OpenAiProperties properties = new OpenAiProperties();
		properties.setEnabled(true);
		properties.setBaseUrl(baseUrl(server));
		properties.setApiKey("sk-test-secret-key");
		properties.setModel("gpt-4o");
		properties.setMaxTokens(256);
		PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
		httpProps.setConnectTimeout(Duration.ofSeconds(5));
		httpProps.setResponseTimeout(Duration.ofSeconds(10));
		httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
		httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
		httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
		PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
				new SimpleMeterRegistry());
		server.enqueue(new MockResponse()
				.setResponseCode(200)
				.setHeader("Content-Type", "application/json")
				.setBody("{\"id\":\"file-abc\",\"filename\":\"test.pdf\",\"purpose\":\"assistants\",\"bytes\":100," +
						"\"status\":\"processed\"}"));

		try {
			// When:
			OpenAiFileUploadResponse result = new OpenAiClientImpl(properties, factory)
					.uploadFile(new byte[]{1, 2, 3}, "test.pdf", "assistants");

			// Then:
			assertThat(result.id()).isEqualTo("file-abc");
		} finally {
			factory.destroy();
			server.shutdown();
		}
	}

	@Test
	void shouldThrowOn4xxErrorWhenCompletingTest() throws IOException {
		// Given:
		MockWebServer server = new MockWebServer();
		server.start();
		OpenAiProperties properties = new OpenAiProperties();
		properties.setEnabled(true);
		properties.setBaseUrl(baseUrl(server));
		properties.setApiKey("sk-test-secret-key");
		properties.setModel("gpt-4o");
		properties.setMaxTokens(256);
		PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
		httpProps.setConnectTimeout(Duration.ofSeconds(5));
		httpProps.setResponseTimeout(Duration.ofSeconds(10));
		httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
		httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
		httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
		PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
				new SimpleMeterRegistry());
		server.enqueue(new MockResponse()
				.setResponseCode(400)
				.setHeader("Content-Type", "application/json")
				.setBody("{\"error\":{\"message\":\"Bad request\"}}"));

		try {
			// When / Then:
			assertThatThrownBy(() -> new OpenAiClientImpl(properties, factory).complete("sys", "msg"))
					.isInstanceOf(OpenAiExternalException.class);
		} finally {
			factory.destroy();
			server.shutdown();
		}
	}

	@Test
	void shouldReturnResponseWithStoreAndPreviousTest() throws IOException {
		// Given:
		MockWebServer server = new MockWebServer();
		server.start();
		OpenAiProperties properties = new OpenAiProperties();
		properties.setEnabled(true);
		properties.setBaseUrl(baseUrl(server));
		properties.setApiKey("sk-test-secret-key");
		properties.setModel("gpt-4o");
		properties.setMaxTokens(256);
		PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
		httpProps.setConnectTimeout(Duration.ofSeconds(5));
		httpProps.setResponseTimeout(Duration.ofSeconds(10));
		httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
		httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
		httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
		PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
				new SimpleMeterRegistry());
		server.enqueue(new MockResponse()
				.setResponseCode(200)
				.setHeader("Content-Type", "application/json")
				.setBody(VALID_RESPONSES_BODY));
		OpenAiClientImpl client = new OpenAiClientImpl(properties, factory);

		try {
			// When:
			OpenAiResponsesResult result = client.createResponse("inst", "msg", "gpt-4o", "ck", List.of(), true, "prev" +
					"-resp");

			// Then:
			assertThat(result.responseId()).isEqualTo("resp-test-001");
		} finally {
			factory.destroy();
			server.shutdown();
		}
	}

	@Test
	void shouldThrowOnNullBodyWhenCompletingTest() throws IOException {
		// Given:
		MockWebServer server = new MockWebServer();
		server.start();
		OpenAiProperties properties = new OpenAiProperties();
		properties.setEnabled(true);
		properties.setBaseUrl(baseUrl(server));
		properties.setApiKey("sk-test-secret-key");
		properties.setModel("gpt-4o");
		properties.setMaxTokens(256);
		PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
		httpProps.setConnectTimeout(Duration.ofSeconds(5));
		httpProps.setResponseTimeout(Duration.ofSeconds(10));
		httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
		httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
		httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
		PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
				new SimpleMeterRegistry());
		server.enqueue(new MockResponse()
				.setResponseCode(200)
				.setHeader("Content-Type", "application/json")
				.setBody("null"));

		try {
			// When / Then:
			assertThatThrownBy(() -> new OpenAiClientImpl(properties, factory).complete("sys", "msg"))
					.isInstanceOf(OpenAiExternalException.class);
		} finally {
			factory.destroy();
			server.shutdown();
		}
	}

	@Test
	void shouldThrowOn5xxForCreateResponseTest() throws IOException {
		// Given:
		MockWebServer server = new MockWebServer();
		server.start();
		OpenAiProperties properties = new OpenAiProperties();
		properties.setEnabled(true);
		properties.setBaseUrl(baseUrl(server));
		properties.setApiKey("sk-test-secret-key");
		properties.setModel("gpt-4o");
		properties.setMaxTokens(256);
		PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
		httpProps.setConnectTimeout(Duration.ofSeconds(5));
		httpProps.setResponseTimeout(Duration.ofSeconds(10));
		httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
		httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
		httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
		PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
				new SimpleMeterRegistry());
		server.enqueue(new MockResponse()
				.setResponseCode(500)
				.setHeader("Content-Type", "application/json")
				.setBody("Internal Server Error"));

		try {
			// When / Then:
			assertThatThrownBy(() -> new OpenAiClientImpl(properties, factory)
					.createResponse("inst", "msg", "gpt-4o"))
					.isInstanceOf(OpenAiExternalException.class);
		} finally {
			factory.destroy();
			server.shutdown();
		}
	}

	@Test
	void shouldThrowOn4xxForUploadFileTest() throws IOException {
		// Given:
		MockWebServer server = new MockWebServer();
		server.start();
		OpenAiProperties properties = new OpenAiProperties();
		properties.setEnabled(true);
		properties.setBaseUrl(baseUrl(server));
		properties.setApiKey("sk-test-secret-key");
		properties.setModel("gpt-4o");
		properties.setMaxTokens(256);
		PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
		httpProps.setConnectTimeout(Duration.ofSeconds(5));
		httpProps.setResponseTimeout(Duration.ofSeconds(10));
		httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
		httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
		httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
		PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
				new SimpleMeterRegistry());
		server.enqueue(new MockResponse()
				.setResponseCode(400)
				.setHeader("Content-Type", "application/json")
				.setBody("{\"error\":{\"message\":\"Invalid file\"}}"));

		try {
			// When / Then:
			assertThatThrownBy(() -> new OpenAiClientImpl(properties, factory)
					.uploadFile(new byte[]{1, 2, 3}, "test.pdf", "assistants"))
					.isInstanceOf(OpenAiExternalException.class);
		} finally {
			factory.destroy();
			server.shutdown();
		}
	}

	@Test
	void shouldThrowWhenUploadFileContentIsEmptyTest() throws IOException {
		// Given:
		OpenAiProperties properties = new OpenAiProperties();
		properties.setBaseUrl("http://localhost:12345");
		properties.setApiKey("sk-test-secret-key");
		properties.setModel("gpt-4o");
		PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
		httpProps.setConnectTimeout(Duration.ofSeconds(5));
		httpProps.setResponseTimeout(Duration.ofSeconds(10));
		httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
		httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
		httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
		PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
				new SimpleMeterRegistry());
		OpenAiClientImpl client = new OpenAiClientImpl(properties, factory);

		try {
			// When / Then:
			assertThatThrownBy(() -> client.uploadFile(new byte[0], "test.pdf", "assistants"))
					.isInstanceOf(OpenAiExternalException.class)
					.hasMessageContaining("file content is required");
		} finally {
			factory.destroy();
		}
	}

	@Test
	void shouldThrowWhenModelIsNullTest() throws IOException {
		// Given:
		OpenAiProperties properties = new OpenAiProperties();
		properties.setBaseUrl("http://localhost:12345");
		properties.setApiKey("sk-test-secret-key");
		properties.setModel("gpt-4o");
		PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
		httpProps.setConnectTimeout(Duration.ofSeconds(5));
		httpProps.setResponseTimeout(Duration.ofSeconds(10));
		httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
		httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
		httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
		PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
				new SimpleMeterRegistry());
		OpenAiClientImpl client = new OpenAiClientImpl(properties, factory);

		try {
			// When / Then:
			assertThatThrownBy(() -> client.createResponse("inst", "msg", null))
					.isInstanceOf(OpenAiExternalException.class)
					.hasMessageContaining("model is required");
		} finally {
			factory.destroy();
		}
	}

	@Test
	void shouldThrowWhenModelIsBlankTest() throws IOException {
		// Given:
		OpenAiProperties properties = new OpenAiProperties();
		properties.setBaseUrl("http://localhost:12345");
		properties.setApiKey("sk-test-secret-key");
		properties.setModel("gpt-4o");
		PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
		httpProps.setConnectTimeout(Duration.ofSeconds(5));
		httpProps.setResponseTimeout(Duration.ofSeconds(10));
		httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
		httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
		httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
		PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
				new SimpleMeterRegistry());
		OpenAiClientImpl client = new OpenAiClientImpl(properties, factory);

		try {
			// When / Then:
			assertThatThrownBy(() -> client.createResponse("inst", "msg", "  "))
					.isInstanceOf(OpenAiExternalException.class)
					.hasMessageContaining("model is required");
		} finally {
			factory.destroy();
		}
	}

	@Test
	void shouldThrowWhenExtractingTextFromNullResponseTest() throws IOException {
		// Given:
		OpenAiProperties properties = new OpenAiProperties();
		properties.setBaseUrl("http://localhost:12345");
		properties.setApiKey("sk-test-secret-key");
		properties.setModel("gpt-4o");
		PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
		httpProps.setConnectTimeout(Duration.ofSeconds(5));
		httpProps.setResponseTimeout(Duration.ofSeconds(10));
		httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
		httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
		httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
		PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
				new SimpleMeterRegistry());
		OpenAiClientImpl client = new OpenAiClientImpl(properties, factory);

		try {
			// When / Then:
			assertThatThrownBy(() -> client.extractResponsesText(null))
					.isInstanceOf(OpenAiExternalException.class)
					.hasMessageContaining("empty output");
		} finally {
			factory.destroy();
		}
	}

	@Test
	void shouldThrowWhenExtractingTextFromNullOutputTest() throws IOException {
		// Given:
		OpenAiProperties properties = new OpenAiProperties();
		properties.setBaseUrl("http://localhost:12345");
		properties.setApiKey("sk-test-secret-key");
		properties.setModel("gpt-4o");
		PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
		httpProps.setConnectTimeout(Duration.ofSeconds(5));
		httpProps.setResponseTimeout(Duration.ofSeconds(10));
		httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
		httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
		httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
		PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
				new SimpleMeterRegistry());
		OpenAiClientImpl client = new OpenAiClientImpl(properties, factory);
		OpenAiResponsesResponse response = Instancio.of(OpenAiResponsesResponse.class)
				.set(field("id"), null)
				.set(field("usage"), null)
				.set(field("output"), null)
				.create();

		try {
			// When / Then:
			assertThatThrownBy(() -> client.extractResponsesText(response))
					.isInstanceOf(OpenAiExternalException.class)
					.hasMessageContaining("empty output");
		} finally {
			factory.destroy();
		}
	}

	@Test
	void shouldSkipItemsWithNullContentWhenExtractingTextTest() throws IOException {
		// Given:
		OpenAiProperties properties = new OpenAiProperties();
		properties.setBaseUrl("http://localhost:12345");
		properties.setApiKey("sk-test-secret-key");
		properties.setModel("gpt-4o");
		PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
		httpProps.setConnectTimeout(Duration.ofSeconds(5));
		httpProps.setResponseTimeout(Duration.ofSeconds(10));
		httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
		httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
		httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
		PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
				new SimpleMeterRegistry());
		OpenAiClientImpl client = new OpenAiClientImpl(properties, factory);
		OpenAiResponsesResponse response = Instancio.of(OpenAiResponsesResponse.class)
				.set(field("id"), null)
				.set(field("usage"), null)
				.set(field("output"), List.of(
						Instancio.of(OpenAiOutputItem.class)
								.set(field("type"), "message")
								.set(field("content"), null)
								.create()))
				.create();

		try {
			// When / Then:
			assertThatThrownBy(() -> client.extractResponsesText(response))
					.isInstanceOf(OpenAiExternalException.class)
					.hasMessageContaining("no text content");
		} finally {
			factory.destroy();
		}
	}

	@Test
	void shouldSkipPartsWithBlankTextWhenExtractingTextTest() throws IOException {
		// Given:
		OpenAiProperties properties = new OpenAiProperties();
		properties.setBaseUrl("http://localhost:12345");
		properties.setApiKey("sk-test-secret-key");
		properties.setModel("gpt-4o");
		PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
		httpProps.setConnectTimeout(Duration.ofSeconds(5));
		httpProps.setResponseTimeout(Duration.ofSeconds(10));
		httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
		httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
		httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
		PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
				new SimpleMeterRegistry());
		OpenAiClientImpl client = new OpenAiClientImpl(properties, factory);
		OpenAiResponsesResponse response = Instancio.of(OpenAiResponsesResponse.class)
				.set(field("id"), null)
				.set(field("usage"), null)
				.set(field("output"), List.of(
						Instancio.of(OpenAiOutputItem.class)
								.set(field("type"), "message")
								.set(field("content"), List.of(
										Instancio.of(OpenAiContentPart.class)
												.set(field("type"), "output_text")
												.set(field("text"), "  ")
												.create()))
								.create()))
				.create();

		try {
			// When / Then:
			assertThatThrownBy(() -> client.extractResponsesText(response))
					.isInstanceOf(OpenAiExternalException.class)
					.hasMessageContaining("no text content");
		} finally {
			factory.destroy();
		}
	}
}
