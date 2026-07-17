package com.aidigital.aionboarding.service.material.support;

import com.aidigital.aionboarding.service.common.mapping.JsonMapReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Counts the text that contributes to material preparation limits and stats.
 */
@Component
@RequiredArgsConstructor
public class MaterialTextSizer {

	private final JsonMapReader jsonMapReader;

	/**
	 * Counts material title, description, body text, YouTube transcript text, and parsed link text.
	 *
	 * @param material prepared material map
	 * @return combined text character count
	 */
	public int count(Map<String, Object> material) {
		if (material == null) {
			return 0;
		}
		String youtubeText = jsonMapReader.mapList(material.get("youtubeTranscripts")).stream()
				.map(transcript -> String.valueOf(transcript.getOrDefault("preparedText", "")))
				.reduce((a, b) -> a + "\n" + b)
				.orElse("");
		String linkText = jsonMapReader.mapList(material.get("linkAssets")).stream()
				.map(link -> String.valueOf(link.getOrDefault("extractedText", "")))
				.reduce((a, b) -> a + "\n" + b)
				.orElse("");
		return String.join("\n",
				String.valueOf(material.getOrDefault("title", "")),
				String.valueOf(material.getOrDefault("description", "")),
				String.valueOf(material.getOrDefault("text", "")),
				youtubeText,
				linkText).length();
	}
}
