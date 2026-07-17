package com.aidigital.aionboarding.external.link.impl;

import com.aidigital.aionboarding.external.common.http.PooledRestClientFactory;
import com.aidigital.aionboarding.external.link.LinkFetchClient;
import com.aidigital.aionboarding.external.link.config.LinkFetchProperties;
import com.aidigital.aionboarding.external.link.model.LinkFetchAttempt;
import com.aidigital.aionboarding.external.link.model.LinkFetchFailureReason;
import com.aidigital.aionboarding.external.link.model.LinkFetchResult;
import com.aidigital.aionboarding.external.link.support.LinkFetchBlockedException;
import com.aidigital.aionboarding.external.link.support.LinkUrlPolicy;
import com.aidigital.aionboarding.external.link.support.PinnedDnsResolver;
import org.apache.hc.client5.http.DnsResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Production {@link LinkFetchClient}. Every URL — the caller-submitted one and each redirect
 * target — is validated for scheme/userinfo/port before being resolved, and its resolved
 * addresses are validated for public reachability by the pinned {@link DnsResolver} at the exact
 * moment the connection is made, closing the gap a DNS-rebinding attack would otherwise exploit.
 * Automatic redirect-following is disabled; this class follows redirects itself, up to a low cap,
 * re-running the full validation on every hop.
 */
public class LinkFetchClientImpl implements LinkFetchClient {

	private static final Logger LOG = LoggerFactory.getLogger(LinkFetchClientImpl.class);

	/**
	 * Caps how many bytes of a redirect (3xx) response body are drained before moving to the
	 * next hop; the body content of a redirect response is never used.
	 */
	private static final int MAX_REDIRECT_BODY_DRAIN_BYTES = 8192;

	private final LinkFetchProperties properties;
	private final RestClient restClient;
	private final LinkUrlPolicy urlPolicy;

	/**
	 * Constructs a client using the real DNS resolver and the real outbound-address policy.
	 *
	 * @param properties runtime-tunable link-fetch properties
	 * @param factory    pooled HTTP client factory
	 */
	public LinkFetchClientImpl(LinkFetchProperties properties, PooledRestClientFactory factory) {
		this(properties, factory, new PinnedDnsResolver(), new LinkUrlPolicy());
	}

	/**
	 * Constructs a client with injectable DNS resolution and URL policy, for tests that fake DNS
	 * answers or relax the address policy to exercise a local test server end to end.
	 *
	 * @param properties  runtime-tunable link-fetch properties
	 * @param factory     pooled HTTP client factory
	 * @param dnsResolver resolver applied to every connection this client makes
	 * @param urlPolicy   scheme/userinfo/port policy applied to every candidate URL
	 */
	public LinkFetchClientImpl(LinkFetchProperties properties, PooledRestClientFactory factory,
	                           DnsResolver dnsResolver, LinkUrlPolicy urlPolicy) {
		this.properties = properties;
		this.restClient = factory.createDynamicHostClient("link-fetch", dnsResolver);
		this.urlPolicy = urlPolicy;
	}

	@Override
	public LinkFetchResult fetch(String url) {
		URI current;
		try {
			current = new URI(url.trim()).normalize();
		} catch (URISyntaxException | NullPointerException ex) {
			return LinkFetchResult.failure("Invalid URL.");
		}

		for (int hop = 0; hop <= properties.getMaxRedirects(); hop++) {
			LinkFetchFailureReason blockReason = urlPolicy.validate(current);
			if (blockReason != null) {
				return LinkFetchResult.securityBlocked(blockReason, "Blocked by outbound link policy.");
			}

			LinkFetchAttempt attempt;
			try {
				attempt = executeOnce(current);
			} catch (Exception ex) {
				return toFailureResult(ex);
			}

			if (attempt.redirectTo() == null) {
				return attempt.result();
			}
			current = current.resolve(attempt.redirectTo());
		}
		return LinkFetchResult.securityBlocked(
				LinkFetchFailureReason.REDIRECT_LIMIT_EXCEEDED, "Too many redirects.");
	}

	/**
	 * Sends one HTTP request and classifies the response: a 3xx with a {@code Location} header
	 * becomes a redirect attempt to re-validate; a 2xx within the content-type and size policy
	 * becomes a success result; anything else becomes a terminal failure result.
	 *
	 * @param uri the validated URI to request
	 * @return the single-hop outcome
	 */
	LinkFetchAttempt executeOnce(URI uri) {
		return restClient.get()
				.uri(uri)
				.header("Accept", "text/html,application/xhtml+xml,text/plain;q=0.9,*/*;q=0.5")
				.header("User-Agent", "AI-Onboarding-Link-Parser/1.0")
				.exchange((request, response) -> {
					int status = response.getStatusCode().value();
					if (status >= 300 && status < 400) {
						URI location = response.getHeaders().getLocation();
						response.getBody().readNBytes(MAX_REDIRECT_BODY_DRAIN_BYTES);
						if (location == null) {
							return LinkFetchAttempt.result(LinkFetchResult.failure("Redirect with no Location header" +
									"."));
						}
						return LinkFetchAttempt.redirect(location);
					}
					if (status >= 400) {
						return LinkFetchAttempt.result(LinkFetchResult.failure("HTTP " + status));
					}
					return LinkFetchAttempt.result(readBoundedBody(response.getHeaders().getContentType(), response));
				});
	}

