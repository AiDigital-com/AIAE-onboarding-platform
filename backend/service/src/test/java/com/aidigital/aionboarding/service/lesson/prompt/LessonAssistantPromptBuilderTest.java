package com.aidigital.aionboarding.service.lesson.prompt;

import com.aidigital.aionboarding.service.common.mapping.JsonMapReader;
import com.aidigital.aionboarding.service.common.mapping.TextValueNormalizer;
import com.aidigital.aionboarding.service.lesson.enums.LessonAssistantPreset;
import com.aidigital.aionboarding.service.lesson.models.ChatTurn;
import com.aidigital.aionboarding.service.lesson.util.LessonTextUtil;
import com.aidigital.aionboarding.service.lesson.util.OpenAiPromptCacheUtil;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LessonAssistantPromptBuilderTest {

	private final LessonAssistantPromptBuilder builder = new LessonAssistantPromptBuilder(
			new LessonTextUtil(), new OpenAiPromptCacheUtil(), new TextValueNormalizer(), new JsonMapReader());

	private Map<String, Object> lesson() {
		Map<String, Object> lesson = new LinkedHashMap<>();
		lesson.put("id", 42L);
		lesson.put("title", "Onboarding basics");
		lesson.put("description", "An intro lesson");
		lesson.put("contentMarkdown", "# Welcome\nThis is the lesson body.");
		lesson.put("updatedAt", "2026-01-01T00:00:00Z");
		return lesson;
	}

	@Nested
	class BuildLessonAssistantPrompt {

		@Test
		void shouldReturnOnlyTheQuestionWhenChainingOntoAPriorResponseTest() {
			// Given: a follow-up call where the server already retains lesson/history context
			Map<String, Object> lesson = lesson();

			// When:
			Map<String, Object> prompt = builder.buildLessonAssistantPrompt(
					lesson, Map.of(), "What about part 2?", List.of(), LessonAssistantPreset.REGULAR, true);

			// Then: no lesson content, source assets, or history is resent
			assertThat(prompt.get("input")).isEqualTo("User question: What about part 2?");
			assertThat(prompt.get("fileInputs")).isEqualTo(List.of());
			assertThat(prompt.get("version")).isEqualTo(LessonAssistantPromptBuilder.LESSON_ASSISTANT_PROMPT_VERSION);
		}

		@Test
		void shouldIncludeStandardModeInstructionsAndFullLessonContextWhenNotAFollowUpTest() {
			// Given: a first-turn question with no small-portions preset
			Map<String, Object> lesson = lesson();

			// When:
			Map<String, Object> prompt = builder.buildLessonAssistantPrompt(
					lesson, Map.of(), "What is this lesson about?", List.of(), LessonAssistantPreset.REGULAR, false);

			// Then:
			assertThat((String) prompt.get("instructions")).contains("# Mode: Standard").doesNotContain("Small " +
					"portions (ACTIVE)");
			assertThat((String) prompt.get("input"))
					.contains("<lesson_title>Onboarding basics</lesson_title>")
					.contains("This is the lesson body.")
					.contains("User question: What is this lesson about?");
		}

		@Test
		void shouldIncludeSmallPortionsInstructionsWhenPresetIsSmallPortionsTest() {
			// Given:
			Map<String, Object> lesson = lesson();

			// When:
			Map<String, Object> prompt = builder.buildLessonAssistantPrompt(
					lesson, Map.of(), "Explain it", List.of(), LessonAssistantPreset.SMALL_PORTIONS, false);

			// Then:
			assertThat((String) prompt.get("instructions")).contains("# Mode: Small portions (ACTIVE)");
		}

		@Test
		void shouldIncludeRecentConversationHistoryWhenPresentTest() {
			// Given: prior turns supplied by the client (not yet chained server-side)
			Map<String, Object> lesson = lesson();
			List<ChatTurn> history = List.of(
					new ChatTurn("user", "What is this about?"),
					new ChatTurn("assistant", "It's about onboarding."));

			// When:
			Map<String, Object> prompt = builder.buildLessonAssistantPrompt(
					lesson, Map.of(), "Tell me more", history, LessonAssistantPreset.REGULAR, false);

			// Then:
			assertThat((String) prompt.get("input"))
					.contains("Recent conversation:")
					.contains("user: What is this about?")
					.contains("assistant: It's about onboarding.");
		}

		@Test
		void shouldFallBackToPlaceholderTextWhenNoLessonContentOrSourceMaterialExistsTest() {
			// Given: a lesson with no content and no prepared materials
			Map<String, Object> lesson = new LinkedHashMap<>();
			lesson.put("title", "Empty lesson");

			// When:
			Map<String, Object> prompt = builder.buildLessonAssistantPrompt(
					lesson, Map.of(), "Anything here?", List.of(), LessonAssistantPreset.REGULAR, false);

			// Then:
			assertThat((String) prompt.get("input"))
					.contains("No lesson text available.")
					.contains("No original source material text available.")
					.contains("No extra lesson assets.");
		}
	}

	@Nested
	class CollectOpenAIFileInputs {

		@Test
		void shouldDedupeByFileIdAndSkipAttachmentsWithoutASupportedTypeTest() {
			// Given: two materials, one repeating the same OpenAI file id, one with an
			// unsupported MIME type and no file id
			Map<String, Object> attachmentA = Map.of("openaiFileId", "file-1", "mimeType", "application/pdf");
			Map<String, Object> attachmentDuplicate = Map.of("openaiFileId", "file-1", "mimeType", "application/pdf");
			Map<String, Object> attachmentUnsupported = Map.of("openaiFileId", "", "mimeType", "video/mp4");
			Map<String, Object> materialOne = Map.of("attachments", List.of(attachmentA, attachmentUnsupported));
			Map<String, Object> materialTwo = Map.of("attachments", List.of(attachmentDuplicate));
			Map<String, Object> preparedMaterials = Map.of("materials", List.of(materialOne, materialTwo));

			// When:
			List<Map<String, Object>> fileInputs = builder.collectOpenAIFileInputs(preparedMaterials);

			// Then:
			assertThat(fileInputs).containsExactly(Map.of("type", "input_file", "file_id", "file-1"));
		}

		@Test
		void shouldReturnEmptyListWhenPreparedMaterialsIsNullTest() {
			// When:
			List<Map<String, Object>> fileInputs = builder.collectOpenAIFileInputs(null);

			// Then:
			assertThat(fileInputs).isEmpty();
		}
	}

	@Nested
	class GetOpenAIFileInputType {

		@Test
		void shouldReturnInputImageForImageMimeTypesTest() {
			// When-Then:
			assertThat(builder.getOpenAIFileInputType(Map.of("mimeType", "image/png"))).isEqualTo("input_image");
		}

		@Test
		void shouldReturnInputFileForPdfTextJsonAndMarkdownMimeTypesTest() {
			// When-Then:
			assertThat(builder.getOpenAIFileInputType(Map.of("mimeType", "application/pdf"))).isEqualTo("input_file");
			assertThat(builder.getOpenAIFileInputType(Map.of("mimeType", "text/plain"))).isEqualTo("input_file");
			assertThat(builder.getOpenAIFileInputType(Map.of("mimeType", "application/json"))).isEqualTo("input_file");
		}

		@Test
		void shouldReturnInputFileWhenMimeTypeIsGenericButFileNameHasASupportedExtensionTest() {
			// Given: a blank/generic MIME type but a recognizable extension
			Map<String, Object> attachment = Map.of("mimeType", "application/octet-stream", "name", "notes.docx");

			// When-Then:
			assertThat(builder.getOpenAIFileInputType(attachment)).isEqualTo("input_file");
		}

		@Test
		void shouldReturnNullForAnUnsupportedFileTest() {
			// When-Then:
			assertThat(builder.getOpenAIFileInputType(Map.of("mimeType", "video/mp4", "name", "clip.mp4"))).isNull();
		}
	}

	@Nested
	class FormatMaterial {

		@Test
		void shouldIncludeAllPresentSectionsInSourceOrderTest() {
			// Given: a material with description, extracted text, a YouTube URL with transcript,
			// a link asset, and an attachment
			Map<String, Object> material = new LinkedHashMap<>();
			material.put("sourceNumber", 1);
			material.put("title", "Company handbook");
			material.put("description", "Policies and procedures");
			material.put("text", "Full extracted text body");
			material.put("youtubeUrls", List.of("https://youtube.com/watch?v=abc"));
			material.put("youtubeTranscripts", List.of(Map.of(
					"url", "https://youtube.com/watch?v=abc", "preparedText", "Transcript body")));
			material.put("links", List.of("https://example.com"));
			material.put("linkAssets", List.of(Map.of(
					"title", "Example site", "url", "https://example.com", "siteName", "Example")));
			material.put("attachments", List.of(Map.of(
					"name", "policy.pdf", "kind", "file", "mimeType", "application/pdf", "openaiFileId", "file-9")));

			// When:
			String formatted = builder.formatMaterial(material);

			// Then:
			assertThat(formatted)
					.contains("Source 1: Company handbook")
					.contains("Description:\nPolicies and procedures")
					.contains("Extracted text:\nFull extracted text body")
					.contains("YouTube URLs:\n- https://youtube.com/watch?v=abc")
					.contains("YouTube transcript for https://youtube.com/watch?v=abc:\nTranscript body")
					.contains("Links:\n- https://example.com")
					.contains("Web asset: Example site")
					.contains("Attachments:")
					.contains("policy.pdf (file, application/pdf, OpenAI file attached as file-9)");
		}

		@Test
		void shouldOmitAbsentSectionsAndSayTranscriptUnavailableWhenTranscriptTextIsBlankTest() {
			// Given: only a title and a YouTube transcript entry with no prepared text
			Map<String, Object> material = new LinkedHashMap<>();
			material.put("sourceNumber", 2);
			material.put("title", "Bare material");
			material.put("youtubeTranscripts", List.of(Map.of("url", "https://youtube.com/watch?v=missing")));

			// When:
			String formatted = builder.formatMaterial(material);

			// Then:
			assertThat(formatted)
					.contains("Source 2: Bare material")
					.contains("YouTube transcript unavailable for https://youtube.com/watch?v=missing.")
					.doesNotContain("Description:")
					.doesNotContain("Attachments:");
		}
	}

	@Nested
	class FormatLinkAsset {

		@Test
		void shouldUseUntitledLinkWhenNeitherTitleNorUrlIsPresentTest() {
			// When:
			String formatted = builder.formatLinkAsset(Map.of());

			// Then:
			assertThat(formatted).isEqualTo("Web asset: Untitled link");
		}

		@Test
		void shouldIncludeAllPresentFieldsTest() {
			// Given:
			Map<String, Object> linkAsset = Map.of(
					"title", "Docs",
					"url", "https://docs.example.com",
					"siteName", "Example Docs",
					"description", "Reference documentation",
					"extractedText", "Body text");

			// When:
			String formatted = builder.formatLinkAsset(linkAsset);

			// Then:
			assertThat(formatted)
					.contains("Web asset: Docs")
					.contains("URL: https://docs.example.com")
					.contains("Site: Example Docs")
					.contains("Description: Reference documentation")
					.contains("Extracted text:\nBody text");
		}
	}

	@Nested
	class FormatLessonAssets {

		@Test
		void shouldReturnPlaceholderWhenThereAreNoAssetsTest() {
			// When-Then:
			assertThat(builder.formatLessonAssets(List.of())).isEqualTo("No extra lesson assets.");
		}

		@Test
		void shouldNumberAssetsAndFallBackThroughTitleCandidatesTest() {
			// Given: one asset with an explicit title, one falling back to originalName, and one
			// with metadata-derived extracted text
			Map<String, Object> named = Map.of("title", "Org chart", "kind", "image");
			Map<String, Object> fallsBackToOriginalName = new LinkedHashMap<>();
			fallsBackToOriginalName.put("originalName", "raw-file.txt");
			fallsBackToOriginalName.put("kind", "file");
			fallsBackToOriginalName.put("metadata", Map.of("extractedText", "Extracted body"));

			// When:
			String formatted = builder.formatLessonAssets(List.of(named, fallsBackToOriginalName));

			// Then:
			assertThat(formatted)
					.contains("Asset 1: Org chart")
					.contains("Kind: image")
					.contains("Asset 2: raw-file.txt")
					.contains("Extracted text:\nExtracted body");
		}
	}
}
