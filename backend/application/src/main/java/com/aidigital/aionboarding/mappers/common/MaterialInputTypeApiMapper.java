package com.aidigital.aionboarding.mappers.common;

import com.aidigital.aionboarding.api.v1.model.MaterialInputTypeV1;
import com.aidigital.aionboarding.config.ApplicationMapperConfig;
import org.mapstruct.Mapper;

@Mapper(config = ApplicationMapperConfig.class)
public interface MaterialInputTypeApiMapper {

	default MaterialInputTypeV1 mapMaterialInputType(String type) {
		if (type == null || type.isBlank()) {
			return MaterialInputTypeV1.LINK;
		}
		try {
			return MaterialInputTypeV1.fromValue(type);
		} catch (IllegalArgumentException ex) {
			return MaterialInputTypeV1.LINK;
		}
	}

	default String fromMaterialInputType(MaterialInputTypeV1 type) {
		return type == null ? null : type.getValue();
	}
}
