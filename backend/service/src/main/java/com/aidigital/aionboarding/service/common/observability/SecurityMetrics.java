package com.aidigital.aionboarding.service.common.observability;

import com.aidigital.aionboarding.service.common.observability.enums.ContinuationRejectionReason;
import com.aidigital.aionboarding.service.common.observability.enums.RateLimitedOperation;
import com.aidigital.aionboarding.service.common.observability.enums.SsrfBlockReason;
import com.aidigital.aionboarding.service.common.observability.enums.UploadRejectionReason;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Records low-cardinality security-relevant rejection/decision counters shared across services
 * (upload verification, outbound link fetching, identity binding, abuse controls, AI-assistant
 * continuation). Every tag is a fixed enum value; callers must never pass a user ID, URL, storage
 * key, email, or provider response ID as a tag, since Prometheus label cardinality is unbounded
 * cost once a raw value leaks in.
 */
@Component
@RequiredArgsConstructor
public class SecurityMetrics {

    private final MeterRegistry meterRegistry;

    /**
     * Records that an upload was rejected before its storage key could be attached to an entity.
     *
     * @param reason fixed, low-cardinality rejection reason
     */
    public void uploadRejected(UploadRejectionReason reason) {
        meterRegistry.counter("security.upload.rejected", "reason", reason.value()).increment();
    }

    /**
     * Records that an outbound link-fetch destination or response was blocked.
     *
     * @param reason fixed, low-cardinality block reason
     */
    public void ssrfBlocked(SsrfBlockReason reason) {
        meterRegistry.counter("security.ssrf.blocked", "reason", reason.value()).increment();
    }

    /** Records that Clerk-subject-to-user resolution found a subject/row identity collision. */
    public void identityBindingConflict() {
        meterRegistry.counter("security.identity.binding_conflict").increment();
    }

    /**
     * Records an abuse-control allow/deny decision for one operation.
     *
     * @param operation fixed, low-cardinality operation identifier
     * @param allowed   {@code true} if the request was allowed to proceed
     */
    public void rateLimitDecision(RateLimitedOperation operation, boolean allowed) {
        meterRegistry.counter(
            "security.rate_limit.decisions",
            "operation", operation.value(),
            "outcome", allowed ? "allowed" : "denied"
        ).increment();
    }

    /**
     * Records that a lesson-assistant follow-up could not continue the requested conversation.
     *
     * @param reason fixed, low-cardinality rejection reason
     */
    public void invalidContinuation(ContinuationRejectionReason reason) {
        meterRegistry.counter("security.assistant.invalid_continuation", "reason", reason.value()).increment();
    }
}
