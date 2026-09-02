package com.aidigital.aionboarding.external.storage.config;

import com.aidigital.aionboarding.external.common.time.CurrentTime;
import com.aidigital.aionboarding.external.storage.StorageClient;
import com.aidigital.aionboarding.external.storage.impl.CloudFrontUrlSigner;
import com.aidigital.aionboarding.external.storage.impl.StorageClientImpl;
import com.aidigital.aionboarding.external.storage.impl.StubStorageClient;
import com.aidigital.aionboarding.observability.external.ExternalCallTimer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.S3Presigner.Builder;

import java.net.URI;
import java.util.Optional;

/**
 * Registers S3 SDK beans and the {@link StorageClient} adapter.
 */
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

	/**
	 * Guard shared by the live storage beans.
	 *
	 * <p>Deliberately does NOT require a static access key pair. On EKS the pod is
	 * authenticated by IRSA and no key pair exists; requiring one silently bound
	 * {@link StubStorageClient} instead, so every upload and preview failed while the
	 * application still reported itself healthy.
	 */
	private static final String LIVE_STORAGE_CONDITION =
			"${app.external.storage.enabled:false} == true "
					+ "and '${app.external.storage.bucket:}'.length() > 0";

	/**
	 * Live storage client when storage is enabled and a bucket is configured.
	 *
	 * @param properties          storage properties
	 * @param s3Client            shared S3 client
	 * @param presigner           shared presigner
	 * @param externalCallTimer   timer for S3 network calls
	 * @param cloudFrontUrlSigner CloudFront signer, present only when CloudFront is configured
	 * @return configured storage client
	 */
	@Bean
	@ConditionalOnExpression(LIVE_STORAGE_CONDITION)
	public StorageClient storageClient(
			StorageProperties properties,
			S3Client s3Client,
			S3Presigner presigner,
			ExternalCallTimer externalCallTimer,
			Optional<CloudFrontUrlSigner> cloudFrontUrlSigner) {
		return new StorageClientImpl(properties, s3Client, presigner, externalCallTimer, cloudFrontUrlSigner);
	}

	/**
	 * CloudFront URL signer, registered only when the CloudFront key pair is fully configured.
	 * When present, {@link StorageClientImpl#presignGet} returns CloudFront-signed URLs instead
	 * of S3 presigned URLs; uploads keep using direct S3 presigned PUT regardless.
	 *
	 * @param properties  storage properties holding the CloudFront domain/key-pair-id/private-key
	 * @param currentTime injectable time boundary used to compute the signature expiration
	 * @return configured signer
	 */
	@Bean
	@ConditionalOnExpression(
			"${app.external.storage.cloud-front-enabled:false} == true "
					+ "and ${app.external.storage.cloud-front-signed-url-enabled:true} == true "
					+ "and '${app.external.storage.cloud-front-domain:}'.length() > 0 "
					+ "and '${app.external.storage.cloud-front-key-pair-id:}'.length() > 0 "
					+ "and '${app.external.storage.cloud-front-private-key:}'.length() > 0")
	public CloudFrontUrlSigner cloudFrontUrlSigner(StorageProperties properties, CurrentTime currentTime) {
		return new CloudFrontUrlSigner(properties, currentTime);
	}

	/**
	 * Enabled-but-unconfigured stub when no bucket is configured.
	 *
	 * @return no-op storage client
	 */
	@Bean
	@ConditionalOnProperty(prefix = "app.external.storage", name = "enabled", havingValue = "true")
	@ConditionalOnMissingBean(StorageClient.class)
	public StorageClient enabledStubStorageClient() {
		return new StubStorageClient();
	}

	/**
	 * Fallback stub when storage is disabled.
	 *
	 * @return no-op storage client
	 */
	@Bean
	@ConditionalOnMissingBean(StorageClient.class)
	public StorageClient stubStorageClient() {
		return new StubStorageClient();
	}

	/**
	 * Shared S3 client bean for advanced callers.
	 *
	 * @param properties storage properties
	 * @return configured {@link S3Client}
	 */
	@Bean
	@ConditionalOnExpression(LIVE_STORAGE_CONDITION)
	public S3Client s3Client(StorageProperties properties) {
		return buildS3Client(properties);
	}

	/**
	 * Shared presigner bean for advanced callers.
	 *
	 * @param properties storage properties
	 * @return configured {@link S3Presigner}
	 */
	@Bean
	@ConditionalOnExpression(LIVE_STORAGE_CONDITION)
	public S3Presigner s3Presigner(StorageProperties properties) {
		return buildS3Presigner(properties);
	}

	/**
	 * Builds the S3 client for the configured region, endpoint and credential source.
	 *
	 * @param properties storage properties
	 * @return configured {@link S3Client}
	 */
	S3Client buildS3Client(StorageProperties properties) {
		S3ClientBuilder builder = S3Client.builder()
				.region(Region.of(properties.getEffectiveRegion()))
				.credentialsProvider(credentialsProvider(properties));

		if (properties.getEndpoint() != null && !properties.getEndpoint().isBlank()) {
			builder.endpointOverride(URI.create(properties.getEndpoint().trim()));
			builder.forcePathStyle(properties.isForcePathStyle());
		}

		return builder.build();
	}

	/**
	 * Builds the S3 presigner for the configured region, endpoint and credential source.
	 *
	 * @param properties storage properties
	 * @return configured {@link S3Presigner}
	 */
	S3Presigner buildS3Presigner(StorageProperties properties) {
		Builder builder = S3Presigner.builder()
				.region(Region.of(properties.getEffectiveRegion()))
				.credentialsProvider(credentialsProvider(properties));

		if (properties.getEndpoint() != null && !properties.getEndpoint().isBlank()) {
			builder.endpointOverride(URI.create(properties.getEndpoint().trim()));
		}

		return builder.build();
	}

	/**
	 * Selects the credential source for the S3 SDK clients.
	 *
	 * <p>An explicit key pair wins so local development and S3-compatible third-party
	 * endpoints keep working. Otherwise the AWS SDK default provider chain resolves the
	 * IRSA web-identity token projected into the pod.
	 *
	 * @param properties storage properties
	 * @return static provider when a key pair is configured, default chain otherwise
	 */
	AwsCredentialsProvider credentialsProvider(StorageProperties properties) {
		if (properties.hasStaticCredentials()) {
			return StaticCredentialsProvider.create(
					AwsBasicCredentials.create(properties.getAccessKeyId(), properties.getSecretAccessKey()));
		}
		return DefaultCredentialsProvider.create();
	}
}
