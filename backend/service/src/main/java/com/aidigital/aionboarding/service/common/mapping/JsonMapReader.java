package com.aidigital.aionboarding.service.common.mapping;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads JSON-like values from untyped maps while filtering unexpected shapes.
 */
@Component
public class JsonMapReader {

	/**
	 * Returns a string-keyed copy of a raw map, or an empty map for non-map values.
	 *
	 * @param value raw value
	 * @return map containing only string-keyed entries
	 */
	public Map<String, Object> map(Object value) {
		if (!(value instanceof Map<?, ?> rawMap)) {
			return Map.of();
		}
		Map<String, Object> result = new LinkedHashMap<>();
		for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
			if (entry.getKey() instanceof String key) {
				result.put(key, entry.getValue());
			}
		}
		return result;
	}

	/**
	 * Returns string-keyed maps from a raw collection, skipping non-map entries.
	 *
	 * @param value raw value
	 * @return filtered map list in original order
	 */
	public List<Map<String, Object>> mapList(Object value) {
		if (!(value instanceof Collection<?> collection)) {
			return List.of();
		}
		List<Map<String, Object>> result = new ArrayList<>();
		for (Object item : collection) {
			if (item instanceof Map<?, ?>) {
				result.add(map(item));
			}
		}
		return result;
	}

	/**
	 * Returns non-blank string entries from a raw collection, preserving order and value text.
	 *
	 * @param value raw value
	 * @return filtered strings in original order
	 */
	public List<String> stringList(Object value) {
		if (!(value instanceof Collection<?> collection)) {
			return List.of();
		}
		List<String> result = new ArrayList<>();
		for (Object item : collection) {
			if (item instanceof String stringValue && !stringValue.isBlank()) {
				result.add(stringValue);
			}
		}
		return result;
	}
}
