package com.aidigital.aionboarding.config;

import com.aidigital.aionboarding.api.v1.model.DashboardPeriodV1;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Converts OpenAPI enum values from query parameters into generated enum constants.
 */
@Component
public class DashboardPeriodV1Converter implements Converter<String, DashboardPeriodV1> {

	@Override
	public DashboardPeriodV1 convert(String source) {
		if (source == null || source.isBlank()) {
			return DashboardPeriodV1.MONTH;
		}
		try {
			return DashboardPeriodV1.fromValue(source.trim());
		} catch (IllegalArgumentException ex) {
			return DashboardPeriodV1.MONTH;
		}
	}
}
