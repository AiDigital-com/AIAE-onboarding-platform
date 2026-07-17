package com.aidigital.aionboarding.service.common.time;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Provides the current time for production services that need UTC timestamps.
 */
@Component
public class CurrentTime {

    /**
     * Returns the current UTC date-time for database timestamp fields.
     *
     * @return current UTC date-time
     */
    public LocalDateTime utcDateTime() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    /**
     * Returns the current instant for provider metadata timestamps.
     *
     * @return current instant
     */
    public Instant instant() {
        return Instant.now();
    }

    /**
     * Returns the current instant as an ISO-8601 string.
     *
     * @return current instant string
     */
    public String instantString() {
        return instant().toString();
    }
}
