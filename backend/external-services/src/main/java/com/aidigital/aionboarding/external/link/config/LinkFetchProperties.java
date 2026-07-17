package com.aidigital.aionboarding.external.link.config;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Runtime-tunable properties for the SSRF-resistant link-fetch adapter.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.external.link-fetch")
@Validated
public class LinkFetchProperties {

    private boolean enabled = true;

    @Positive
    private int maxRedirects = 5;

    @Positive
    private long maxResponseBytes = 2_000_000;

    @NotEmpty
    private List<String> allowedContentTypePrefixes = List.of("text/html", "application/xhtml+xml", "text/plain");
}
