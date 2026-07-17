package com.aidigital.aionboarding.mappers.common;

import com.aidigital.aionboarding.api.v1.model.PreparationStatusV1;
import com.aidigital.aionboarding.config.ApplicationMapperConfig;
import org.mapstruct.Mapper;

@Mapper(config = ApplicationMapperConfig.class)
public interface PreparationStatusApiMapper {

	default PreparationStatusV1 mapPreparationStatus(String status) {
		if (status == null || status.isBlank()) {
			return PreparationStatusV1.NOT_STARTED;
		}
		try {
			return PreparationStatusV1.fromValue(status);
		} catch (IllegalArgumentException ex) {
			return PreparationStatusV1.NOT_STARTED;
		}
	}

	default String fromPreparationStatus(PreparationStatusV1 status) {
		return status == null ? null : status.getValue();
	}
}
