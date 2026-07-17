package com.aidigital.aionboarding.service.common.observability;

import com.aidigital.aionboarding.service.common.observability.enums.ContinuationRejectionReason;
import com.aidigital.aionboarding.service.common.observability.enums.RateLimitedOperation;
import com.aidigital.aionboarding.service.common.observability.enums.SsrfBlockReason;
import com.aidigital.aionboarding.service.common.observability.enums.UploadRejectionReason;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityMetricsTest {

	@Test
	void uploadRejectedShouldIncrementCounterTaggedOnlyWithTheFixedReasonTest() {
		// Given:
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		SecurityMetrics metrics = new SecurityMetrics(registry);

		// When:
		metrics.uploadRejected(UploadRejectionReason.MIME_NOT_ALLOWED);
		metrics.uploadRejected(UploadRejectionReason.MIME_NOT_ALLOWED);
		metrics.uploadRejected(UploadRejectionReason.SIZE_MISMATCH);

		// Then:
		assertThat(registry.counter("security.upload.rejected", "reason", "mime_not_allowed").count()).isEqualTo(2.0);
		assertThat(registry.counter("security.upload.rejected", "reason", "size_mismatch").count()).isEqualTo(1.0);
		assertThat(registry.getMeters()).hasSize(2);
	}

	@Test
	void ssrfBlockedShouldIncrementCounterTaggedOnlyWithTheFixedReasonTest() {
		// Given:
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		SecurityMetrics metrics = new SecurityMetrics(registry);

		// When:
		metrics.ssrfBlocked(SsrfBlockReason.PRIVATE_ADDRESS);

		// Then:
		assertThat(registry.counter("security.ssrf.blocked", "reason", "private_address").count()).isEqualTo(1.0);
		assertThat(registry.getMeters()).hasSize(1);
	}

	@Test
	void identityBindingConflictShouldIncrementAnUntaggedCounterTest() {
		// Given:
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		SecurityMetrics metrics = new SecurityMetrics(registry);

		// When:
		metrics.identityBindingConflict();
		metrics.identityBindingConflict();

		// Then:
		assertThat(registry.counter("security.identity.binding_conflict").count()).isEqualTo(2.0);
	}

	@Test
	void rateLimitDecisionShouldTagOperationAndOutcomeSeparatelyTest() {
		// Given:
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		SecurityMetrics metrics = new SecurityMetrics(registry);

		// When:
		metrics.rateLimitDecision(RateLimitedOperation.AI_LESSON_GENERATION, true);
		metrics.rateLimitDecision(RateLimitedOperation.AI_LESSON_GENERATION, false);

		// Then:
		assertThat(registry.counter(
				"security.rate_limit.decisions", "operation", "ai_lesson_generation", "outcome", "allowed"
		).count()).isEqualTo(1.0);
		assertThat(registry.counter(
				"security.rate_limit.decisions", "operation", "ai_lesson_generation", "outcome", "denied"
		).count()).isEqualTo(1.0);
	}

	@Test
	void invalidContinuationShouldIncrementCounterTaggedOnlyWithTheFixedReasonTest() {
		// Given:
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		SecurityMetrics metrics = new SecurityMetrics(registry);

		// When:
		metrics.invalidContinuation(ContinuationRejectionReason.LESSON_MISMATCH);

		// Then:
		assertThat(registry.counter("security.assistant.invalid_continuation", "reason", "lesson_mismatch").count())
				.isEqualTo(1.0);
	}
}
