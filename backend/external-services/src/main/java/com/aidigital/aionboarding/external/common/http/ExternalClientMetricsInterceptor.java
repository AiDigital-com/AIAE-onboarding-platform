package com.aidigital.aionboarding.external.common.http;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * Times every call made through a pooled {@link org.springframework.web.client.RestClient},
 * recording an {@code external.client.requests} timer tagged by the fixed logical client name
 * (e.g. {@code openai}, {@code heygen}, {@code youtube} — never a request path or query value)
 * and a coarse outcome so slow/failing downstream providers are visible in Prometheus without
 * per-request log inspection.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class ExternalClientMetricsInterceptor implements ClientHttpRequestInterceptor {

	private final String clientName;
	private final MeterRegistry meterRegistry;

	/**
	 * Wraps the downstream call with a timer, tagging the outcome as {@code success},
	 * {@code client_error}, {@code server_error}, or {@code io_error}.
	 *
	 * @param request   outbound request
	 * @param body      outbound request body
	 * @param execution remaining interceptor/execution chain
	 * @return the downstream response, unmodified
	 * @throws IOException propagated from the downstream execution
	 */
	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
			throws IOException {
		Timer.Sample sample = Timer.start(meterRegistry);
		String outcome = "io_error";
		try {
			ClientHttpResponse response = execution.execute(request, body);
			outcome = classifyOutcome(response.getStatusCode());
			return response;
		} finally {
			sample.stop(Timer.builder("external.client.requests")
					.tag("client", clientName)
					.tag("outcome", outcome)
					.register(meterRegistry));
		}
	}

	/**
	 * Buckets an HTTP status into a coarse, low-cardinality outcome label.
	 *
	 * @param status response status
	 * @return one of {@code success}, {@code client_error}, {@code server_error}
	 */
	String classifyOutcome(HttpStatusCode status) {
		if (status.is5xxServerError()) {
			return "server_error";
		}
		if (status.is4xxClientError()) {
			return "client_error";
		}
		return "success";
	}
}
