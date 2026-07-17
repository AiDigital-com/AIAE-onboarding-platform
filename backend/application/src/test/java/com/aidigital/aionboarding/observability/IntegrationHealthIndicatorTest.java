package com.aidigital.aionboarding.observability;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.core.env.Environment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IntegrationHealthIndicatorTest {

	@Mock
	private Environment environment;

	@Test
	void shouldReportUpWithResolvedDetailsTest() {
		// Given:
		IntegrationHealthIndicator indicator = new IntegrationHealthIndicator(environment);
		// health() unconditionally resolves every fallback property name in firstNonBlank(...)'s
		// varargs, even once an earlier candidate already resolved; stub a catch-all default first
		// so strict stubbing doesn't flag those as mismatches.
		when(environment.getProperty(anyString())).thenReturn(null);
		when(environment.getProperty("spring.profiles.active")).thenReturn("test");
		when(environment.getProperty("server.port")).thenReturn("8080");
		when(environment.getProperty("HOSTNAME")).thenReturn("test-host");
		when(environment.getProperty("DATABASE_URL")).thenReturn("postgres://localhost");
		when(environment.getProperty("OPENAI_API_KEY")).thenReturn("sk-test");
		when(environment.getProperty("BUCKET")).thenReturn("test-bucket");
		when(environment.getProperty("ENDPOINT")).thenReturn("http://s3");
		when(environment.getProperty("ACCESS_KEY_ID")).thenReturn("key");
		when(environment.getProperty("SECRET_ACCESS_KEY")).thenReturn("secret");

		// When:
		Health health = indicator.health();

		// Then:
		assertThat(health.getStatus()).isEqualTo(org.springframework.boot.actuate.health.Status.UP);
		Map<String, Object> details = health.getDetails();
		assertThat(details.get("ok")).isEqualTo(true);
		assertThat(details.get("service")).isEqualTo("aionboarding");
		assertThat(details.get("nodeEnv")).isEqualTo("test");
		assertThat(details.get("port")).isEqualTo("8080");
		assertThat(details.get("hostname")).isEqualTo("test-host");
		assertThat(details.get("hasDatabaseUrl")).isEqualTo(true);
		assertThat(details.get("hasPgHost")).isEqualTo(false);
		assertThat(details.get("hasOpenAiKey")).isEqualTo(true);
		assertThat(details.get("hasBucket")).isEqualTo(true);
		assertThat(details.get("hasBucketEndpoint")).isEqualTo(true);
		assertThat(details.get("hasBucketAccessKeyId")).isEqualTo(true);
		assertThat(details.get("hasBucketSecretAccessKey")).isEqualTo(true);
	}

	@Test
	void shouldFallbackToEnvironmentVariablesAndDefaultsTest() {
		// Given:
		IntegrationHealthIndicator indicator = new IntegrationHealthIndicator(environment);
		when(environment.getProperty("spring.profiles.active")).thenReturn(null);
		when(environment.getProperty("SPRING_PROFILES_ACTIVE")).thenReturn(null);
		when(environment.getProperty("server.port")).thenReturn(null);
		when(environment.getProperty("PORT")).thenReturn(null);
		when(environment.getProperty("HOSTNAME")).thenReturn(null);

		// When:
		Health health = indicator.health();

		// Then:
		assertThat(health.getStatus()).isEqualTo(org.springframework.boot.actuate.health.Status.UP);
		Map<String, Object> details = health.getDetails();
		assertThat(details.get("nodeEnv")).isEqualTo("");
		assertThat(details.get("port")).isEqualTo("");
		assertThat(details.get("hostname")).isEqualTo("");
		assertThat(details.get("hasDatabaseUrl")).isEqualTo(false);
		assertThat(details.get("hasOpenAiKey")).isEqualTo(false);
	}

	@Test
	void shouldDetectPgHostWhenDatabaseUrlIsMissingTest() {
		// Given:
		IntegrationHealthIndicator indicator = new IntegrationHealthIndicator(environment);
		// health() unconditionally resolves many property names before/around the PGHOST check;
		// stub a catch-all default first so strict stubbing doesn't flag those as mismatches, then
		// override the one name this test actually cares about.
		when(environment.getProperty(anyString())).thenReturn(null);
		when(environment.getProperty("PGHOST")).thenReturn("localhost");

		// When:
		Health health = indicator.health();

		// Then:
		assertThat(health.getDetails().get("hasPgHost")).isEqualTo(true);
	}

	@Test
	void shouldDetectAlternativeBucketVariablesTest() {
		// Given:
		IntegrationHealthIndicator indicator = new IntegrationHealthIndicator(environment);
		// health() unconditionally resolves many property names besides the bucket variables under
		// test; stub a catch-all default first so strict stubbing doesn't flag those as mismatches.
		when(environment.getProperty(anyString())).thenReturn(null);
		when(environment.getProperty("AWS_BUCKET")).thenReturn("aws-bucket");
		when(environment.getProperty("AWS_ENDPOINT")).thenReturn("aws-endpoint");
		when(environment.getProperty("AWS_ACCESS_KEY_ID")).thenReturn("aws-key");
		when(environment.getProperty("AWS_SECRET_ACCESS_KEY")).thenReturn("aws-secret");

		// When:
		Health health = indicator.health();

		// Then:
		Map<String, Object> details = health.getDetails();
		assertThat(details.get("hasBucket")).isEqualTo(true);
		assertThat(details.get("hasBucketEndpoint")).isEqualTo(true);
		assertThat(details.get("hasBucketAccessKeyId")).isEqualTo(true);
		assertThat(details.get("hasBucketSecretAccessKey")).isEqualTo(true);
	}

	@Test
	void shouldDetectRailwayBucketVariablesTest() {
		// Given:
		IntegrationHealthIndicator indicator = new IntegrationHealthIndicator(environment);
		// health() unconditionally resolves many property names besides the bucket variables under
		// test; stub a catch-all default first so strict stubbing doesn't flag those as mismatches.
		when(environment.getProperty(anyString())).thenReturn(null);
		when(environment.getProperty("RAILWAY_BUCKET_BUCKET")).thenReturn("railway-bucket");
		when(environment.getProperty("RAILWAY_BUCKET_ENDPOINT")).thenReturn("railway-endpoint");
		when(environment.getProperty("RAILWAY_BUCKET_ACCESS_KEY_ID")).thenReturn("railway-key");
		when(environment.getProperty("RAILWAY_BUCKET_SECRET_ACCESS_KEY")).thenReturn("railway-secret");

		// When:
		Health health = indicator.health();

		// Then:
		Map<String, Object> details = health.getDetails();
		assertThat(details.get("hasBucket")).isEqualTo(true);
		assertThat(details.get("hasBucketEndpoint")).isEqualTo(true);
		assertThat(details.get("hasBucketAccessKeyId")).isEqualTo(true);
		assertThat(details.get("hasBucketSecretAccessKey")).isEqualTo(true);
	}
}
