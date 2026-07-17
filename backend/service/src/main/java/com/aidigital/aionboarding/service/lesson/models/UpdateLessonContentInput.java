package com.aidigital.aionboarding.service.lesson.models;

import java.util.List;
import java.util.Optional;

public record UpdateLessonContentInput(
    Optional<String> title,
    Optional<String> contentMarkdown,
    Optional<String> contentHtml,
    Optional<List<String>> tags,
    Optional<String> coverImageStorageKey,
    Optional<String> coverImageOriginalName,
    Optional<String> coverImageMimeType
) { }
