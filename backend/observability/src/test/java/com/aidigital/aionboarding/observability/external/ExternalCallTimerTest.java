package com.aidigital.aionboarding.observability.external;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies that {@link ExternalCallTimer} records the {@code external.client.requests}
 * timer under the fixed {@code client}/{@code operation}/{@code outcome} tags this class's
 * JavaDoc promises, and that a thrown exception is rethrown while still being timed and
 * tagged as an error outcome.
 */
class ExternalCallTimerTest {

	@Test
	void recordSupplierShouldTagSuccessOutcomeAndReturnTheValueTest() {
		// Given:
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		ExternalCallTimer timer = new ExternalCallTimer(registry);

		// When:
		String result = timer.record("s3", "getObject", () -> "payload");

		// Then:
		assertThat(result).isEqualTo("payload");
		assertThat(registry.get("external.client.requests")
				.tag("client", "s3")
				.tag("operation", "getObject")
				.tag("outcome", "success")
				.timer().count()).isEqualTo(1L);
	}

	@Test
	void recordSupplierShouldTagErrorOutcomeAndRethrowWhenTheCallThrowsTest() {
		// Given:
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		ExternalCallTimer timer = new ExternalCallTimer(registry);
		RuntimeException failure = new IllegalStateException("downstream failed");

		// When-Then:
		assertThatThrownBy(() -> timer.record("s3", "putObject", () -> {
			throw failure;
		})).isSameAs(failure);

		assertThat(registry.get("external.client.requests")
				.tag("client", "s3")
				.tag("operation", "putObject")
				.tag("outcome", "error")
				.timer().count()).isEqualTo(1L);
	}

	@Test
	void recordRunnableShouldTagSuccessOutcomeAndRunTheCallTest() {
		// Given:
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		ExternalCallTimer timer = new ExternalCallTimer(registry);
		boolean[] invoked = {false};
		// Explicitly typed so overload resolution picks the Runnable overload rather than
		// the Supplier<T> one — an untyped assignment-expression lambda is compatible with
		// both and javac resolves it to Supplier, which would leave this overload untested.
		Runnable call = () -> invoked[0] = true;

		// When:
		timer.record("s3", "deleteObjects", call);

		// Then:
		assertThat(invoked[0]).isTrue();
		assertThat(registry.get("external.client.requests")
				.tag("client", "s3")
				.tag("operation", "deleteObjects")
				.tag("outcome", "success")
				.timer().count()).isEqualTo(1L);
	}

	@Test
	void recordRunnableShouldTagErrorOutcomeAndRethrowWhenTheCallThrowsTest() {
		// Given:
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		ExternalCallTimer timer = new ExternalCallTimer(registry);
		RuntimeException failure = new IllegalStateException("downstream failed");
		// See the note above: a throw-only lambda body is also compatible with both
		// overloads, so it is typed explicitly to force the Runnable overload.
		Runnable call = () -> {
			throw failure;
		};

		// When-Then:
		assertThatThrownBy(() -> timer.record("s3", "deleteObjects", call)).isSameAs(failure);

		assertThat(registry.get("external.client.requests")
				.tag("client", "s3")
				.tag("operation", "deleteObjects")
				.tag("outcome", "error")
				.timer().count()).isEqualTo(1L);
	}
}
