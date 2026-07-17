package com.aidigital.aionboarding.mappers.common;

import com.aidigital.aionboarding.api.v1.model.LessonPublishActionV1;
import com.aidigital.aionboarding.config.ApplicationMapperConfig;
import org.mapstruct.Mapper;

@Mapper(config = ApplicationMapperConfig.class)
public interface LessonPublishActionApiMapper {

    default String from(LessonPublishActionV1 action) {
        return action == null ? null : action.getValue();
    }
}
