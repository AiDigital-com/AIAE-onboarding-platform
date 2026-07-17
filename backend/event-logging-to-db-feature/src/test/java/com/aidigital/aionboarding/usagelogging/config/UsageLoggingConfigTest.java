package com.aidigital.aionboarding.usagelogging.config;

import com.aidigital.aionboarding.usagelogging.loggers.UsageLogger;
import com.aidigital.aionboarding.usagelogging.loggers.impl.NoOpUsageLogger;
import com.aidigital.aionboarding.usagelogging.loggers.impl.PostgresUsageLogger;
import com.aidigital.aionboarding.usagelogging.models.UsageEvent;
import com.aidigital.aionboarding.usagelogging.repositories.UsageEventRepository;
import com.aidigital.aionboarding.usagelogging.sink.UsageEventSink;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class UsageLoggingConfigTest {

	private final UsageLoggingConfig config = new UsageLoggingConfig();
	private final UsageEventSink usageEventSink = mock(UsageEventSink.class);

	private UsageLoggingProperties props(String serviceName) {
		UsageLoggingProperties p = new UsageLoggingProperties();
		p.setServiceName(serviceName);
		p.setEnvironment("test");
		return p;
	}

	@Test
	void shouldBuildPostgresLoggerForValidServiceNameTest() {
		// When:
		UsageLogger logger = config.postgresUsageLogger(usageEventSink, props("employee-directory"));

		// Then:
		assertThat(logger).isInstanceOf(PostgresUsageLogger.class);
	}

	@Test
	void shouldFailFastForBlankServiceNameTest() {
		// When / Then:
		assertThatThrownBy(() -> config.postgresUsageLogger(usageEventSink, props("  ")))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("service-name");
	}

	@Test
	void shouldFailFastForPlaceholderServiceNameTest() {
		// When / Then:
		assertThatThrownBy(() -> config.postgresUsageLogger(usageEventSink, props("replit-mvp-template")))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void shouldProvidePersistenceExecutorAndNoOpBeansTest() {
		// When / Then:
		assertThat(config.usageEventPersistenceService(mock(UsageEventRepository.class))).isNotNull();
		TaskExecutor executor = config.usageLoggingExecutor(props("svc"), new SimpleMeterRegistry());
		assertThat(executor).isNotNull();
		assertThat(config.noOpUsageLogger()).isInstanceOf(NoOpUsageLogger.class);
	}

	@Test
	void shouldDispatchThroughLoggersWithoutThrowingTest() {
		// Given:
		UsageEvent event = UsageEvent.builder().eventId("y").action("a").build();

		// When / Then: NoOp drops it; Postgres delegates to the (mocked) persistence
		config.noOpUsageLogger().record(event);
		config.postgresUsageLogger(usageEventSink, props("svc")).record(event);
	}
}
