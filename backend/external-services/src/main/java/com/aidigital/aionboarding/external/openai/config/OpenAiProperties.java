package com.aidigital.aionboarding.external.openai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Runtime-tunable properties for the OpenAI adapter (Responses API default).
 *
 * <p>All fields bind from the {@code app.external.openai.*} namespace.
 *
 * <p>Typical {@code application.yml} stubs:
 * <pre>
 * app:
 *   external:
 *     openai:
 *       enabled: ${OPENAI_ENABLED:false}
 *       base-url: ${OPENAI_BASE_URL:https://api.openai.com}
 *       api-key: ${OPENAI_API_KEY:}
 *       model: ${OPENAI_MODEL:gpt-4o}
 *       max-tokens: ${OPENAI_MAX_TOKENS:1024}
 * </pre>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.external.openai")
@Validated
public class OpenAiProperties {

	/**
	 * Whether the OpenAI integration is enabled.
	 */
	private boolean enabled = false;
	/**
	 * OpenAI API base URL (must not end with a slash).
	 */
	private String baseUrl = "https://api.openai.com";
	/**
	 * OpenAI API key. Never log or expose this value.
	 */
	private String apiKey = "";
	/**
	 * {@code responses} (default) or {@code chat-completions} (legacy).
	 */
	private String apiMode = "responses";
	private String responsesPath = "/v1/responses";
	private String chatCompletionsPath = "/v1/chat/completions";
	/**
	 * OpenAI model identifier.
	 */
	private String model = "gpt-4o";
	/**
	 * Maximum number of tokens to generate per request.
	 */
	private int maxTokens = 1024;

	/**
	 * Returns whether the Responses API is selected (default).
	 *
	 * @return {@code true} for Responses API, {@code false} for legacy Chat Completions
	 */
	public boolean isUseResponsesApi() {
		return apiMode == null || apiMode.isBlank() || "responses".equalsIgnoreCase(apiMode);
	}
}
