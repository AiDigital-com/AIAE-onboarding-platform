package com.aidigital.aionboarding.external.youtube.config;

import com.aidigital.aionboarding.external.common.http.PooledRestClientFactory;
import com.aidigital.aionboarding.external.youtube.YoutubeClient;
import com.aidigital.aionboarding.external.youtube.impl.StubYoutubeClient;
import com.aidigital.aionboarding.external.youtube.impl.YoutubeClientImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the YouTube adapter when enabled.
 */
@Configuration
@EnableConfigurationProperties(YoutubeProperties.class)
public class YoutubeConfig {

	/**
	 * Live YouTube client when the integration is enabled.
	 *
	 * @param properties YouTube properties
	 * @param factory    pooled HTTP client factory
	 * @return configured client
	 */
	@Bean
	@ConditionalOnProperty(prefix = "app.external.youtube", name = "enabled", havingValue = "true",
			matchIfMissing = true)
	public YoutubeClient youtubeClient(YoutubeProperties properties, PooledRestClientFactory factory) {
		return new YoutubeClientImpl(properties, factory);
	}

	/**
	 * Fallback stub when YouTube integration is explicitly disabled.
	 *
	 * @return stub client
	 */
	@Bean
	@ConditionalOnMissingBean(YoutubeClient.class)
	public YoutubeClient stubYoutubeClient() {
		return new StubYoutubeClient();
	}
}
