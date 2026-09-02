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
     * Returns whether the bucket and some resolvable credential source are present.
     *
     * <p>On EKS the pod authenticates through IRSA, so the static key pair is absent by
     * design and the AWS SDK default provider chain supplies credentials instead. Requiring
     * static keys here would report an IRSA-backed deployment as unconfigured.
     *
     * @return {@code true} when a bucket is set
     */
    public boolean isConfigured() {
        return bucket != null && !bucket.isBlank();
    }

    /**
     * Returns whether an explicit static access key pair was supplied.
     *
     * <p>True for local development and S3-compatible third-party endpoints, false on EKS
     * where IRSA provides short-lived credentials through the default provider chain.
     *
     * @return {@code true} when both the access key id and secret access key are non-blank
     */
    public boolean hasStaticCredentials() {
        return accessKeyId != null && !accessKeyId.isBlank()
            && secretAccessKey != null && !secretAccessKey.isBlank();
    }
}
