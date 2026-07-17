package com.aidigital.aionboarding.service.common.mapping;

import org.springframework.stereotype.Component;

/**
 * Centralizes explicit string normalization semantics for JSON-like values and DTO fields.
 */
@Component
public class TextValueNormalizer {

    /**
     * Converts a value to a trimmed non-null string.
     *
     * @param value value to convert
     * @return trimmed string, or empty string for {@code null}
     */
    public String trimmed(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    /**
     * Converts a value to a non-null string without trimming.
     *
     * @param value value to convert
     * @return raw string value, or empty string for {@code null}
     */
    public String raw(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    /**
     * Returns the first non-blank candidate after trimming it.
     *
     * @param values candidate values in priority order
     * @return first non-blank trimmed value, or empty string
     */
    public String firstNonBlankTrimmed(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    /**
     * Returns the first non-blank candidate without trimming the returned value.
     *
     * @param values candidate values in priority order
     * @return first non-blank raw value, or empty string
     */
    public String firstNonBlankRaw(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
