package com.aidigital.aionboarding.external.common.http;

import com.aidigital.aionboarding.external.common.http.config.PooledHttpClientProperties;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.zalando.logbook.Logbook;
import org.zalando.logbook.spring.LogbookClientHttpRequestInterceptor;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory that creates {@link RestClient} instances backed by dedicated
 * Apache HttpClient 5 pools. All created clients are closed on container shutdown.
 */
@Component
@RequiredArgsConstructor
public class PooledRestClientFactory {

	private final Logbook logbook;
	private final PooledHttpClientProperties properties;
	private final MeterRegistry meterRegistry;
	private final List<ManagedPooledRestClient> managedClients = new ArrayList<>();

	/**
	 * Creates a {@link RestClient} for the given service, backed by a managed pool.
	 *
	 * @param name    logical service name (used for diagnostics only)
	 * @param baseUrl base URL for all requests made by this client
	 * @return a fully configured {@code RestClient}
	 */
	public synchronized RestClient createClient(String name, String baseUrl) {
		PoolingHttpClientConnectionManager connectionManager = buildConnectionManager();

		RequestConfig requestConfig = RequestConfig.custom()
				.setConnectTimeout(Timeout.ofMilliseconds(properties.getConnectTimeout().toMillis()))
				.setResponseTimeout(Timeout.ofMilliseconds(properties.getResponseTimeout().toMillis()))
				.setConnectionRequestTimeout(Timeout.ofMilliseconds(
						properties.getConnectionRequestTimeout().toMillis()))
				.build();

		CloseableHttpClient httpClient = HttpClients.custom()
				.setConnectionManager(connectionManager)
				.setDefaultRequestConfig(requestConfig)
				.evictIdleConnections(TimeValue.ofMilliseconds(
						properties.getIdleEvictionDuration().toMillis()))
				.build();

		HttpComponentsClientHttpRequestFactory requestFactory =
				new HttpComponentsClientHttpRequestFactory(httpClient);

		RestClient restClient = RestClient.builder()
				.baseUrl(baseUrl)
				.requestFactory(requestFactory)
				.requestInterceptor(new ExternalClientMetricsInterceptor(name, meterRegistry))
				.requestInterceptor(new LogbookClientHttpRequestInterceptor(logbook))
				.build();

		ManagedPooledRestClient managed =
				new ManagedPooledRestClient(name, restClient, httpClient, connectionManager);
		managedClients.add(managed);
		return restClient;
	}

	/**
	 * Creates a {@link RestClient} with no fixed base URL, for adapters that connect to a
	 * caller-supplied target per request (e.g. an SSRF-resistant link fetcher) rather than one
	 * fixed upstream host. Automatic redirect-following is disabled so the caller can validate
	 * and re-issue every redirect hop itself. Callers must supply an absolute {@code URI} on
	 * every request.
	 *
	 * @param name        logical service name (used for diagnostics only)
	 * @param dnsResolver resolver invoked for every connection this client makes, including every
	 *                    manually re-issued redirect hop
	 * @return a fully configured {@code RestClient} with redirects disabled
	 */
	public synchronized RestClient createDynamicHostClient(String name, DnsResolver dnsResolver) {
		PoolingHttpClientConnectionManager connectionManager = buildConnectionManager(dnsResolver);

		RequestConfig requestConfig = RequestConfig.custom()
				.setConnectTimeout(Timeout.ofMilliseconds(properties.getConnectTimeout().toMillis()))
				.setResponseTimeout(Timeout.ofMilliseconds(properties.getResponseTimeout().toMillis()))
				.setConnectionRequestTimeout(Timeout.ofMilliseconds(
						properties.getConnectionRequestTimeout().toMillis()))
				.setRedirectsEnabled(false)
				.build();

		CloseableHttpClient httpClient = HttpClients.custom()
				.setConnectionManager(connectionManager)
				.setDefaultRequestConfig(requestConfig)
				.evictIdleConnections(TimeValue.ofMilliseconds(
						properties.getIdleEvictionDuration().toMillis()))
				.build();

		HttpComponentsClientHttpRequestFactory requestFactory =
				new HttpComponentsClientHttpRequestFactory(httpClient);

		RestClient restClient = RestClient.builder()
				.requestFactory(requestFactory)
				.requestInterceptor(new ExternalClientMetricsInterceptor(name, meterRegistry))
				.requestInterceptor(new LogbookClientHttpRequestInterceptor(logbook))
				.build();

		ManagedPooledRestClient managed =
				new ManagedPooledRestClient(name, restClient, httpClient, connectionManager);
		managedClients.add(managed);
		return restClient;
	}

	/**
	 * Builds the connection manager with pool limits and keep-alive from properties.
	 *
	 * @return configured {@link PoolingHttpClientConnectionManager}
	 */
	public PoolingHttpClientConnectionManager buildConnectionManager() {
		PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
		connectionManager.setMaxTotal(properties.getMaxTotalConnections());
		connectionManager.setDefaultMaxPerRoute(properties.getMaxConnectionsPerRoute());
		ConnectionConfig connectionConfig = ConnectionConfig.custom()
				.setTimeToLive(TimeValue.ofMilliseconds(properties.getKeepAliveDuration().toMillis()))
				.setValidateAfterInactivity(TimeValue.ofMilliseconds(
						properties.getIdleEvictionDuration().toMillis()))
				.build();
		connectionManager.setDefaultConnectionConfig(connectionConfig);
		return connectionManager;
	}

	/**
	 * Builds a connection manager with the same pool limits and keep-alive as
	 * {@link #buildConnectionManager()}, but pinned to a custom {@link DnsResolver} so every
	 * connection this pool makes resolves addresses through it.
	 *
	 * @param dnsResolver the resolver to pin every connection in this pool to
	 * @return configured {@link PoolingHttpClientConnectionManager}
	 */
	public PoolingHttpClientConnectionManager buildConnectionManager(DnsResolver dnsResolver) {
		ConnectionConfig connectionConfig = ConnectionConfig.custom()
				.setTimeToLive(TimeValue.ofMilliseconds(properties.getKeepAliveDuration().toMillis()))
				.setValidateAfterInactivity(TimeValue.ofMilliseconds(
						properties.getIdleEvictionDuration().toMillis()))
				.build();
		return PoolingHttpClientConnectionManagerBuilder.create()
				.setDnsResolver(dnsResolver)
				.setMaxConnTotal(properties.getMaxTotalConnections())
				.setMaxConnPerRoute(properties.getMaxConnectionsPerRoute())
				.setDefaultConnectionConfig(connectionConfig)
				.build();
	}

	/**
	 * Closes every pooled client created by this factory.
	 */
	@PreDestroy
	public synchronized void destroy() {
		for (ManagedPooledRestClient managed : managedClients) {
			managed.close();
		}
		managedClients.clear();
	}
}