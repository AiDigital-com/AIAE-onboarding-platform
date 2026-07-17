package com.aidigital.aionboarding.external.link.impl;

import com.aidigital.aionboarding.external.common.http.PooledRestClientFactory;
import com.aidigital.aionboarding.external.common.http.config.PooledHttpClientProperties;
import com.aidigital.aionboarding.external.link.config.LinkFetchProperties;
import com.aidigital.aionboarding.external.link.model.LinkFetchFailureReason;
import com.aidigital.aionboarding.external.link.model.LinkFetchResult;
import com.aidigital.aionboarding.external.link.support.HostnameResolver;
import com.aidigital.aionboarding.external.link.support.LinkFetchBlockedException;
import com.aidigital.aionboarding.external.link.support.LinkUrlPolicy;
import com.aidigital.aionboarding.external.link.support.OutboundAddressValidator;
import com.aidigital.aionboarding.external.link.support.PinnedDnsResolver;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.hc.client5.http.DnsResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.zalando.logbook.Logbook;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.URI;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises {@link LinkFetchClientImpl} end to end against a real local HTTP server. The DNS
 * resolver and URL policy are relaxed via their public test seams so this suite can prove
 * redirect-following, content-type, and byte-cap behavior without depending on real DNS or the
 * public internet; scheme/userinfo/port and address-classification policy are covered exhaustively
 * by {@code LinkUrlPolicyTest} and {@code OutboundAddressValidatorTest}.
 */
class LinkFetchClientImplTest {

	private MockWebServer server;
	private PooledRestClientFactory factory;
	private LinkFetchClientImpl client;

	@BeforeEach
	void setUp() throws IOException {
		server = new MockWebServer();
		server.start();

		LinkFetchProperties properties = new LinkFetchProperties();
		properties.setMaxRedirects(3);
		properties.setMaxResponseBytes(1000);
		properties.setAllowedContentTypePrefixes(List.of("text/html", "text/plain"));

		PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
		httpProps.setConnectTimeout(Duration.ofSeconds(5));
		httpProps.setResponseTimeout(Duration.ofSeconds(10));
		httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
		httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
		httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
		factory = new PooledRestClientFactory(Logbook.builder().build(), httpProps, new SimpleMeterRegistry());

		HostnameResolver alwaysLocalhost = host -> new InetAddress[]{InetAddress.getByName("127.0.0.1")};
		OutboundAddressValidator permissiveValidator = new OutboundAddressValidator() {
			@Override
			public boolean allPublic(InetAddress[] addresses) {
				return true;
			}
		};
		DnsResolver dnsResolver = new PinnedDnsResolver(alwaysLocalhost, permissiveValidator);
		LinkUrlPolicy permissiveUrlPolicy = new LinkUrlPolicy() {
			@Override
			public LinkFetchFailureReason validate(URI uri) {
				return null;
			}
		};
		client = new LinkFetchClientImpl(properties, factory, dnsResolver, permissiveUrlPolicy);
	}

	@AfterEach
	void tearDown() throws IOException {
		factory.destroy();
		server.shutdown();
	}

	private String baseUrl() {
		return "http://127.0.0.1:" + server.getPort();
	}

	@Test
	void fetchShouldReturnSuccessResultForAnOrdinaryHtmlResponseTest() {
		// Given:
		server.enqueue(new MockResponse().setResponseCode(200)
				.setHeader("Content-Type", "text/html").setBody("<p>hi</p>"));

		// When:
		LinkFetchResult result = client.fetch(baseUrl() + "/page");

		// Then:
		assertThat(result.success()).isTrue();
		assertThat(result.body()).isEqualTo("<p>hi</p>");
		assertThat(result.contentType()).contains("text/html");
		assertThat(result.securityBlockReason()).isNull();
	}

	@Test
	void fetchShouldFollowARedirectAndReturnTheFinalResponseTest() throws InterruptedException {
		// Given:
		server.enqueue(new MockResponse().setResponseCode(302).setHeader("Location", "/final"));
		server.enqueue(new MockResponse().setResponseCode(200)
				.setHeader("Content-Type", "text/html").setBody("<p>final</p>"));

		// When:
		LinkFetchResult result = client.fetch(baseUrl() + "/start");

		// Then:
		assertThat(result.success()).isTrue();
		assertThat(result.body()).isEqualTo("<p>final</p>");
		assertThat(server.getRequestCount()).isEqualTo(2);
		assertThat(server.takeRequest().getPath()).isEqualTo("/start");
		assertThat(server.takeRequest().getPath()).isEqualTo("/final");
	}

