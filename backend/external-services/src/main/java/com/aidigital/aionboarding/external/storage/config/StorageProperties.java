package com.aidigital.aionboarding.external.storage.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Runtime-tunable properties for S3-compatible object storage.
 *
 * <p>Typical {@code application.yml} stubs mirror the Node {@code storage.js}
 * env fallback chains for region, endpoint, credentials, and bucket.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.external.storage")
@Validated
public class StorageProperties {

    private boolean enabled = false;
    private String region = "us-east-1";
    private String endpoint = "";
    private String accessKeyId = "";
    private String secretAccessKey = "";
    private String bucket = "";
    private boolean forcePathStyle = true;
    private int presignPutExpiresSeconds = 3600;
    private int presignGetExpiresSeconds = 86400;
    private long maxUploadSizeBytes = 524_288_000L;
    private boolean cloudFrontEnabled = false;
    private boolean cloudFrontSignedUrlEnabled = true;
    private String cloudFrontDomain = "";
    private String cloudFrontKeyPairId = "";
    private String cloudFrontPrivateKey = "";

    /**
     * Returns the effective AWS region, normalising {@code auto} to {@code us-east-1}.
     *
     * @return region identifier
     */
    public String getEffectiveRegion() {
        if (region == null || region.isBlank() || "auto".equalsIgnoreCase(region.trim())) {
            return "us-east-1";
        }
        return region.trim();
    }

    /**
     * Returns whether credentials and bucket are present enough for a live client.
     *
     * @return {@code true} when bucket and access keys are non-blank
     */
    public boolean isConfigured() {
        return bucket != null && !bucket.isBlank()
            && accessKeyId != null && !accessKeyId.isBlank()
            && secretAccessKey != null && !secretAccessKey.isBlank();
    }
}
