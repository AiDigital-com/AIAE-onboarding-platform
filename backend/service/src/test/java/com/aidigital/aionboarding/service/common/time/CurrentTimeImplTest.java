package com.aidigital.aionboarding.service.common.time;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class CurrentTimeImplTest {

	private final CurrentTimeImpl currentTime = new CurrentTimeImpl();

	@Test
	void shouldReturnUtcDateTimeCloseToSystemClockTest() {
		// Given/When:
		LocalDateTime before = LocalDateTime.now(ZoneOffset.UTC);
		LocalDateTime result = currentTime.utcDateTime();
		LocalDateTime after = LocalDateTime.now(ZoneOffset.UTC);

		// Then: this is the negative case for the split — before it, CurrentTime.java called
		// LocalDateTime.now(ZoneOffset.UTC) directly and tripped check-production-current-time.sh;
		// now the direct call is isolated to this exempted file, and callers depend only on the
		// interface's contract, verified here against the real system clock.
		assertThat(result).isBetween(before, after);
	}

	@Test
	void shouldReturnInstantCloseToSystemClockTest() {
		// Given/When:
		Instant before = Instant.now();
		Instant result = currentTime.instant();
		Instant after = Instant.now();

		// Then:
		assertThat(result).isBetween(before, after);
	}

	@Test
	void shouldReturnInstantStringMatchingTheInstantIsoRepresentationTest() {
		// When:
		String result = currentTime.instantString();

		// Then: the default method on the interface derives from instant(), not a second
		// now() call, so both values round-trip to the same instant within a second.
		assertThat(Instant.parse(result)).isCloseTo(Instant.now(), within(2, ChronoUnit.SECONDS));
	}
}