	@Test
	void fetchShouldReturnRedirectLimitExceededWhenRedirectsNeverTerminateTest() {
		// Given: maxRedirects=3, but the server always redirects back to the same path.
		for (int i = 0; i <= 5; i++) {
			server.enqueue(new MockResponse().setResponseCode(302).setHeader("Location", "/loop"));
		}

		// When:
		LinkFetchResult result = client.fetch(baseUrl() + "/loop");

		// Then:
		assertThat(result.success()).isFalse();
		assertThat(result.securityBlockReason()).isEqualTo(LinkFetchFailureReason.REDIRECT_LIMIT_EXCEEDED);
	}

	@Test
	void fetchShouldRejectAResponseWithADisallowedContentTypeTest() {
		// Given:
		server.enqueue(new MockResponse().setResponseCode(200)
				.setHeader("Content-Type", "application/octet-stream").setBody("binary"));

		// When:
		LinkFetchResult result = client.fetch(baseUrl() + "/file.bin");

		// Then:
		assertThat(result.success()).isFalse();
		assertThat(result.securityBlockReason()).isEqualTo(LinkFetchFailureReason.UNSUPPORTED_CONTENT_TYPE);
	}

	@Test
	void fetchShouldRejectAResponseThatExceedsTheByteCapViaDeclaredContentLengthTest() {
		// Given: maxResponseBytes=1000 and this response declares a larger Content-Length.
		String hugeBody = "x".repeat(2000);
		server.enqueue(new MockResponse().setResponseCode(200)
				.setHeader("Content-Type", "text/plain").setBody(hugeBody));

		// When:
		LinkFetchResult result = client.fetch(baseUrl() + "/huge");

		// Then:
		assertThat(result.success()).isFalse();
		assertThat(result.securityBlockReason()).isEqualTo(LinkFetchFailureReason.RESPONSE_TOO_LARGE);
	}

	@Test
	void fetchShouldRejectAResponseThatExceedsTheByteCapWhenContentLengthIsAbsentTest() {
		// Given: chunked transfer-encoding omits Content-Length, forcing the actual-byte-count cap.
		String hugeBody = "y".repeat(2000);
		server.enqueue(new MockResponse().setResponseCode(200)
				.setHeader("Content-Type", "text/plain").setChunkedBody(hugeBody, 256));

		// When:
		LinkFetchResult result = client.fetch(baseUrl() + "/huge-chunked");

		// Then:
		assertThat(result.success()).isFalse();
		assertThat(result.securityBlockReason()).isEqualTo(LinkFetchFailureReason.RESPONSE_TOO_LARGE);
	}

	@Test
	void fetchShouldReturnAnOrdinaryFailureForANon2xxNon3xxStatusTest() {
		// Given:
		server.enqueue(new MockResponse().setResponseCode(404));

		// When:
		LinkFetchResult result = client.fetch(baseUrl() + "/missing");

		// Then:
		assertThat(result.success()).isFalse();
		assertThat(result.securityBlockReason()).isNull();
		assertThat(result.errorMessage()).contains("404");
	}

	@Test
	void fetchShouldReturnAnOrdinaryFailureForAnUnparseableUrlTest() {
		// When:
		LinkFetchResult result = client.fetch("not a url with spaces");

		// Then:
		assertThat(result.success()).isFalse();
		assertThat(result.securityBlockReason()).isNull();
	}

	@Test
	void fetchShouldReturnFailureForNullUrlTest() {
		// When:
		LinkFetchResult result = client.fetch(null);

		// Then:
		assertThat(result.success()).isFalse();
		assertThat(result.errorMessage()).isEqualTo("Invalid URL.");
	}

	@Test
	void fetchShouldHandleRedirectWithoutLocationTest() {
		// Given:
		server.enqueue(new MockResponse().setResponseCode(302));

		// When:
		LinkFetchResult result = client.fetch(baseUrl() + "/redirect-nowhere");

		// Then:
		assertThat(result.success()).isFalse();
		assertThat(result.errorMessage()).contains("Redirect with no Location header");
	}