	/**
	 * Enforces the content-type allowlist and byte cap while reading a 2xx response body.
	 *
	 * @param contentType the response {@code Content-Type}, or {@code null} if absent
	 * @param response    the response to read the body from
	 * @return a success result, or a security-blocked result if the policy rejects this response
	 * @throws IOException if reading the response body fails
	 */
	LinkFetchResult readBoundedBody(MediaType contentType,
	                                RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response)
			throws IOException {
		String contentTypeValue = contentType == null ? "" : contentType.toString();
		if (!isAllowedContentType(contentType)) {
			response.getBody().readNBytes(MAX_REDIRECT_BODY_DRAIN_BYTES);
			throw new LinkFetchBlockedException(
					LinkFetchFailureReason.UNSUPPORTED_CONTENT_TYPE, "Unsupported content type: " + contentTypeValue);
		}

		long maxBytes = properties.getMaxResponseBytes();
		long declaredLength = response.getHeaders().getContentLength();
		if (declaredLength > maxBytes) {
			throw new LinkFetchBlockedException(
					LinkFetchFailureReason.RESPONSE_TOO_LARGE, "Declared response size exceeds the allowed limit.");
		}

		byte[] bytes = response.getBody().readNBytes((int) Math.min(maxBytes + 1, Integer.MAX_VALUE));
		if (bytes.length > maxBytes) {
			throw new LinkFetchBlockedException(
					LinkFetchFailureReason.RESPONSE_TOO_LARGE, "Response body exceeds the allowed limit.");
		}

		Charset charset = contentType != null && contentType.getCharset() != null
				? contentType.getCharset()
				: StandardCharsets.UTF_8;
		return LinkFetchResult.success(new String(bytes, charset), contentTypeValue);
	}

	/**
	 * Checks a response's content type against the configured allowlist by prefix, ignoring
	 * parameters such as {@code charset}.
	 *
	 * @param contentType the response {@code Content-Type}, or {@code null} if absent
	 * @return {@code true} if this content type may be processed
	 */
	boolean isAllowedContentType(MediaType contentType) {
		if (contentType == null) {
			return false;
		}
		String typeAndSubtype = contentType.getType() + "/" + contentType.getSubtype();
		List<String> allowed = properties.getAllowedContentTypePrefixes();
		for (String prefix : allowed) {
			if (typeAndSubtype.equalsIgnoreCase(prefix)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Translates an exception raised while executing one hop into the appropriate
	 * {@link LinkFetchResult}, distinguishing outbound-security policy blocks from ordinary
	 * network failures.
	 *
	 * @param ex the exception raised while executing one hop
	 * @return the corresponding failure result
	 */
	LinkFetchResult toFailureResult(Exception ex) {
		LinkFetchBlockedException blocked = findBlockedException(ex);
		if (blocked != null) {
			LOG.warn("Link fetch blocked by outbound-security policy: reason={}", blocked.reason().value());
			return LinkFetchResult.securityBlocked(blocked.reason(), blocked.getMessage());
		}
		if (isTimeout(ex)) {
			return LinkFetchResult.securityBlocked(LinkFetchFailureReason.TIMEOUT, "Request timed out.");
		}
		LOG.debug("Link fetch failed: {}", ex.getMessage());
		return LinkFetchResult.failure(ex.getMessage() == null ? "Failed to fetch link." : ex.getMessage());
	}

	/**
	 * Walks an exception's cause chain looking for a {@link LinkFetchBlockedException}, since
	 * intermediate HTTP client layers may wrap the resolver's exception before it reaches here.
	 *
	 * @param ex the exception to inspect
	 * @return the blocked exception found in the chain, or {@code null} if none
	 */
	LinkFetchBlockedException findBlockedException(Throwable ex) {
		Throwable current = ex;
		for (int depth = 0; current != null && depth < 5; depth++) {
			if (current instanceof LinkFetchBlockedException blocked) {
				return blocked;
			}
			current = current.getCause();
		}
		return null;
	}

	/**
	 * Determines whether an exception represents a client-enforced timeout.
	 *
	 * @param ex the exception to inspect
	 * @return {@code true} if this is a timeout
	 */
	boolean isTimeout(Throwable ex) {
		Throwable current = ex;
		for (int depth = 0; current != null && depth < 5; depth++) {
			if (current instanceof InterruptedIOException || current instanceof UncheckedIOException) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}
}
