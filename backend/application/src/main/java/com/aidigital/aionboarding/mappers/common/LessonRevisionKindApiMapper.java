package com.aidigital.aionboarding.mappers.common;

import com.aidigital.aionboarding.api.v1.model.LessonRevisionKindV1;
import com.aidigital.aionboarding.config.ApplicationMapperConfig;
import org.mapstruct.Mapper;

@Mapper(config = ApplicationMapperConfig.class)
public interface LessonRevisionKindApiMapper {

	default LessonRevisionKindV1 mapLessonRevisionKind(String changeScope) {
		if (changeScope == null || changeScope.isBlank()) {
			return LessonRevisionKindV1.SUBSTANTIAL;
		}
		try {
			return LessonRevisionKindV1.fromValue(changeScope);
		} catch (IllegalArgumentException ex) {
			return LessonRevisionKindV1.SUBSTANTIAL;
		}
	}

	default String fromLessonRevisionKind(LessonRevisionKindV1 changeScope) {
		return changeScope == null ? null : changeScope.getValue();
	}
}
