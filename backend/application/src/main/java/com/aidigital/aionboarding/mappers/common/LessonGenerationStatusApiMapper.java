package com.aidigital.aionboarding.mappers.common;

import com.aidigital.aionboarding.api.v1.model.LessonGenerationStatusV1;
import com.aidigital.aionboarding.config.ApplicationMapperConfig;
import org.mapstruct.Mapper;

@Mapper(config = ApplicationMapperConfig.class)
public interface LessonGenerationStatusApiMapper {

	default LessonGenerationStatusV1 mapLessonGenerationStatus(String status) {
		if (status == null || status.isBlank()) {
			return LessonGenerationStatusV1.DRAFT;
		}
		try {
			return LessonGenerationStatusV1.fromValue(status);
		} catch (IllegalArgumentException ex) {
			return LessonGenerationStatusV1.DRAFT;
		}
	}

	default String fromLessonGenerationStatus(LessonGenerationStatusV1 status) {
		return status == null ? null : status.getValue();
	}
}
