package com.aidigital.aionboarding.service.lesson.models;

import java.util.Map;

public record AskLessonResultRecord(
    String answer,
    Map<String, Object> metadata
) { }
