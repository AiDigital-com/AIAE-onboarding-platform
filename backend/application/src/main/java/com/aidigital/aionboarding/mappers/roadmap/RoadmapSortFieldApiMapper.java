package com.aidigital.aionboarding.mappers.roadmap;

import com.aidigital.aionboarding.api.v1.model.RoadmapSortFieldV1;
import com.aidigital.aionboarding.config.ApplicationMapperConfig;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapSortField;
import org.mapstruct.Mapper;

/**
 * Converts the generated {@link RoadmapSortFieldV1} query parameter to the whitelisted
 * service-layer {@link RoadmapSortField}.
 */
@Mapper(config = ApplicationMapperConfig.class)
public interface RoadmapSortFieldApiMapper {

	/**
	 * Converts the generated sort field enum to the whitelisted service-layer sort field.
	 *
	 * @param sort the generated sort field value; {@code null} defaults to {@code CREATED_AT}
	 * @return the equivalent {@link RoadmapSortField}
	 */
	default RoadmapSortField toRoadmapSortField(RoadmapSortFieldV1 sort) {
		if (sort == null) {
			return RoadmapSortField.CREATED_AT;
		}
		return switch (sort) {
			case CREATED_AT -> RoadmapSortField.CREATED_AT;
			case UPDATED_AT -> RoadmapSortField.UPDATED_AT;
			case TITLE -> RoadmapSortField.TITLE;
		};
	}
}
