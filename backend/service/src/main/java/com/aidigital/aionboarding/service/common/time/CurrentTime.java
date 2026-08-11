package com.aidigital.aionboarding.service.common.time;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Provides the current time for production services that need UTC timestamps, from an
 * injectable boundary so tests can control "now" without touching the system clock. Direct
 * {@code now()} calls in production code are rejected by {@code check-production-current-time.sh};
 * use this interface (via {@link CurrentTimeImpl}) instead.
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
