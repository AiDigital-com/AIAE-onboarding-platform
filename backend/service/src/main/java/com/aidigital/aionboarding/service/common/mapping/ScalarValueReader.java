package com.aidigital.aionboarding.service.common.mapping;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Converts loosely typed projection values into stable service-layer scalar values.
 */
@Component
public class ScalarValueReader {

    /**
     * Converts a nullable value to a non-null string.
     *
     * @param raw source value
     * @return string value, or an empty string when absent
     */
    public String stringVal(Object raw) {
        return raw == null ? "" : String.valueOf(raw);
    }

    /**
     * Converts a nullable value to an integer with fallback.
     *
     * @param raw source value
     * @param fallback fallback value for absent or unparsable input
     * @return integer value
     */
    public int intVal(Object raw, int fallback) {
        if (raw instanceof Number number) {
            return number.intValue();
        }
        if (raw == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(String.valueOf(raw));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    /**
     * Converts a nullable value to a boolean with fallback.
     *
     * @param raw source value
     * @param fallback fallback value for absent input
     * @return boolean value
     */
    public boolean boolVal(Object raw, boolean fallback) {
        if (raw instanceof Boolean bool) {
            return bool;
        }
        if (raw == null) {
            return fallback;
        }
        return Boolean.parseBoolean(String.valueOf(raw));
    }

    /**
     * Converts a nullable value to a local date-time when it is already typed or ISO-8601 text.
     *
     * @param raw source value
     * @return parsed date-time, or {@code null} when absent or invalid
     */
    public LocalDateTime dateTimeVal(Object raw) {
        if (raw instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (raw == null) {
            return null;
        }
        String text = String.valueOf(raw).trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(text);
        } catch (Exception ignored) {
            return null;
        }
    }
}
