package com.aidigital.aionboarding.mappers.common;

import com.aidigital.aionboarding.api.v1.model.ProgressStatusV1;
import com.aidigital.aionboarding.config.ApplicationMapperConfig;
import org.mapstruct.Mapper;

@Mapper(config = ApplicationMapperConfig.class)
public interface ProgressStatusApiMapper {

	default ProgressStatusV1 mapProgressStatus(String status) {
		if (status == null || status.isBlank()) {
			return ProgressStatusV1.NOT_STARTED;
		}
		try {
			return ProgressStatusV1.fromValue(status);
		} catch (IllegalArgumentException ex) {
			return ProgressStatusV1.NOT_STARTED;
		}
	}

	default String fromProgressStatus(ProgressStatusV1 status) {
		return status == null ? null : status.getValue();
	}
}
