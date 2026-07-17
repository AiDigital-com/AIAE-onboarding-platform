package com.aidigital.aionboarding.service.lessongen.model;

import java.util.Map;

public record ActivityRequest(
		String type,
		int count,
		Map<String, ActivityCountLimits> limits
) {

}
