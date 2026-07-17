package com.aidigital.aionboarding.config;

import org.junit.jupiter.api.Test;
import org.zalando.logbook.Correlation;
import org.zalando.logbook.HttpHeaders;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.HttpResponse;
import org.zalando.logbook.Precorrelation;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Proves the bounded-metadata formatter never reads a request/response body — including a
 * representative large lesson/material payload — while still surfacing method, path, status,
 * duration, correlation id, and content length.
 */
class MetadataOnlyHttpLogFormatterTest {

	private final MetadataOnlyHttpLogFormatter formatter = new MetadataOnlyHttpLogFormatter();

	@Test
	void formatRequestExcludesLargeBodyButIncludesMetadataTest() throws Exception {
		// Given: a request whose body is a large representative JSON payload
		String largeBody = "{\"contentHtml\":\"" + "x".repeat(50_000) + "\"}";
		HttpRequest request = mock(HttpRequest.class);
		when(request.getProtocolVersion()).thenReturn("HTTP/1.1");
		when(request.getMethod()).thenReturn("POST");
		when(request.getPath()).thenReturn("/api/v1/lessons/42");
		when(request.getHeaders()).thenReturn(HttpHeaders.of("Content-Length", String.valueOf(largeBody.length())));
		when(request.getBodyAsString()).thenReturn(largeBody);
		Precorrelation precorrelation = mock(Precorrelation.class);
		when(precorrelation.getId()).thenReturn("corr-1");

		// When:
		String line = formatter.format(precorrelation, request);

		// Then: metadata is present, the large body content is not, and the body was never read
		assertThat(line).contains("\"correlation\":\"corr-1\"", "\"method\":\"POST\"",
				"\"path\":\"/api/v1/lessons/42\"", "\"contentLength\":\"" + largeBody.length() + "\"");
		assertThat(line).doesNotContain("contentHtml", "x".repeat(50_000));
		verify(request, never()).getBody();
		verify(request, never()).getBodyAsString();
	}

	@Test
	void formatResponseExcludesLargeBodyButIncludesMetadataTest() throws Exception {
		// Given: a response whose body is a large representative JSON payload
		String largeBody = "{\"contentHtml\":\"" + "y".repeat(50_000) + "\"}";
		HttpResponse response = mock(HttpResponse.class);
		when(response.getStatus()).thenReturn(200);
		when(response.getHeaders()).thenReturn(HttpHeaders.of("Content-Length", String.valueOf(largeBody.length())));
		when(response.getBodyAsString()).thenReturn(largeBody);
		Correlation correlation = mock(Correlation.class);
		when(correlation.getId()).thenReturn("corr-1");
		when(correlation.getDuration()).thenReturn(Duration.ofMillis(42));

		// When:
		String line = formatter.format(correlation, response);

		// Then: metadata is present, the large body content is not, and the body was never read
		assertThat(line).contains("\"correlation\":\"corr-1\"", "\"durationMs\":42", "\"status\":200",
				"\"contentLength\":\"" + largeBody.length() + "\"");
		assertThat(line).doesNotContain("contentHtml", "y".repeat(50_000));
		verify(response, never()).getBody();
		verify(response, never()).getBodyAsString();
	}
}
