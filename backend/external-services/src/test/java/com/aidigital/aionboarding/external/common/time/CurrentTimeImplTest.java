package com.aidigital.aionboarding.external.common.time;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class CurrentTimeImplTest {

	private final CurrentTimeImpl currentTime = new CurrentTimeImpl();

	@Test
	void shouldReturnInstantCloseToSystemClockTest() {
		// Given/When: this is the negative case for the split — before it,
		// CloudFrontUrlSigner.sign() called Instant.now() directly with no seam a test could
		// substitute; now every caller in this module depends on this interface instead.
		Instant before = Instant.now();
		Instant result = currentTime.instant();
		Instant after = Instant.now();

		// Then:
		assertThat(result).isBetween(before, after);
	}
}
