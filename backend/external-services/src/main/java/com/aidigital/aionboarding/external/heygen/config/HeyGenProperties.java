package com.aidigital.aionboarding.external.heygen.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Runtime-tunable properties for the HeyGen adapter.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.external.heygen")
@Validated
public class HeyGenProperties {

    private boolean enabled = false;
    private String baseUrl = "https://api.heygen.com";
    private String apiKey = "";
    /** Configured avatar id after env-chain binding. */
    private String avatarId = "";
    /** Configured voice id after env-chain binding. */
    private String voiceId = "";

    /**
     * Returns whether an API key is configured.
     *
     * @return {@code true} if the API key is present and non-blank
     */
    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
