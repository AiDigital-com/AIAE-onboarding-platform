package com.aidigital.aionboarding.external.storage.config;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

import static org.assertj.core.api.Assertions.assertThat;

class StorageConfigTest {

	@Test
	void shouldUseStaticCredentialsWhenKeyPairConfiguredTest() {
		// Given:
		StorageProperties properties = new StorageProperties();
		properties.setBucket("materials");
		properties.setAccessKeyId("AKIAEXAMPLE");
		properties.setSecretAccessKey("secret");

		// When:
		AwsCredentialsProvider provider = new StorageConfig().credentialsProvider(properties);

		// Then:
		assertThat(provider).isInstanceOf(StaticCredentialsProvider.class);
	}

	@Test
	void shouldFallBackToDefaultChainWhenKeyPairAbsentTest() {
		// Given: an IRSA-backed deployment supplies a bucket but no static key pair.
		StorageProperties properties = new StorageProperties();
		properties.setBucket("materials");

		// When:
		AwsCredentialsProvider provider = new StorageConfig().credentialsProvider(properties);

		// Then:
		assertThat(provider).isInstanceOf(DefaultCredentialsProvider.class);
	}

	@Test
	void shouldTreatBucketWithoutStaticCredentialsAsConfiguredTest() {
		// Given: the exact shape of an EKS pod using IRSA.
		StorageProperties properties = new StorageProperties();
		properties.setBucket("materials");

		// When:
		boolean configured = properties.isConfigured();

		// Then: reporting this as unconfigured is what silently bound the stub client.
		assertThat(configured).isTrue();
		assertThat(properties.hasStaticCredentials()).isFalse();
	}

	@Test
	void shouldNotBeConfiguredWithoutBucketTest() {
		// Given:
		StorageProperties properties = new StorageProperties();
		properties.setAccessKeyId("AKIAEXAMPLE");
		properties.setSecretAccessKey("secret");

		// When:
		boolean configured = properties.isConfigured();

		// Then:
		assertThat(configured).isFalse();
	}

	@Test
	void shouldReportStaticCredentialsOnlyWhenBothHalvesPresentTest() {
		// Given:
		StorageProperties properties = new StorageProperties();
		properties.setBucket("materials");
		properties.setAccessKeyId("AKIAEXAMPLE");

		// When:
		boolean hasStatic = properties.hasStaticCredentials();

		// Then: a half-configured key pair must not defeat the default chain.
		assertThat(hasStatic).isFalse();
	}
}
