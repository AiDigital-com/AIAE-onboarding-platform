package com.aidigital.aionboarding.mappers.common;

import com.aidigital.aionboarding.api.v1.model.SortDirectionV1;
import com.aidigital.aionboarding.config.ApplicationMapperConfig;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Sort;

/**
 * Converts the generated {@link SortDirectionV1} query parameter to Spring Data's {@link Sort.Direction}.
 */
@Mapper(config = ApplicationMapperConfig.class)
public interface SortDirectionApiMapper {

	/**
	 * Converts the generated sort direction enum to the Spring Data equivalent.
	 *
	 * @param direction the generated sort direction value; {@code null} defaults to {@code DESC}
	 * @return the equivalent {@link Sort.Direction}
	 */
	default Sort.Direction toSortDirection(SortDirectionV1 direction) {
		return direction == SortDirectionV1.ASC ? Sort.Direction.ASC : Sort.Direction.DESC;
	}
}