	@Test
	void fetchShouldResolveRelativeRedirectLocationTest() throws InterruptedException {
		// Given:
		server.enqueue(new MockResponse().setResponseCode(302).setHeader("Location", "/relative"));
		server.enqueue(new MockResponse().setResponseCode(200)
				.setHeader("Content-Type", "text/html").setBody("<p>relative</p>"));

		// When:
		LinkFetchResult result = client.fetch(baseUrl() + "/start");

		// Then:
		assertThat(result.success()).isTrue();
		assertThat(result.body()).isEqualTo("<p>relative</p>");
		assertThat(server.getRequestCount()).isEqualTo(2);
		assertThat(server.takeRequest().getPath()).isEqualTo("/start");
		assertThat(server.takeRequest().getPath()).isEqualTo("/relative");
	}

	@Test
	void fetchShouldUseResponseCharsetWhenPresentTest() {
		// Given:
		server.enqueue(new MockResponse().setResponseCode(200)
				.setHeader("Content-Type", "text/html; charset=UTF-8").setBody("<p>charset</p>"));

		// When:
		LinkFetchResult result = client.fetch(baseUrl() + "/charset");

		// Then:
		assertThat(result.success()).isTrue();
		assertThat(result.body()).isEqualTo("<p>charset</p>");
		assertThat(result.contentType()).contains("text/html");
	}

	@Test
	void fetchShouldBlockWhenContentTypeIsMissingTest() {
		// Given:
		server.enqueue(new MockResponse().setResponseCode(200).setBody("no content type"));

		// When:
		LinkFetchResult result = client.fetch(baseUrl() + "/no-type");

		// Then:
		assertThat(result.success()).isFalse();
		assertThat(result.securityBlockReason()).isEqualTo(LinkFetchFailureReason.UNSUPPORTED_CONTENT_TYPE);
	}

	@Test
	void fetchShouldBlockWhenUrlPolicyRejectsTest() {
		// Given:
		LinkFetchProperties properties = new LinkFetchProperties();
		properties.setMaxRedirects(3);
		properties.setMaxResponseBytes(1000);
		properties.setAllowedContentTypePrefixes(List.of("text/html"));
		PooledHttpClientProperties httpProps = new PooledHttpClientProperties();
		httpProps.setConnectTimeout(Duration.ofSeconds(5));
		httpProps.setResponseTimeout(Duration.ofSeconds(10));
		httpProps.setConnectionRequestTimeout(Duration.ofSeconds(5));
		httpProps.setKeepAliveDuration(Duration.ofSeconds(60));
		httpProps.setIdleEvictionDuration(Duration.ofSeconds(30));
		PooledRestClientFactory localFactory = new PooledRestClientFactory(Logbook.builder().build(), httpProps,
				new SimpleMeterRegistry());
		HostnameResolver localhost = host -> new InetAddress[]{InetAddress.getByName("127.0.0.1")};
		OutboundAddressValidator permissive = new OutboundAddressValidator() {
			@Override
			public boolean allPublic(InetAddress[] addresses) {
				return true;
			}
		};
		DnsResolver dnsResolver = new PinnedDnsResolver(localhost, permissive);
		LinkUrlPolicy blockingPolicy = new LinkUrlPolicy() {
			@Override
			public LinkFetchFailureReason validate(URI uri) {
				return LinkFetchFailureReason.DISALLOWED_SCHEME;
			}
		};
		LinkFetchClientImpl blockingClient = new LinkFetchClientImpl(properties, localFactory, dnsResolver,
				blockingPolicy);

		try {
			// When:
			LinkFetchResult result = blockingClient.fetch("http://example.com/page");

			// Then:
			assertThat(result.success()).isFalse();
			assertThat(result.securityBlockReason()).isEqualTo(LinkFetchFailureReason.DISALLOWED_SCHEME);
			assertThat(result.errorMessage()).contains("Blocked by outbound link policy");
		} finally {
			localFactory.destroy();
		}
	}

	@Test
	void shouldRejectNullContentTypeTest() {
		// When:
		boolean allowed = client.isAllowedContentType(null);

		// Then:
		assertThat(allowed).isFalse();
	}

