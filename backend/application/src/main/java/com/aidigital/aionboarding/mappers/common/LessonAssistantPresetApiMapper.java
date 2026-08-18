package com.aidigital.aionboarding.mappers.common;

import com.aidigital.aionboarding.api.v1.model.LessonAssistantPresetV1;
import com.aidigital.aionboarding.config.ApplicationMapperConfig;
import com.aidigital.aionboarding.service.lesson.enums.LessonAssistantPreset;
import org.mapstruct.Mapper;

@Mapper(config = ApplicationMapperConfig.class)
public interface LessonAssistantPresetApiMapper {

    /**
     * Converts an API preset value to the service enum, defaulting to {@code REGULAR} when the
     * value is missing or unrecognized.
     *
     * @param preset the requested preset, or {@code null}
     * @return matching preset, defaulting to {@code REGULAR}
     */
    default LessonAssistantPreset mapAssistantPreset(LessonAssistantPresetV1 preset) {
        return resolvePreset(preset == null ? null : preset.getValue());
    }

    default LessonAssistantPresetV1 fromAssistantPreset(String preset) {
        try {
            return LessonAssistantPresetV1.fromValue(resolvePreset(preset).value());
        } catch (IllegalArgumentException ex) {
            return LessonAssistantPresetV1.REGULAR;
        }
    }

    /**
     * Resolves a preset's wire/storage value to its enum constant, defaulting to
     * {@code REGULAR} when the value is missing or unrecognized. Kept as a mapper default
     * method, rather than a static factory on {@link LessonAssistantPreset} itself, so backend
     * beans never carry hand-written static methods.
     *
     * @param value the wire/storage value, possibly {@code null}
     * @return matching preset, defaulting to {@code REGULAR}
     */
    default LessonAssistantPreset resolvePreset(String value) {
        for (LessonAssistantPreset candidate : LessonAssistantPreset.values()) {
            if (candidate.value().equalsIgnoreCase(value)) {
                return candidate;
            }
        }
        return LessonAssistantPreset.REGULAR;
    }
}
