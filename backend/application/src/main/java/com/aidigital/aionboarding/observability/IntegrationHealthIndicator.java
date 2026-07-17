package com.aidigital.aionboarding.observability;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IntegrationHealthIndicator implements HealthIndicator {

    private final Environment environment;

    @Override
    public Health health() {
        return Health.up()
            .withDetail("ok", true)
            .withDetail("service", "aionboarding")
            .withDetail("nodeEnv", firstNonBlank(
                environment.getProperty("spring.profiles.active"),
                environment.getProperty("SPRING_PROFILES_ACTIVE"),
                ""
            ))
            .withDetail("port", firstNonBlank(
                environment.getProperty("server.port"),
                environment.getProperty("PORT"),
                ""
            ))
            .withDetail("hostname", firstNonBlank(
                environment.getProperty("HOSTNAME"),
                ""
            ))
            .withDetail("hasDatabaseUrl", hasAny("DATABASE_URL"))
            .withDetail("hasPgHost", hasAny("PGHOST"))
            .withDetail("hasOpenAiKey", hasAny("OPENAI_API_KEY"))
            .withDetail("hasBucket", hasAny("BUCKET", "AWS_BUCKET", "S3_BUCKET", "RAILWAY_BUCKET_BUCKET"))
            .withDetail("hasBucketEndpoint", hasAny(
                "ENDPOINT", "AWS_ENDPOINT", "S3_ENDPOINT", "RAILWAY_BUCKET_ENDPOINT"))
            .withDetail("hasBucketAccessKeyId", hasAny(
                "ACCESS_KEY_ID", "AWS_ACCESS_KEY_ID", "S3_ACCESS_KEY_ID", "RAILWAY_BUCKET_ACCESS_KEY_ID"))
            .withDetail("hasBucketSecretAccessKey", hasAny(
                "SECRET_ACCESS_KEY", "AWS_SECRET_ACCESS_KEY", "S3_SECRET_ACCESS_KEY", "RAILWAY_BUCKET_SECRET_ACCESS_KEY"))
            .build();
    }

    boolean hasAny(String... names) {
        for (String name : names) {
            if (!firstNonBlank(environment.getProperty(name), System.getenv(name)).isBlank()) {
                return true;
            }
        }
        return false;
    }

    String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
