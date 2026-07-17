package com.aidigital.aionboarding.mappers.common;

import com.aidigital.aionboarding.api.v1.model.LessonVisibilityV1;
import com.aidigital.aionboarding.config.ApplicationMapperConfig;
import org.mapstruct.Mapper;

@Mapper(config = ApplicationMapperConfig.class)
public interface LessonVisibilityApiMapper {

	default LessonVisibilityV1 mapLessonVisibility(String publicationStatus) {
		if (publicationStatus == null || publicationStatus.isBlank()) {
			return LessonVisibilityV1.PUBLISHED;
		}
		try {
			return LessonVisibilityV1.fromValue(publicationStatus);
		} catch (IllegalArgumentException ex) {
			return LessonVisibilityV1.PUBLISHED;
		}
	}

	default String fromLessonVisibility(LessonVisibilityV1 publicationStatus) {
		return publicationStatus == null ? null : publicationStatus.getValue();
	}
}
