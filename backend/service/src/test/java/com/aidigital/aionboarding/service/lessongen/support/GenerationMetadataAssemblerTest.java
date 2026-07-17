package com.aidigital.aionboarding.service.lessongen.support;

import com.aidigital.aionboarding.external.openai.model.OpenAiResponsesUsage;
import com.aidigital.aionboarding.service.lessongen.model.LessonGenPrompt;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GenerationMetadataAssemblerTest {

	private final GenerationMetadataAssembler assembler = new GenerationMetadataAssembler();

	@Test
	void baseMetadataShouldIncludeSharedGenerationFieldsTest() {
		LessonGenPrompt prompt = new LessonGenPrompt("v1", "cache-key", "instructions", "input");
		OpenAiResponsesUsage usage = new OpenAiResponsesUsage(1, 2, 3);

		Map<String, Object> metadata = assembler.baseMetadata(prompt, "gpt-4o", "raw", "resp-1", usage);

		assertThat(metadata).containsEntry("provider", "openai");
		assertThat(metadata).containsEntry("model", "gpt-4o");
		assertThat(metadata).containsEntry("promptVersion", "v1");
		assertThat(metadata).containsEntry("promptCacheKey", "cache-key");
		assertThat(metadata).containsEntry("responseId", "resp-1");
		assertThat(metadata).containsEntry("usage", usage);
		assertThat(metadata).containsEntry("rawOutput", "raw");
	}

	@Test
	void baseMetadataShouldFallbackCacheKeyAndOmitNullRawOutputTest() {
		LessonGenPrompt prompt = new LessonGenPrompt("v1", " ", "instructions", "input");

		Map<String, Object> metadata = assembler.baseMetadata(prompt, "gpt-4o-mini", null, null, null);

		assertThat(metadata).containsEntry("promptCacheKey", "v1");
		assertThat(metadata).containsEntry("responseId", "");
		assertThat(metadata).doesNotContainKey("rawOutput");
	}
}
