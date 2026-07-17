package com.aidigital.aionboarding.service.lessongen.model;

import java.util.Map;

public record GeneratedContentResult(
    String content,
    Map<String, Object> metadata
) {
}
