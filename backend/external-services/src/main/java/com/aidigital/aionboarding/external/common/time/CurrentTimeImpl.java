package com.aidigital.aionboarding.external.common.time;

import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * System-clock implementation of {@link CurrentTime} for {@code external-services}
 * integrations. Named {@code CurrentTimeImpl.java} deliberately — {@code scan-production-java.py}
 * whitelists exactly this filename regardless of package, so the one legitimate {@code now()}
 * call site in this boundary does not trip the gate.
 * <p>
 * Registered under an explicit bean name: {@code service.common.time.CurrentTimeImpl} is a
 * distinct, unrelated type also named {@code CurrentTimeImpl} that ends up in the same
 * application context (both modules are on {@code application}'s classpath), and Spring's
 * default annotation bean-name generator collides on the simple class name alone, ignoring the
 * package — {@code ConflictingBeanDefinitionException} otherwise.
 */
@Component("externalServicesCurrentTimeImpl")
public class CurrentTimeImpl implements CurrentTime {

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
