package com.aidigital.aionboarding.config;

import org.junit.jupiter.api.Test;
import org.zalando.logbook.BodyFilter;
import org.zalando.logbook.HeaderFilter;
import org.zalando.logbook.HttpHeaders;
import org.zalando.logbook.HttpLogFormatter;
import org.zalando.logbook.Strategy;
import org.zalando.logbook.core.DefaultStrategy;
import org.zalando.logbook.core.WithoutBodyStrategy;
import org.zalando.logbook.json.JsonHttpLogFormatter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LogbookConfig}: the {@code logBodies} diagnostic-mode toggle and the
 * masking filters that must still apply whenever a body is actually logged.
 */
class LogbookConfigTest {

	private final LogbookConfig config = new LogbookConfig();

	@Test
	void resolveFormatterDefaultsToMetadataOnlyTest() {
		LogbookConfig.LogbookProperties props = new LogbookConfig.LogbookProperties();

		HttpLogFormatter formatter = config.resolveFormatter(props);

		assertThat(formatter).isInstanceOf(MetadataOnlyHttpLogFormatter.class);
	}

	@Test
	void resolveFormatterUsesFullJsonWhenLogBodiesEnabledTest() {
		LogbookConfig.LogbookProperties props = new LogbookConfig.LogbookProperties();
		props.setLogBodies(true);

		HttpLogFormatter formatter = config.resolveFormatter(props);

		assertThat(formatter).isInstanceOf(JsonHttpLogFormatter.class);
	}

	@Test
	void resolveStrategyDefaultsToWithoutBodyTest() {
		LogbookConfig.LogbookProperties props = new LogbookConfig.LogbookProperties();

		Strategy strategy = config.resolveStrategy(props);

		assertThat(strategy).isInstanceOf(WithoutBodyStrategy.class);
	}

	@Test
	void resolveStrategyUsesDefaultBufferingWhenLogBodiesEnabledTest() {
		LogbookConfig.LogbookProperties props = new LogbookConfig.LogbookProperties();
		props.setLogBodies(true);

		Strategy strategy = config.resolveStrategy(props);

		assertThat(strategy).isInstanceOf(DefaultStrategy.class);
	}

	@Test
	void buildBodyFilterMasksConfiguredSensitiveJsonFieldTest() {
		LogbookConfig.LogbookProperties props = new LogbookConfig.LogbookProperties();

		BodyFilter bodyFilter = config.buildBodyFilter(props);
		String filtered = bodyFilter.filter("application/json", "{\"password\":\"hunter2\",\"title\":\"Lesson\"}");

		assertThat(filtered).contains("\"password\":\"XXX\"").contains("\"title\":\"Lesson\"");
	}

	@Test
	void buildHeaderFilterMasksConfiguredSensitiveHeaderTest() {
		LogbookConfig.LogbookProperties props = new LogbookConfig.LogbookProperties();

		HeaderFilter headerFilter = config.buildHeaderFilter(props);
		HttpHeaders filtered = headerFilter.filter(HttpHeaders.of("Authorization", "Bearer secret-token"));

		assertThat(filtered.getFirst("Authorization")).isEqualTo("XXX");
	}
}
