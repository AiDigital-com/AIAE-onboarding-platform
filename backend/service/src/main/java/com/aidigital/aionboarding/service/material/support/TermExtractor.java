package com.aidigital.aionboarding.service.material.support;

import com.aidigital.aionboarding.service.material.models.MaterialPreparationItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts candidate capitalized terms from prepared material text.
 */
@Component
@RequiredArgsConstructor
public class TermExtractor {

	static final Pattern CAPITALIZED_TERM = Pattern.compile(
			"\\b([A-Z][a-zA-Z]{1,}(?:\\s+[A-Z][a-zA-Z]+)*)\\b");
	static final Set<String> IGNORED_TERMS = Set.of(
			"The", "A", "An", "In", "Of", "On", "At", "To", "For", "And", "Or", "But", "With",
			"Is", "Are", "Was", "Were", "Be", "Been", "Being", "Have", "Has", "Had",
			"Do", "Does", "Did", "Will", "Would", "Could", "Should", "May", "Might");
	static final int MAX_TERMS = 12;

	/**
	 * Extracts up to {@value #MAX_TERMS} capitalized candidate terms from the given materials.
	 *
	 * @param materials prepared material items to scan
	 * @return list of unique candidate terms in discovery order, capped at {@value #MAX_TERMS}
	 */
	public List<String> extractTerms(List<MaterialPreparationItem> materials) {
		Set<String> terms = new LinkedHashSet<>();
		for (MaterialPreparationItem material : materials) {
			if (terms.size() >= MAX_TERMS) {
				break;
			}
			String sourceText = buildSourceText(material);
			Matcher matcher = CAPITALIZED_TERM.matcher(sourceText);
			while (matcher.find() && terms.size() < MAX_TERMS) {
				String candidate = matcher.group(1).trim();
				if (!IGNORED_TERMS.contains(candidate)) {
					terms.add(candidate);
				}
			}
		}
		return new ArrayList<>(terms);
	}

	String buildSourceText(MaterialPreparationItem material) {
		Map<String, Object> data = material.data();
		StringBuilder builder = new StringBuilder();
		appendNormalized(builder, data.get("title"));
		appendNormalized(builder, data.get("description"));
		appendNormalized(builder, data.get("text"));
		appendLinkText(builder, data.get("linkAssets"));
		appendTranscriptText(builder, data.get("youtubeTranscripts"));
		return builder.toString();
	}

	void appendNormalized(StringBuilder builder, Object value) {
		if (value == null) {
			return;
		}
		String text = String.valueOf(value).trim();
		if (!text.isBlank()) {
			builder.append(text).append("\n");
		}
	}

	@SuppressWarnings("unchecked")
	void appendLinkText(StringBuilder builder, Object linkAssets) {
		if (!(linkAssets instanceof List<?> list)) {
			return;
		}
		for (Object item : list) {
			if (item instanceof Map<?, ?> map) {
				appendNormalized(builder, map.get("extractedText"));
			}
		}
	}

	@SuppressWarnings("unchecked")
	void appendTranscriptText(StringBuilder builder, Object transcripts) {
		if (!(transcripts instanceof List<?> list)) {
			return;
		}
		for (Object item : list) {
			if (item instanceof Map<?, ?> map) {
				appendNormalized(builder, map.get("preparedText"));
			}
		}
	}
}
