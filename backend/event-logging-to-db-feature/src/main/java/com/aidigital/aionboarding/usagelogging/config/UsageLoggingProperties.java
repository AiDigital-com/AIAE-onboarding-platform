// UsageLoggingProperties — @ConfigurationProperties("app.usage-logging").
// Binding spec: observability/usage-logging-rules.md → "Required env placeholders".

package com.aidigital.aionboarding.usagelogging.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed configuration for usage event persistence.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.usage-logging")
public class UsageLoggingProperties {

	/**
	 * Master switch. Set to false to silence the aspect via NoOpUsageLogger.
	 */
	private boolean enabled = true;

	/**
	 * Stable lowercase-hyphen identifier (e.g. `employee-directory`). Must
	 * NOT remain the template placeholder `replit-mvp-template` in deployment.
	 */
	private String serviceName;

	/**
	 * `prod` | `staging` | `dev`.
	 */
	private String environment = "dev";

	private int executorCorePoolSize = 1;
	private int executorMaxPoolSize = 2;
	private int executorQueueCapacity = 200;
	private int maxErrorMessageLength = 500;
	private int maxUserAgentLength = 500;
}
