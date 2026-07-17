package com.aidigital.aionboarding.mappers.material;

import com.aidigital.aionboarding.api.v1.model.MaterialSortFieldV1;
import com.aidigital.aionboarding.config.ApplicationMapperConfig;
import com.aidigital.aionboarding.service.material.models.MaterialSortField;
import org.mapstruct.Mapper;

/**
 * Converts the generated {@link MaterialSortFieldV1} query parameter to the whitelisted
 * service-layer {@link MaterialSortField}.
 */
@Mapper(config = ApplicationMapperConfig.class)
public interface MaterialSortFieldApiMapper {

    /**
     * Converts the generated sort field enum to the whitelisted service-layer sort field.
     *
     * @param sort the generated sort field value; {@code null} defaults to {@code CREATED_AT}
     * @return the equivalent {@link MaterialSortField}
     */
    default MaterialSortField toMaterialSortField(MaterialSortFieldV1 sort) {
        if (sort == null) {
            return MaterialSortField.CREATED_AT;
        }
        return switch (sort) {
            case CREATED_AT -> MaterialSortField.CREATED_AT;
            case UPDATED_AT -> MaterialSortField.UPDATED_AT;
            case TITLE -> MaterialSortField.TITLE;
            case USAGE_COUNT -> MaterialSortField.USAGE_COUNT;
        };
    }
}
