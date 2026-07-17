package com.aidigital.aionboarding.mappers.common;

import com.aidigital.aionboarding.api.v1.model.ActivityCompletionModeV1;
import com.aidigital.aionboarding.config.ApplicationMapperConfig;
import org.mapstruct.Mapper;

@Mapper(config = ApplicationMapperConfig.class)
public interface ActivityCompletionModeApiMapper {

	default ActivityCompletionModeV1 mapActivityCompletionMode(String action) {
		if (action == null || action.isBlank()) {
			return ActivityCompletionModeV1.FINISHED;
		}
		try {
			return ActivityCompletionModeV1.fromValue(action);
		} catch (IllegalArgumentException ex) {
			return ActivityCompletionModeV1.FINISHED;
		}
	}

	default String fromActivityCompletionMode(ActivityCompletionModeV1 action) {
		return action == null ? null : action.getValue();
	}
}
