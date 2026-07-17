package com.aidigital.aionboarding.mappers.learning;

import com.aidigital.aionboarding.api.v1.model.LessonEnrollmentV1;
import com.aidigital.aionboarding.config.ApplicationMapperConfig;
import com.aidigital.aionboarding.service.learning.models.LessonEnrollmentRecord;
import org.mapstruct.Mapper;

@Mapper(config = ApplicationMapperConfig.class)
public interface LessonEnrollmentApiMapper {

    LessonEnrollmentV1 toLessonEnrollmentV1(LessonEnrollmentRecord enrollment);
}
