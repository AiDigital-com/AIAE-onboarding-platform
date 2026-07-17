package com.aidigital.aionboarding.service.lesson.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OpenAiLessonProperties.class)
public class LessonServiceConfig {
}
