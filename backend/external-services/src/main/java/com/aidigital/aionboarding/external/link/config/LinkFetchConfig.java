package com.aidigital.aionboarding.external.link.config;

import com.aidigital.aionboarding.external.common.http.PooledRestClientFactory;
import com.aidigital.aionboarding.external.link.LinkFetchClient;
import com.aidigital.aionboarding.external.link.impl.LinkFetchClientImpl;
import com.aidigital.aionboarding.external.link.impl.StubLinkFetchClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the SSRF-resistant link-fetch adapter when enabled.
 */
@Configuration
@EnableConfigurationProperties(LinkFetchProperties.class)
public class LinkFetchConfig {

	/**
	 * Live link-fetch client when the integration is enabled.
	 *
	 * @param properties link-fetch properties
	 * @param factory    pooled HTTP client factory
	 * @return configured client
	 */
	@Bean
	@ConditionalOnProperty(prefix = "app.external.link-fetch", name = "enabled", havingValue = "true",
			matchIfMissing = true)
	public LinkFetchClient linkFetchClient(LinkFetchProperties properties, PooledRestClientFactory factory) {
		return new LinkFetchClientImpl(properties, factory);
	}

	/**
	 * Fallback stub when link fetching is explicitly disabled.
	 *
	 * @return stub client
	 */
	@Bean
	@ConditionalOnMissingBean(LinkFetchClient.class)
	public LinkFetchClient stubLinkFetchClient() {
		return new StubLinkFetchClient();
	}
}
