package com.aidigital.aionboarding.external.heygen.config;

import com.aidigital.aionboarding.external.common.http.PooledRestClientFactory;
import com.aidigital.aionboarding.external.heygen.HeyGenClient;
import com.aidigital.aionboarding.external.heygen.impl.HeyGenClientImpl;
import com.aidigital.aionboarding.external.heygen.impl.StubHeyGenClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the HeyGen adapter when enabled and an API key is present.
 */
@Configuration
@EnableConfigurationProperties(HeyGenProperties.class)
public class HeyGenConfig {

	/**
	 * Live HeyGen client when enabled with a non-blank API key.
	 *
	 * @param properties HeyGen properties
	 * @param factory    pooled HTTP client factory
	 * @return configured client
	 */
	@Bean
	@ConditionalOnProperty(prefix = "app.external.heygen", name = "enabled", havingValue = "true")
	public HeyGenClient heyGenClient(HeyGenProperties properties, PooledRestClientFactory factory) {
		if (!properties.hasApiKey()) {
			return new StubHeyGenClient();
		}
		return new HeyGenClientImpl(properties, factory);
	}

	/**
	 * Fallback stub when HeyGen is disabled or the API key is absent.
	 *
	 * @return stub client
	 */
	@Bean
	@ConditionalOnMissingBean(HeyGenClient.class)
	public HeyGenClient stubHeyGenClient() {
		return new StubHeyGenClient();
	}
}
