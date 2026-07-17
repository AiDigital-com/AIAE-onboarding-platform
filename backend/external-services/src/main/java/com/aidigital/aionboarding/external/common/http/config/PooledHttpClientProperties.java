package com.aidigital.aionboarding.external.common.http.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Runtime-tunable properties for the shared pooled HTTP client.
 *
 * <p>All fields bind from the {@code app.external.http.*} namespace.
 * Validation is enforced at context startup via {@code @Validated}.
 *
 * <p>Typical {@code application.yml} stubs:
 * <pre>
 * app:
 *   external:
 *     http:
 *       connect-timeout: 5s
 *       response-timeout: 30s
 *       connection-request-timeout: 5s
 *       max-total-connections: 50
 *       max-connections-per-route: 10
 *       keep-alive-duration: 60s
 *       idle-eviction-duration: 30s
 * </pre>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.external.http")
@Validated
public class PooledHttpClientProperties {

    /** Maximum time to establish a TCP connection. */
    @NotNull
    private Duration connectTimeout = Duration.ofSeconds(5);

    /** Maximum time to wait for the first response byte after sending the request. */
    @NotNull
    private Duration responseTimeout = Duration.ofSeconds(30);

    /** Maximum time to wait for a connection lease from the pool. */
    @NotNull
    private Duration connectionRequestTimeout = Duration.ofSeconds(5);

    /** Maximum number of connections across all routes. */
    @Positive
    private int maxTotalConnections = 50;

    /** Maximum number of connections per individual route (host). */
    @Positive
    private int maxConnectionsPerRoute = 10;

    /** How long to keep a connection alive in the pool when idle. */
    @NotNull
    private Duration keepAliveDuration = Duration.ofSeconds(60);

    /** How long before idle connections are evicted from the pool. */
    @NotNull
    private Duration idleEvictionDuration = Duration.ofSeconds(30);
}