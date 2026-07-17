package com.aidigital.aionboarding.mappers.common;

import com.aidigital.aionboarding.api.v1.model.LessonContentFormatV1;
import com.aidigital.aionboarding.config.ApplicationMapperConfig;
import org.mapstruct.Mapper;

@Mapper(config = ApplicationMapperConfig.class)
public interface LessonContentFormatApiMapper {

	default LessonContentFormatV1 mapLessonContentFormat(String contentFormat) {
		if (contentFormat == null || contentFormat.isBlank()) {
			return LessonContentFormatV1.MARKDOWN;
		}
		try {
			return LessonContentFormatV1.fromValue(contentFormat);
		} catch (IllegalArgumentException ex) {
			return LessonContentFormatV1.MARKDOWN;
		}
	}

	default String fromLessonContentFormat(LessonContentFormatV1 contentFormat) {
		return contentFormat == null ? null : contentFormat.getValue();
	}
}
