package com.aidigital.aionboarding.mappers.common;

import com.aidigital.aionboarding.api.v1.model.MaterialTypeV1;
import com.aidigital.aionboarding.config.ApplicationMapperConfig;
import org.mapstruct.Mapper;

@Mapper(config = ApplicationMapperConfig.class)
public interface MaterialTypeApiMapper {

    default MaterialTypeV1 mapMaterialType(String type) {
        if (type == null || type.isBlank()) {
            return MaterialTypeV1.LINK;
        }
        try {
            return MaterialTypeV1.fromValue(type);
        } catch (IllegalArgumentException ex) {
            return MaterialTypeV1.LINK;
        }
    }

    default String fromMaterialType(MaterialTypeV1 type) {
        return type == null ? null : type.getValue();
    }
}
