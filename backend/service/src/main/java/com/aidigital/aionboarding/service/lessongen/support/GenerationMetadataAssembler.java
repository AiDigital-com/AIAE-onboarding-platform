package com.aidigital.aionboarding.service.lessongen.support;

import com.aidigital.aionboarding.external.openai.model.OpenAiResponsesUsage;
import com.aidigital.aionboarding.service.lessongen.model.LessonGenPrompt;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Builds shared metadata for OpenAI-backed generation results.
 */
@Component
public class GenerationMetadataAssembler {

	/**
	 * Builds the common provider metadata stored with generated lesson artifacts.
	 *
	 * @param prompt     prompt contract used for the request
	 * @param model      OpenAI model name
	 * @param rawOutput  raw model output, when retained
	 * @param responseId OpenAI response identifier
	 * @param usage      OpenAI token usage payload
	 * @return mutable metadata map
	 */
	public Map<String, Object> baseMetadata(LessonGenPrompt prompt, String model, String rawOutput,
	                                        String responseId, OpenAiResponsesUsage usage) {
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("provider", "openai");
		metadata.put("model", model);
		metadata.put("promptVersion", prompt.version());
		metadata.put("promptCacheKey", prompt.cacheKey() == null || prompt.cacheKey().isBlank()
				? prompt.version()
				: prompt.cacheKey());
		metadata.put("responseId", responseId != null ? responseId : "");
		metadata.put("usage", usage);
		if (rawOutput != null) {
			metadata.put("rawOutput", rawOutput);
		}
		return metadata;
	}
}
