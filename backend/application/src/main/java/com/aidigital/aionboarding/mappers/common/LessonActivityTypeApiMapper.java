package com.aidigital.aionboarding.mappers.common;

import com.aidigital.aionboarding.api.v1.model.LessonActivityTypeV1;
import com.aidigital.aionboarding.config.ApplicationMapperConfig;
import org.mapstruct.Mapper;

@Mapper(config = ApplicationMapperConfig.class)
public interface LessonActivityTypeApiMapper {

    default LessonActivityTypeV1 mapLessonActivityType(String type) {
        if (type == null || type.isBlank()) {
            return LessonActivityTypeV1.QUIZ;
        }
        try {
            return LessonActivityTypeV1.fromValue(type);
        } catch (IllegalArgumentException ex) {
            return LessonActivityTypeV1.QUIZ;
        }
    }

    default String fromLessonActivityType(LessonActivityTypeV1 type) {
        return type == null ? null : type.getValue();
    }
}
