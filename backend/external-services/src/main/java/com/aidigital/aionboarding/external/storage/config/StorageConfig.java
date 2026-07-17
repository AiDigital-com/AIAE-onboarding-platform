package com.aidigital.aionboarding.external.storage.config;

import com.aidigital.aionboarding.external.common.http.ExternalCallTimer;
import com.aidigital.aionboarding.external.storage.StorageClient;
import com.aidigital.aionboarding.external.storage.impl.CloudFrontUrlSigner;
import com.aidigital.aionboarding.external.storage.impl.StorageClientImpl;
import com.aidigital.aionboarding.external.storage.impl.StubStorageClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
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
	 * Live storage client when enabled and credentials are configured.
	 *
	 * @param properties          storage properties
	 * @param s3Client            shared S3 client
	 * @param presigner           shared presigner
	 * @param externalCallTimer   timer for S3 network calls
	 * @param cloudFrontUrlSigner CloudFront signer, present only when CloudFront is configured
	 * @return configured storage client
	 */
	@Bean
	@ConditionalOnExpression(
			"${app.external.storage.enabled:false} == true "
					+ "and '${app.external.storage.bucket:}'.length() > 0 "
					+ "and '${app.external.storage.access-key-id:}'.length() > 0 "
					+ "and '${app.external.storage.secret-access-key:}'.length() > 0")
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
	 * @param properties storage properties holding the CloudFront domain/key-pair-id/private-key
	 * @return configured signer
	 */
	@Bean
	@ConditionalOnExpression(
			"${app.external.storage.cloud-front-enabled:false} == true "
					+ "and ${app.external.storage.cloud-front-signed-url-enabled:true} == true "
					+ "and '${app.external.storage.cloud-front-domain:}'.length() > 0 "
					+ "and '${app.external.storage.cloud-front-key-pair-id:}'.length() > 0 "
					+ "and '${app.external.storage.cloud-front-private-key:}'.length() > 0")
	public CloudFrontUrlSigner cloudFrontUrlSigner(StorageProperties properties) {
		return new CloudFrontUrlSigner(properties);
	}

	/**
	 * Enabled-but-unconfigured stub when bucket credentials are absent.
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
	@ConditionalOnExpression(
			"${app.external.storage.enabled:false} == true "
					+ "and '${app.external.storage.bucket:}'.length() > 0 "
					+ "and '${app.external.storage.access-key-id:}'.length() > 0 "
					+ "and '${app.external.storage.secret-access-key:}'.length() > 0")
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
	@ConditionalOnExpression(
			"${app.external.storage.enabled:false} == true "
					+ "and '${app.external.storage.bucket:}'.length() > 0 "
					+ "and '${app.external.storage.access-key-id:}'.length() > 0 "
					+ "and '${app.external.storage.secret-access-key:}'.length() > 0")
	public S3Presigner s3Presigner(StorageProperties properties) {
		return buildS3Presigner(properties);
	}

	S3Client buildS3Client(StorageProperties properties) {
		S3ClientBuilder builder = S3Client.builder()
				.region(Region.of(properties.getEffectiveRegion()))
				.credentialsProvider(StaticCredentialsProvider.create(
						AwsBasicCredentials.create(properties.getAccessKeyId(), properties.getSecretAccessKey())));

		if (properties.getEndpoint() != null && !properties.getEndpoint().isBlank()) {
			builder.endpointOverride(URI.create(properties.getEndpoint().trim()));
			builder.forcePathStyle(properties.isForcePathStyle());
		}

		return builder.build();
	}

	S3Presigner buildS3Presigner(StorageProperties properties) {
		Builder builder = S3Presigner.builder()
				.region(Region.of(properties.getEffectiveRegion()))
				.credentialsProvider(StaticCredentialsProvider.create(
						AwsBasicCredentials.create(properties.getAccessKeyId(), properties.getSecretAccessKey())));

		if (properties.getEndpoint() != null && !properties.getEndpoint().isBlank()) {
			builder.endpointOverride(URI.create(properties.getEndpoint().trim()));
		}

		return builder.build();
	}
}