	@Test
	void shouldAllowExactContentTypeTest() {
		// When:
		boolean allowed = client.isAllowedContentType(MediaType.parseMediaType("text/html"));

		// Then:
		assertThat(allowed).isTrue();
	}

	@Test
	void shouldMatchAllowedContentTypeCaseInsensitivelyTest() {
		// When:
		boolean allowed = client.isAllowedContentType(MediaType.parseMediaType("TEXT/HTML"));

		// Then:
		assertThat(allowed).isTrue();
	}

	@Nested
	class ExceptionHandling {

		@Test
		void shouldReturnSecurityBlockedForBlockedExceptionTest() {
			// Given:
			LinkFetchBlockedException blocked = new LinkFetchBlockedException(
					LinkFetchFailureReason.PRIVATE_ADDRESS, "private address");

			// When:
			LinkFetchResult result = client.toFailureResult(blocked);

			// Then:
			assertThat(result.success()).isFalse();
			assertThat(result.securityBlockReason()).isEqualTo(LinkFetchFailureReason.PRIVATE_ADDRESS);
			assertThat(result.errorMessage()).isEqualTo("private address");
		}

		@Test
		void shouldReturnTimeoutForInterruptedIOExceptionTest() {
			// Given:
			Exception ex = new RuntimeException(new InterruptedIOException("timed out"));

			// When:
			LinkFetchResult result = client.toFailureResult(ex);

			// Then:
			assertThat(result.success()).isFalse();
			assertThat(result.securityBlockReason()).isEqualTo(LinkFetchFailureReason.TIMEOUT);
		}

		@Test
		void shouldReturnFailureForOtherExceptionTest() {
			// Given:
			Exception ex = new RuntimeException("boom");

			// When:
			LinkFetchResult result = client.toFailureResult(ex);

			// Then:
			assertThat(result.success()).isFalse();
			assertThat(result.errorMessage()).isEqualTo("boom");
			assertThat(result.securityBlockReason()).isNull();
		}

		@Test
		void shouldReturnFallbackFailureWhenExceptionMessageIsNullTest() {
			// Given:
			Exception ex = new RuntimeException();

			// When:
			LinkFetchResult result = client.toFailureResult(ex);

			// Then:
			assertThat(result.success()).isFalse();
			assertThat(result.errorMessage()).isEqualTo("Failed to fetch link.");
		}

		@Test
		void shouldFindBlockedExceptionWithinCauseChainTest() {
			// Given:
			LinkFetchBlockedException blocked = new LinkFetchBlockedException(
					LinkFetchFailureReason.PRIVATE_ADDRESS, "private");
			Exception ex = new RuntimeException(new RuntimeException(blocked));

			// When:
			LinkFetchBlockedException found = client.findBlockedException(ex);

			// Then:
			assertThat(found).isSameAs(blocked);
		}

		@Test
		void shouldNotFindBlockedExceptionWhenTooDeepTest() {
			// Given:
			LinkFetchBlockedException blocked = new LinkFetchBlockedException(
					LinkFetchFailureReason.PRIVATE_ADDRESS, "private");
			Exception current = blocked;
			for (int i = 0; i < 6; i++) {
				current = new RuntimeException(current);
			}

			// When:
			LinkFetchBlockedException found = client.findBlockedException(current);

			// Then:
			assertThat(found).isNull();
		}

		@Test
		void shouldDetectInterruptedIOExceptionTimeoutTest() {
			// When:
			boolean timeout = client.isTimeout(new InterruptedIOException("timed out"));

			// Then:
			assertThat(timeout).isTrue();
		}

		@Test
		void shouldDetectUncheckedIOExceptionTimeoutTest() {
			// When:
			boolean timeout = client.isTimeout(new UncheckedIOException(new IOException("timed out")));

			// Then:
			assertThat(timeout).isTrue();
		}

		@Test
		void shouldDetectTimeoutWithinCauseChainTest() {
			// When:
			boolean timeout = client.isTimeout(new RuntimeException(new InterruptedIOException()));

			// Then:
			assertThat(timeout).isTrue();
		}

		@Test
		void shouldNotDetectTimeoutForOtherExceptionTest() {
			// When:
			boolean timeout = client.isTimeout(new RuntimeException("boom"));

			// Then:
			assertThat(timeout).isFalse();
		}
	}
}
