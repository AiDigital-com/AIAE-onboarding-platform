package com.aidigital.aionboarding.mappers.lesson;

import com.aidigital.aionboarding.api.v1.model.LessonSortFieldV1;
import com.aidigital.aionboarding.config.ApplicationMapperConfig;
import com.aidigital.aionboarding.service.lesson.models.LessonSortField;
import org.mapstruct.Mapper;

/**
 * Converts the generated {@link LessonSortFieldV1} query parameter to the whitelisted
 * service-layer {@link LessonSortField}.
 */
@Mapper(config = ApplicationMapperConfig.class)
public interface LessonSortFieldApiMapper {

	/**
	 * Converts the generated sort field enum to the whitelisted service-layer sort field.
	 *
	 * @param sort the generated sort field value; {@code null} defaults to {@code CREATED_AT}
	 * @return the equivalent {@link LessonSortField}
	 */
	default LessonSortField toLessonSortField(LessonSortFieldV1 sort) {
		if (sort == null) {
			return LessonSortField.CREATED_AT;
		}
		return switch (sort) {
			case CREATED_AT -> LessonSortField.CREATED_AT;
			case UPDATED_AT -> LessonSortField.UPDATED_AT;
			case TITLE -> LessonSortField.TITLE;
		};
	}
}
