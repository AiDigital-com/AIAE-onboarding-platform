package com.aidigital.aionboarding.usagelogging.sink;

import com.aidigital.aionboarding.usagelogging.models.UsageEvent;
import com.aidigital.aionboarding.usagelogging.persistence.UsageEventPersistenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Default usage analytics sink: persists events to the {@code usage_events} table.
 */
@Component
@RequiredArgsConstructor
public class PostgreSqlUsageEventSink implements UsageEventSink {

    private final UsageEventPersistenceService persistenceService;

    @Override
    public void record(UsageEvent event) {
        persistenceService.persist(event);
    }
}
