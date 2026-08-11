package com.aidigital.aionboarding.service.common.time;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Provides the current time for production services that need UTC timestamps, from an
 * injectable boundary so tests can control "now" without touching the system clock. Direct
 * {@code now()} calls in production code are rejected by {@code check-production-current-time.sh};
 * use this interface (via {@link CurrentTimeImpl}) instead.
 * <p>
 * <b>There is a second, deliberately separate clock.</b>
 * {@code com.aidigital.aionboarding.external.common.time.CurrentTime} serves
 * {@code external-services}, which cannot depend on this module without a cycle. The two are
 * unrelated types that both wrap the system clock, so they agree in production but not
 * necessarily in a test. <b>If you freeze time, freeze both</b> — stubbing only this one
 * leaves the external boundary running on the real clock, which surfaces as an
 * occasionally-failing test rather than an obvious one. Presigned-URL expiry is computed on
 * this clock while the CloudFront signature is computed on the other, so that pair is the
 * likeliest place to notice.
 */
public interface CurrentTime {

    /**
     * Returns the current UTC date-time for database timestamp fields.
     *
     * @return current UTC date-time
     */
    LocalDateTime utcDateTime();

    /**
     * Returns the current instant for provider metadata timestamps.
     *
     * @return current instant
     */
    Instant instant();

    /**
     * Returns the current instant as an ISO-8601 string.
     *
     * @return current instant string
     */
    default String instantString() {
        return instant().toString();
    }
}
