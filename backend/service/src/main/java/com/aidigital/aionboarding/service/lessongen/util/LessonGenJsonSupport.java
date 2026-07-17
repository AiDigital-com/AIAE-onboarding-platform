package com.aidigital.aionboarding.service.lessongen.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class LessonGenJsonSupport {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	public Map<String, Object> extractJsonPayload(String value) {
		Map<String, Object> direct = safeJsonParse(value);
		if (direct != null) {
			return direct;
		}

		int start = value.indexOf('{');
		int end = value.lastIndexOf('}');
		if (start == -1 || end == -1 || end <= start) {
			return null;
		}
		return safeJsonParse(value.substring(start, end + 1));
	}

	Map<String, Object> safeJsonParse(String value) {
		try {
			Map<String, Object> parsed = OBJECT_MAPPER.readValue(value, new TypeReference<>() {
			});
			if (parsed == null || parsed.isEmpty()) {
				return null;
			}
			return parsed;
		} catch (Exception ex) {
			return null;
		}
	}
}
