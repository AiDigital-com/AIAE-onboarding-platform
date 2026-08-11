package com.aidigital.aionboarding.cachemanagement.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link CacheManagementProperties} validation and defaults.
 */
class CacheManagementPropertiesTest {

	@Test
	void shouldExposeSafeDefaultsTest() {
		// Given:
		CacheManagementProperties properties = new CacheManagementProperties();

		// Then:
		assertThat(properties.isVerifyRegistry()).isFalse();
		assertThat(properties.isCleanupEnabled()).isFalse();
		assertThat(properties.getBatchSize()).isEqualTo(500);
		assertThat(properties.getMaxBatchesPerPoll()).isEqualTo(20);
		assertThat(properties.getRetention()).isEqualTo(Duration.ofDays(7));
	}

	@Test
	void shouldRejectNonPositiveBatchSizeTest() {
		// Given:
		CacheManagementProperties properties = new CacheManagementProperties();

		// When-Then:
		assertThatThrownBy(() -> properties.setBatchSize(0))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void shouldRejectNonPositiveMaxBatchesPerPollTest() {
		// Given:
		CacheManagementProperties properties = new CacheManagementProperties();

		// When-Then:
		assertThatThrownBy(() -> properties.setMaxBatchesPerPoll(0))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void shouldRejectNonPositiveRetentionTest() {
		// Given:
		CacheManagementProperties properties = new CacheManagementProperties();

		// When-Then:
		assertThatThrownBy(() -> properties.setRetention(Duration.ZERO))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> properties.setRetention(null))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> properties.setRetention(Duration.ofDays(-1)))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void shouldAcceptValidOverridesTest() {
		// Given:
		CacheManagementProperties properties = new CacheManagementProperties();

		// When:
		properties.setBatchSize(100);
		properties.setMaxBatchesPerPoll(5);
		properties.setRetention(Duration.ofDays(1));
		properties.setVerifyRegistry(true);
		properties.setCleanupEnabled(true);

		// Then:
		assertThat(properties.getBatchSize()).isEqualTo(100);
		assertThat(properties.getMaxBatchesPerPoll()).isEqualTo(5);
		assertThat(properties.getRetention()).isEqualTo(Duration.ofDays(1));
		assertThat(properties.isVerifyRegistry()).isTrue();
		assertThat(properties.isCleanupEnabled()).isTrue();
	}
}
