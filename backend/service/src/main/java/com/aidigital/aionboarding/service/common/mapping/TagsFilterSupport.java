package com.aidigital.aionboarding.service.common.mapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Builds the JSONB array literal used by native queries to match every tag in a filter list
 * against a JSONB {@code tags} column via PostgreSQL array containment ({@code @>}).
 */
@Component
@RequiredArgsConstructor
public class TagsFilterSupport {

    private final ObjectMapper objectMapper;

    /**
     * Serializes the given tags into a JSON array literal for JSONB containment matching.
     *
     * @param tags tag values to match; {@code null} or empty means "no tag filter"
     * @return a JSON array literal (e.g. {@code ["design","ux"]}), or {@code null} when no filter applies
     */
    public String toContainmentJson(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(tags);
        } catch (JsonProcessingException e) {
            throw new AppException(ErrorReason.C002, "Invalid tag filter value.");
        }
    }
}
