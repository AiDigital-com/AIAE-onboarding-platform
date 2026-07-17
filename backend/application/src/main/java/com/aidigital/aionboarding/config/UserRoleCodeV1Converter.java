package com.aidigital.aionboarding.config;

import com.aidigital.aionboarding.api.v1.model.UserRoleCodeV1;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Converts OpenAPI enum values from query parameters into generated enum constants.
 */
@Component
public class UserRoleCodeV1Converter implements Converter<String, UserRoleCodeV1> {

	@Override
	public UserRoleCodeV1 convert(String source) {
		if (source == null || source.isBlank()) {
			return null;
		}
		try {
			return UserRoleCodeV1.fromValue(source.trim());
		} catch (IllegalArgumentException ex) {
			return null;
		}
	}
}
