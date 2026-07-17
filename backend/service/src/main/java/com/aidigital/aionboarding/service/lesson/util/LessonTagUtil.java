package com.aidigital.aionboarding.service.lesson.util;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Normalizes lesson, roadmap, and material tag inputs to match the Next.js source.
 */
@Component
public class LessonTagUtil {

	private static final int MAX_TAGS = 12;
	private static final int MAX_TAG_LENGTH = 48;

	public List<String> normalizeLessonTagInput(List<?> tags) {
		if (tags == null) {
			return List.of();
		}

		LinkedHashSet<String> unique = new LinkedHashSet<>();
		for (Object tag : tags) {
			if (!(tag instanceof String rawTag)) {
				continue;
			}
			String normalized = rawTag.trim().replaceAll("\\s+", " ");
			if (normalized.isEmpty()) {
				continue;
			}
			if (normalized.length() > MAX_TAG_LENGTH) {
				normalized = normalized.substring(0, MAX_TAG_LENGTH).trim();
			}
			if (normalized.isEmpty()) {
				continue;
			}
			unique.add(normalized);
			if (unique.size() >= MAX_TAGS) {
				break;
			}
		}
		return new ArrayList<>(unique);
	}
}
