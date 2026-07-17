package com.aidigital.aionboarding.mappers.common;

import com.aidigital.aionboarding.api.v1.model.QuizQuestionTypeV1;
import com.aidigital.aionboarding.config.ApplicationMapperConfig;
import org.mapstruct.Mapper;

@Mapper(config = ApplicationMapperConfig.class)
public interface QuizQuestionTypeApiMapper {

    default QuizQuestionTypeV1 mapQuizQuestionType(String type) {
        if (type == null || type.isBlank()) {
            return QuizQuestionTypeV1.MULTIPLE_CHOICE;
        }
        try {
            return QuizQuestionTypeV1.fromValue(type);
        } catch (IllegalArgumentException ex) {
            return QuizQuestionTypeV1.MULTIPLE_CHOICE;
        }
    }

    default String fromQuizQuestionType(QuizQuestionTypeV1 type) {
        return type == null ? null : type.getValue();
    }
}
