package com.aidigital.aionboarding.external.common.http;

import com.aidigital.aionboarding.external.common.http.config.PooledHttpClientProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.hc.client5.http.SystemDefaultDnsResolver;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.zalando.logbook.Logbook;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that {@link PooledRestClientFactory} applies configured pool limits and
 * produces a functional {@link RestClient}.
 */
class PooledRestClientFactoryTest {

	private PooledHttpClientProperties defaultProperties() {
		PooledHttpClientProperties props = new PooledHttpClientProperties();
		props.setMaxTotalConnections(50);
		props.setMaxConnectionsPerRoute(10);
		props.setConnectTimeout(Duration.ofSeconds(5));
		props.setResponseTimeout(Duration.ofSeconds(30));
		props.setConnectionRequestTimeout(Duration.ofSeconds(5));
		props.setKeepAliveDuration(Duration.ofSeconds(60));
		props.setIdleEvictionDuration(Duration.ofSeconds(30));
		return props;
	}

	@Test
	void shouldReturnNonNullRestClientTest() {
		// Given:
		PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), defaultProperties(),
				new SimpleMeterRegistry());

		// When:
		RestClient client = factory.createClient("test", "https://example.com");

		// Then:
		assertThat(client).isNotNull();
	}

	@Test
	void shouldApplyMaxTotalConnectionsTest() {
		// Given:
		PooledHttpClientProperties props = defaultProperties();
		props.setMaxTotalConnections(77);
		PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), props,
				new SimpleMeterRegistry());

		// When:
		PoolingHttpClientConnectionManager cm = factory.buildConnectionManager();

		// Then:
		assertThat(cm.getMaxTotal()).isEqualTo(77);
	}

	@Test
	void shouldApplyMaxPerRouteConnectionsTest() {
		// Given:
		PooledHttpClientProperties props = defaultProperties();
		props.setMaxConnectionsPerRoute(13);
		PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), props,
				new SimpleMeterRegistry());

		// When:
		PoolingHttpClientConnectionManager cm = factory.buildConnectionManager();

		// Then:
		assertThat(cm.getDefaultMaxPerRoute()).isEqualTo(13);
	}

	@Test
	void shouldNotThrowWithCustomPoolSizeTest() {
		// Given:
		PooledHttpClientProperties props = defaultProperties();
		props.setMaxTotalConnections(5);
		props.setMaxConnectionsPerRoute(2);
		PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), props,
				new SimpleMeterRegistry());

		// When / Then:
		assertThat(factory.createClient("small-pool", "https://api.example.com")).isNotNull();
	}

	@Test
	void shouldNotThrowWithCustomTimeoutsTest() {
		// Given:
		PooledHttpClientProperties props = defaultProperties();
		props.setConnectTimeout(Duration.ofSeconds(1));
		props.setResponseTimeout(Duration.ofSeconds(10));
		props.setConnectionRequestTimeout(Duration.ofSeconds(2));
		PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), props,
				new SimpleMeterRegistry());

		// When / Then:
		assertThat(factory.createClient("fast-timeouts", "https://api.example.com")).isNotNull();
	}

	@Test
	void shouldReturnIndependentClientsTest() {
		// Given:
		PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), defaultProperties(),
				new SimpleMeterRegistry());

		// When:
		RestClient c1 = factory.createClient("svc-a", "https://a.example.com");
		RestClient c2 = factory.createClient("svc-b", "https://b.example.com");

		// Then:
		assertThat(c1).isNotSameAs(c2);
	}

	@Test
	void createDynamicHostClientShouldReturnNonNullRestClientTest() {
		// Given:
		PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), defaultProperties(),
				new SimpleMeterRegistry());

		// When:
		RestClient client = factory.createDynamicHostClient("link-fetch", new SystemDefaultDnsResolver());

		// Then:
		assertThat(client).isNotNull();
	}

	@Test
	void buildConnectionManagerWithDnsResolverShouldApplyConfiguredPoolLimitsTest() {
		// Given:
		PooledHttpClientProperties props = defaultProperties();
		props.setMaxTotalConnections(23);
		props.setMaxConnectionsPerRoute(4);
		PooledRestClientFactory factory = new PooledRestClientFactory(Logbook.builder().build(), props,
				new SimpleMeterRegistry());

		// When:
		PoolingHttpClientConnectionManager cm = factory.buildConnectionManager(new SystemDefaultDnsResolver());

		// Then:
		assertThat(cm.getMaxTotal()).isEqualTo(23);
		assertThat(cm.getDefaultMaxPerRoute()).isEqualTo(4);
	}
}