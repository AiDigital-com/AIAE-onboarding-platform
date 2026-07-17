package com.aidigital.aionboarding.external.youtube.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Runtime-tunable properties for the YouTube adapter.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.external.youtube")
@Validated
public class YoutubeProperties {

	private boolean enabled = true;
	private String transcriptLang = "";
	private boolean transcriptDebug = false;
}
