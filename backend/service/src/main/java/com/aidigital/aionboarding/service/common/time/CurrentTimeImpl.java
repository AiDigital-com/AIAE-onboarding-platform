package com.aidigital.aionboarding.service.common.time;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * UTC implementation of {@link CurrentTime}, backed by the system clock. Named
 * {@code CurrentTimeImpl.java} deliberately — {@code scan-production-java.py}'s time scanner
 * whitelists exactly this filename so the one legitimate {@code now()} call site in this
 * boundary does not trip the gate that exists to keep everywhere else from calling it directly.
 */
@Component
public class CurrentTimeImpl implements CurrentTime {

    /**
     * Returns the current UTC date-time from the system clock.
     *
     * @return current UTC date-time
     */
    @Override
    public LocalDateTime utcDateTime() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    /**
     * Returns the current instant from the system clock.
     *
     * @return current instant
     */
    @Override
    public Instant instant() {
        return Instant.now();
    }
}
