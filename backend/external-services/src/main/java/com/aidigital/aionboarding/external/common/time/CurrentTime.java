package com.aidigital.aionboarding.external.common.time;

import java.time.Instant;

/**
 * Provides the current instant for {@code external-services} integrations, from an injectable
 * boundary so tests can control "now" without touching the system clock.
 * <p>
 * This mirrors {@code service.common.time.CurrentTime}'s contract but is a distinct type kept
 * local to this module: {@code service} depends on {@code external-services}, so
 * {@code external-services} cannot depend back on {@code service}'s {@code CurrentTime}
 * without a module cycle. There is no lower-level module both sides already share for this —
 * the scaffold this project is built from has no {@code external-services} module at all, so
 * this boundary was never designed for. Duplicating the narrow time-boundary contract here is
 * simpler and safer than adding a new inter-module dependency edge for one field.
 */
public interface CurrentTime {

    /**
     * Returns the current instant.
     *
     * @return current instant from the application clock
     */
    Instant instant();
}
