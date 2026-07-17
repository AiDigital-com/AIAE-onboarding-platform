package com.aidigital.aionboarding.service.lessongen.model;

import java.util.List;
import java.util.Map;

/**
 * @param store whether OpenAI should retain this response so a later turn can chain onto it via
 *     {@code previousResponseId}; {@code false} for one-shot generation calls
 * @param previousResponseId OpenAI response id to continue from, or {@code null} to start a new
 *     conversation
 */
public record LessonGenPrompt(
    String version,
    String cacheKey,
    String instructions,
    String input,
    List<Map<String, Object>> fileInputs,
    boolean store,
    String previousResponseId
) {

    public LessonGenPrompt(String version, String cacheKey, String instructions, String input) {
        this(version, cacheKey, instructions, input, List.of());
    }

    public LessonGenPrompt(
        String version, String cacheKey, String instructions, String input, List<Map<String, Object>> fileInputs
    ) {
        this(version, cacheKey, instructions, input, fileInputs, false, null);
    }
}
