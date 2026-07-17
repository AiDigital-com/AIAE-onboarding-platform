// LogbookConfig — JSON HTTP request/response logging with required masking.
// Canonical spec: templates/generated-project/observability/logbook-http-logging-rules.md.
// Binds app.logbook.* properties (see application.yml baseline). Logbook's
// permissive defaults are NOT enough — this config enforces the company
// rules (Authorization/JWT masked; secret-like JSON fields masked;
// health/spec/swagger/options excluded).

package com.aidigital.aionboarding.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zalando.logbook.BodyFilter;
import org.zalando.logbook.HeaderFilter;
import org.zalando.logbook.HttpLogFormatter;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.Logbook;
import org.zalando.logbook.Strategy;
import org.zalando.logbook.core.BodyFilters;
import org.zalando.logbook.core.Conditions;
import org.zalando.logbook.core.DefaultHttpLogWriter;
import org.zalando.logbook.core.DefaultSink;
import org.zalando.logbook.core.DefaultStrategy;
import org.zalando.logbook.core.HeaderFilters;
import org.zalando.logbook.core.WithoutBodyStrategy;
import org.zalando.logbook.json.JsonBodyFilters;
import org.zalando.logbook.json.JsonHttpLogFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Configures structured HTTP logging and masking for Logbook.
 */
@Configuration
@EnableConfigurationProperties(LogbookConfig.LogbookProperties.class)
public class LogbookConfig {

	/**
	 * Builds the Logbook instance used by the servlet filter.
	 *
	 * @param props bound masking and exclusion settings
	 * @return configured Logbook instance
	 */
	@Bean
	Logbook logbook(LogbookProperties props) {
		return Logbook.builder()
				.condition(Conditions.exclude(buildExclusions(props)))
				.headerFilter(buildHeaderFilter(props))
				.bodyFilter(buildBodyFilter(props))
				.strategy(resolveStrategy(props))
				.sink(new DefaultSink(resolveFormatter(props), new DefaultHttpLogWriter()))
				.build();
	}

	/**
	 * Builds the header-masking filter from the configured header allowlist.
	 *
	 * @param props bound masking settings
	 * @return filter replacing censored header values
	 */
	HeaderFilter buildHeaderFilter(LogbookProperties props) {
		return HeaderFilters.replaceHeaders(
				name -> props.getHeadersToCensor().stream()
						.anyMatch(h -> h.equalsIgnoreCase(name)),
				props.getCensoredReplacement()
		);
	}

	/**
	 * Builds the JSON body-masking filter from the configured field-name regex patterns.
	 * {@code JsonBodyFilters.replaceJsonStringProperty}'s {@code Set<String>} overload matches
	 * field names by exact equality, not regex, so each configured pattern (e.g.
	 * {@code .*password.*}) is compiled and matched via a {@link Predicate} instead — otherwise a
	 * field literally named {@code password} would never be masked.
	 *
	 * @param props bound masking settings
	 * @return combined filter replacing every configured sensitive JSON field
	 */
	BodyFilter buildBodyFilter(LogbookProperties props) {
		List<BodyFilter> bodyFilters = new ArrayList<>();
		for (String pattern : props.getJsonFieldsToCensor()) {
			Pattern compiled = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
			bodyFilters.add(JsonBodyFilters.replaceJsonStringProperty(
					(Predicate<String>) name -> name != null && compiled.matcher(name).matches(),
					props.getCensoredReplacement()));
		}
		return bodyFilters.stream()
				.reduce(BodyFilter::merge)
				.orElse(BodyFilters.defaultValue());
	}

	/**
	 * Builds the path/method exclusions for noisy infrastructure endpoints (actuator, swagger,
	 * OpenAPI spec, health probes, all OPTIONS). Each configured entry is a path mapped to its
	 * list of excluded methods.
	 *
	 * @param props bound exclusion settings
	 * @return predicates matching requests to exclude from logging
	 */
	List<Predicate<HttpRequest>> buildExclusions(LogbookProperties props) {
		return props.getExcluded().entrySet().stream()
				.flatMap(e -> e.getValue().stream()
						.map(method -> {
							Predicate<HttpRequest> pathMatches = Conditions.requestTo(e.getKey());
							Predicate<HttpRequest> methodMatches = Conditions.requestWithMethod(method);
							return pathMatches.and(methodMatches);
						}))
				.toList();
	}

	/**
	 * Resolves the log-line formatter: bounded metadata by default so large lesson/material
	 * payloads are never parsed/masked/serialized just to log a line, or full masked-body JSON
	 * when the {@code logBodies} diagnostic override is enabled.
	 *
	 * @param props bound logging-mode settings
	 * @return the formatter to use for this mode
	 */
	HttpLogFormatter resolveFormatter(LogbookProperties props) {
		return props.isLogBodies() ? new JsonHttpLogFormatter() : new MetadataOnlyHttpLogFormatter();
	}

	/**
	 * Resolves the buffering strategy: {@link WithoutBodyStrategy} by default so request/response
	 * bodies are never buffered at all, or {@link DefaultStrategy} when the {@code logBodies}
	 * diagnostic override is enabled.
	 *
	 * @param props bound logging-mode settings
	 * @return the strategy to use for this mode
	 */
	Strategy resolveStrategy(LogbookProperties props) {
		return props.isLogBodies() ? new DefaultStrategy() : new WithoutBodyStrategy();
	}

	/**
	 * Binds `app.logbook.*` from application.yml. Defaults mirror the
	 * canonical rules so a fresh project is compliant without overrides.
	 */
	@Getter
	@Setter
	@ConfigurationProperties("app.logbook")
	public static class LogbookProperties {

		/**
		 * Diagnostic override only — false (default) logs bounded metadata (method, path,
		 * status, duration, correlation id, content length) with bodies never buffered or
		 * read. Set true temporarily to restore full masked-body JSON logging; revert after
		 * troubleshooting.
		 */
		private boolean logBodies = false;
		private String censoredReplacement = "XXX";
		private List<String> headersToCensor = List.of(
				"Authorization", "Cookie", "Set-Cookie",
				"X-API-Key", "AccessKey", "Proxy-Authorization");
		private List<String> jsonFieldsToCensor = List.of(
				".*password.*", ".*token.*", ".*secret.*", ".*key.*",
				".*credential.*", ".*authorization.*",
				"privateKey", "clientSecret", "serviceAccount");
		private Map<String, List<String>> excluded = Map.of(
				"/**/actuator/**", List.of("GET"),
				"/**/swagger-ui/**", List.of("GET"),
				"/**/specs/**", List.of("GET"),
				"/health", List.of("GET"),
				"/**", List.of("OPTIONS"));
	}
}
