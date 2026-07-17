package com.aidigital.aionboarding.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression fixture: verifies the actual HTTP response headers a
 * browser/CDN would see for SPA delivery. Needs a real embedded server (not MockMvc) —
 * Tomcat's gzip compression happens below the DispatcherServlet layer and MockMvc never
 * exercises it. Reads a fixture under {@code src/test/resources/static/assets/} (merged
 * onto the test classpath alongside the real build output) so these assertions don't
 * depend on the frontend having actually been built first.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class StaticDeliveryHeadersIntegrationTest {

	private static final String HASHED_ASSET_PATH = "/assets/perf017-cache-test-fixture.js";

	@Autowired
	private TestRestTemplate restTemplate;

	// SSO-only: SecurityConfig.jwtDecoder is @ConditionalOnMissingBean and fails fast
	// without a real Clerk issuer. A stub decoder lets the context load without
	// reaching a JWKS endpoint (see ApplicationSmokeTest).
	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	void indexHtmlShouldBeNoCacheAndMustRevalidateTest() {
		// When:
		ResponseEntity<String> response = restTemplate.getForEntity("/", String.class);

		// Then:
		assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
		assertThat(response.getHeaders().getCacheControl()).contains("no-cache").contains("must-revalidate");
	}

	@Test
	void hashedAssetShouldBeImmutableAndFarFutureCacheableTest() {
		// When:
		ResponseEntity<byte[]> response = restTemplate.getForEntity(HASHED_ASSET_PATH, byte[].class);

		// Then:
		assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
		assertThat(response.getHeaders().getCacheControl())
				.contains("immutable")
				.contains("max-age=31536000")
				.contains("public");
	}

	@Test
	void hashedAssetShouldBeGzipCompressedWhenAcceptedTest() throws Exception {
		// Given: a raw JDK HttpClient — TestRestTemplate's Apache HttpClient backing would
		// otherwise auto-negotiate Accept-Encoding and silently decompress the body,
		// hiding the very header/behavior this test verifies.
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder(URI.create(restTemplate.getRootUri() + HASHED_ASSET_PATH))
				.header("Accept-Encoding", "gzip")
				.GET()
				.build();

		// When:
		HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

		// Then:
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.headers().firstValue("Content-Encoding")).contains("gzip");
	}
}
