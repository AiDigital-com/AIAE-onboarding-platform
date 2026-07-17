package com.aidigital.aionboarding.mappers.common;

import com.aidigital.aionboarding.api.v1.model.LearningStatusV1;
import com.aidigital.aionboarding.config.ApplicationMapperConfig;
import org.mapstruct.Mapper;

@Mapper(config = ApplicationMapperConfig.class)
public interface LearningStatusApiMapper {

    default LearningStatusV1 mapLearningStatus(String state) {
        if (state == null || state.isBlank()) {
            return LearningStatusV1.IN_PROGRESS;
        }
        try {
            return LearningStatusV1.fromValue(state);
        } catch (IllegalArgumentException ex) {
            return LearningStatusV1.IN_PROGRESS;
        }
    }

    default String fromLearningStatus(LearningStatusV1 state) {
        return state == null ? null : state.getValue();
    }
}
