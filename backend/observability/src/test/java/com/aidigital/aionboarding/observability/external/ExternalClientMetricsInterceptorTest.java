package com.aidigital.aionboarding.observability.external;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies that {@link ExternalClientMetricsInterceptor} records the
 * {@code external.client.requests} timer tagged by the fixed logical {@code client} name and a
 * coarse {@code outcome}, and that the downstream response/exception is passed through
 * unmodified.
 */
class ExternalClientMetricsInterceptorTest {

	@Test
	void interceptShouldTagSuccessOutcomeForATwoHundredResponseTest() throws IOException {
		// Given:
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		ExternalClientMetricsInterceptor interceptor = new ExternalClientMetricsInterceptor("openai", registry);
		HttpRequest request = mock(HttpRequest.class);
		byte[] body = new byte[0];
		ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
		ClientHttpResponse response = mock(ClientHttpResponse.class);
		when(response.getStatusCode()).thenReturn(HttpStatus.OK);
		when(execution.execute(request, body)).thenReturn(response);

		// When:
		ClientHttpResponse result = interceptor.intercept(request, body, execution);

		// Then:
		assertThat(result).isSameAs(response);
		assertThat(registry.get("external.client.requests")
				.tag("client", "openai")
				.tag("outcome", "success")
				.timer().count()).isEqualTo(1L);
	}

	@Test
	void interceptShouldTagClientErrorOutcomeForAFourHundredResponseTest() throws IOException {
		// Given:
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		ExternalClientMetricsInterceptor interceptor = new ExternalClientMetricsInterceptor("heygen", registry);
		HttpRequest request = mock(HttpRequest.class);
		byte[] body = new byte[0];
		ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
		ClientHttpResponse response = mock(ClientHttpResponse.class);
		when(response.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);
		when(execution.execute(request, body)).thenReturn(response);

		// When:
		interceptor.intercept(request, body, execution);

		// Then:
		assertThat(registry.get("external.client.requests")
				.tag("client", "heygen")
				.tag("outcome", "client_error")
				.timer().count()).isEqualTo(1L);
	}

	@Test
	void interceptShouldTagServerErrorOutcomeForAFiveHundredResponseTest() throws IOException {
		// Given:
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		ExternalClientMetricsInterceptor interceptor = new ExternalClientMetricsInterceptor("youtube", registry);
		HttpRequest request = mock(HttpRequest.class);
		byte[] body = new byte[0];
		ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
		ClientHttpResponse response = mock(ClientHttpResponse.class);
		when(response.getStatusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
		when(execution.execute(request, body)).thenReturn(response);

		// When:
		interceptor.intercept(request, body, execution);

		// Then:
		assertThat(registry.get("external.client.requests")
				.tag("client", "youtube")
				.tag("outcome", "server_error")
				.timer().count()).isEqualTo(1L);
	}

	@Test
	void interceptShouldTagIoErrorOutcomeAndRethrowWhenExecutionThrowsTest() throws IOException {
		// Given:
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		ExternalClientMetricsInterceptor interceptor = new ExternalClientMetricsInterceptor("openai", registry);
		HttpRequest request = mock(HttpRequest.class);
		byte[] body = new byte[0];
		ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
		IOException failure = new IOException("connection reset");
		when(execution.execute(request, body)).thenThrow(failure);

		// When-Then:
		assertThatThrownBy(() -> interceptor.intercept(request, body, execution)).isSameAs(failure);

		assertThat(registry.get("external.client.requests")
				.tag("client", "openai")
				.tag("outcome", "io_error")
				.timer().count()).isEqualTo(1L);
	}

	@Test
	void classifyOutcomeShouldReturnSuccessForATwoHundredStatusTest() {
		// Given:
		ExternalClientMetricsInterceptor interceptor =
				new ExternalClientMetricsInterceptor("openai", new SimpleMeterRegistry());

		// When:
		String outcome = interceptor.classifyOutcome(HttpStatus.CREATED);

		// Then:
		assertThat(outcome).isEqualTo("success");
	}

	@Test
	void classifyOutcomeShouldReturnClientErrorForAFourHundredStatusTest() {
		// Given:
		ExternalClientMetricsInterceptor interceptor =
				new ExternalClientMetricsInterceptor("openai", new SimpleMeterRegistry());

		// When:
		String outcome = interceptor.classifyOutcome(HttpStatus.NOT_FOUND);

		// Then:
		assertThat(outcome).isEqualTo("client_error");
	}

	@Test
	void classifyOutcomeShouldReturnServerErrorForAFiveHundredStatusTest() {
		// Given:
		ExternalClientMetricsInterceptor interceptor =
				new ExternalClientMetricsInterceptor("openai", new SimpleMeterRegistry());

		// When:
		String outcome = interceptor.classifyOutcome(HttpStatus.SERVICE_UNAVAILABLE);

		// Then:
		assertThat(outcome).isEqualTo("server_error");
	}
}
