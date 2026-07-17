package com.aidigital.aionboarding.mappers.common;

import com.aidigital.aionboarding.api.v1.model.LessonAssistantPresetV1;
import com.aidigital.aionboarding.config.ApplicationMapperConfig;
import com.aidigital.aionboarding.service.lesson.enums.LessonAssistantPreset;
import org.mapstruct.Mapper;

@Mapper(config = ApplicationMapperConfig.class)
public interface LessonAssistantPresetApiMapper {

    default LessonAssistantPreset mapAssistantPreset(LessonAssistantPresetV1 preset) {
        return LessonAssistantPreset.fromValue(preset == null ? null : preset.getValue());
    }

    default LessonAssistantPresetV1 fromAssistantPreset(String preset) {
        try {
            return LessonAssistantPresetV1.fromValue(LessonAssistantPreset.fromValue(preset).value());
        } catch (IllegalArgumentException ex) {
            return LessonAssistantPresetV1.REGULAR;
        }
    }
}
