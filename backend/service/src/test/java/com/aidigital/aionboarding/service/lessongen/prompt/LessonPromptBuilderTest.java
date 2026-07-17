package com.aidigital.aionboarding.service.lessongen.prompt;

import com.aidigital.aionboarding.service.common.mapping.JsonMapReader;
import com.aidigital.aionboarding.service.common.mapping.TextValueNormalizer;
import com.aidigital.aionboarding.service.lessongen.model.LessonGenPrompt;
import com.aidigital.aionboarding.service.material.models.DuplicateTitle;
import com.aidigital.aionboarding.service.material.models.DuplicateUrl;
import com.aidigital.aionboarding.service.material.models.MaterialPreparationItem;
import com.aidigital.aionboarding.service.material.models.OverlapNotes;
import com.aidigital.aionboarding.service.material.models.PreparationStats;
import com.aidigital.aionboarding.service.material.models.PreparedMaterialsResult;
import com.aidigital.aionboarding.service.material.models.SignalItem;
import com.aidigital.aionboarding.service.material.models.SignalNotes;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LessonPromptBuilderTest {

	private final LessonPromptBuilder builder = new LessonPromptBuilder(new TextValueNormalizer(),
			new JsonMapReader());

	@Nested
	class BuildTheoreticalLessonPromptConvenienceOverload {

		@Test
		void shouldDelegateToFullOverloadWithFixedDefaultsTest() {
			// Given: a prepared-materials result with no materials and no extracted terms, so
			// the convenience overload's fixed defaults drive every setting line
			PreparedMaterialsResult preparedMaterials = new PreparedMaterialsResult(
					List.of(),
					List.of(),
					List.of(),
					new SignalNotes(List.of(), List.of()),
					new OverlapNotes(List.of(), List.of()),
					new PreparationStats(0, 0));

			// When:
			LessonGenPrompt prompt = builder.buildTheoreticalLessonPrompt(preparedMaterials);

			// Then:
			assertThat(prompt.version()).isEqualTo(LessonPromptConstants.LESSON_PROMPT_VERSION);
			assertThat(prompt.cacheKey()).isEqualTo(LessonPromptConstants.LESSON_PROMPT_VERSION);
			assertThat(prompt.instructions()).isEqualTo(LessonPromptConstants.LESSON_INSTRUCTIONS);
			assertThat(prompt.fileInputs()).isEqualTo(List.of());
			assertThat(prompt.input())
					.contains("Create one coherent theoretical lesson from the user-provided topic/instructions.")
					.contains("- Source mode: prompt-only topic")
					.contains("- Depth: standard")
					.contains("- Tone: clear")
					.contains("- Desired format: structured theoretical lesson")
					.contains("- Extra user instructions: none")
					.contains("- Candidate key terms: No candidate terms extracted.")
					.contains("Selected source materials: none. Use the extra user instructions as the topic.");
		}
	}

	@Nested
	class BuildTheoreticalLessonPrompt {

		@Test
		void shouldBuildPromptWithSourceMaterialsUserInstructionsAndFileInputsTest() {
			// Given: two prepared materials, custom generation settings, user instructions, and
			// OpenAI file inputs to attach
			Map<String, Object> materialOne = new LinkedHashMap<>();
			materialOne.put("sourceNumber", 1);
			materialOne.put("title", "Doc A");
			materialOne.put("text", "Body text A");
			Map<String, Object> materialTwo = new LinkedHashMap<>();
			materialTwo.put("sourceNumber", 2);
			materialTwo.put("title", "Doc B");
			materialTwo.put("text", "Body text B");

			PreparedMaterialsResult preparedMaterials = new PreparedMaterialsResult(
					List.of(
							new MaterialPreparationItem(1L, 1, materialOne),
							new MaterialPreparationItem(2L, 2, materialTwo)),
					List.of(),
					List.of("Onboarding", "SLA"),
					new SignalNotes(List.of(), List.of()),
					new OverlapNotes(List.of(), List.of()),
					new PreparationStats(2, 1234));
			List<Map<String, Object>> fileInputs = List.of(Map.of("type", "input_file", "file_id", "file-1"));

			// When:
			LessonGenPrompt prompt = builder.buildTheoreticalLessonPrompt(
					preparedMaterials, "Focus on onboarding SLAs.", "deep", "formal", "step-by-step guide",
					fileInputs);

			// Then:
			assertThat(prompt.version()).isEqualTo(LessonPromptConstants.LESSON_PROMPT_VERSION);
			assertThat(prompt.cacheKey()).isEqualTo(LessonPromptConstants.LESSON_PROMPT_VERSION);
			assertThat(prompt.instructions()).isEqualTo(LessonPromptConstants.LESSON_INSTRUCTIONS);
			assertThat(prompt.fileInputs()).isEqualTo(fileInputs);
			assertThat(prompt.input())
					.contains("Create one coherent theoretical lesson from the selected materials.")
					.contains("- Source mode: selected materials provided")
					.contains("- Depth: deep")
					.contains("- Tone: formal")
					.contains("- Desired format: step-by-step guide")
					.contains("- Extra user instructions: Focus on onboarding SLAs.")
					.contains("- Materials count: 2")
					.contains("- Combined extracted text characters: 1234")
					.contains("- Candidate key terms: Onboarding, SLA")
					.contains("Selected source materials:")
					.contains("Source 1: Doc A\n\nExtracted text:\nBody text A\n\n---\n\nSource 2: Doc B\n\nExtracted " +
							"text:\nBody text B");
		}

		@Test
		void shouldTreatNullFileInputsAsEmptyListTest() {
			// Given: a minimal prepared-materials result and fileInputs = null
			PreparedMaterialsResult preparedMaterials = new PreparedMaterialsResult(
					List.of(),
					List.of(),
					List.of(),
					new SignalNotes(List.of(), List.of()),
					new OverlapNotes(List.of(), List.of()),
					new PreparationStats(0, 0));

			// When:
			LessonGenPrompt prompt = builder.buildTheoreticalLessonPrompt(
					preparedMaterials, "", "standard", "clear", "structured theoretical lesson", null);

			// Then:
			assertThat(prompt.fileInputs()).isEqualTo(List.of());
		}

		@Test
		void shouldUseNoSourceMaterialsMessagingWhenMaterialsListIsEmptyTest() {
			// Given: a prepared-materials result with an empty materials list
			PreparedMaterialsResult preparedMaterials = new PreparedMaterialsResult(
					List.of(),
					List.of(),
					List.of(),
					new SignalNotes(List.of(), List.of()),
					new OverlapNotes(List.of(), List.of()),
					new PreparationStats(0, 0));

			// When:
			LessonGenPrompt prompt = builder.buildTheoreticalLessonPrompt(
					preparedMaterials, "Some topic", "standard", "clear", "structured theoretical lesson", List.of());

			// Then: the prompt asks the model to use the topic instead of source materials, and no
			// source-material block is appended after the "Selected source materials" line
			assertThat(prompt.input())
					.contains("Create one coherent theoretical lesson from the user-provided topic/instructions.")
					.contains("- Source mode: prompt-only topic")
					.contains("Selected source materials: none. Use the extra user instructions as the topic.")
					.doesNotContain("Selected source materials:\n");
		}

		@Test
		void shouldShowNoCandidateTermsMessageWhenExtractedTermsIsEmptyTest() {
			// Given: a prepared-materials result with no extracted terms
			PreparedMaterialsResult preparedMaterials = new PreparedMaterialsResult(
					List.of(),
					List.of(),
					List.of(),
					new SignalNotes(List.of(), List.of()),
					new OverlapNotes(List.of(), List.of()),
					new PreparationStats(0, 0));

			// When:
			LessonGenPrompt prompt = builder.buildTheoreticalLessonPrompt(preparedMaterials);

			// Then:
			assertThat(prompt.input()).contains("- Candidate key terms: No candidate terms extracted.");
		}

		@Test
		void shouldShowNoUserInstructionsMessageWhenUserInstructionsIsNullTest() {
			// Given: userInstructions explicitly null
			PreparedMaterialsResult preparedMaterials = new PreparedMaterialsResult(
					List.of(),
					List.of(),
					List.of(),
					new SignalNotes(List.of(), List.of()),
					new OverlapNotes(List.of(), List.of()),
					new PreparationStats(0, 0));

			// When:
			LessonGenPrompt prompt = builder.buildTheoreticalLessonPrompt(
					preparedMaterials, null, "standard", "clear", "structured theoretical lesson", List.of());

			// Then:
			assertThat(prompt.input()).contains("- Extra user instructions: none");
		}

		@Test
		void shouldShowNoUserInstructionsMessageWhenUserInstructionsIsBlankTest() {
			// Given: userInstructions is whitespace-only
			PreparedMaterialsResult preparedMaterials = new PreparedMaterialsResult(
					List.of(),
					List.of(),
					List.of(),
					new SignalNotes(List.of(), List.of()),
					new OverlapNotes(List.of(), List.of()),
					new PreparationStats(0, 0));

			// When:
			LessonGenPrompt prompt = builder.buildTheoreticalLessonPrompt(
					preparedMaterials, "   ", "standard", "clear", "structured theoretical lesson", List.of());

			// Then:
			assertThat(prompt.input()).contains("- Extra user instructions: none");
		}

		@Test
		void shouldIncludeOverlapAndSignalNotesInPreparationSectionTest() {
			// Given: overlaps and signals attached to the prepared-materials result
			PreparedMaterialsResult preparedMaterials = new PreparedMaterialsResult(
					List.of(),
					List.of(),
					List.of(),
					new SignalNotes(
							List.of(new SignalItem(1, "Use UTM tags.")),
							List.of(new SignalItem(2, "Do not exceed budget."))),
					new OverlapNotes(
							List.of(new DuplicateTitle("Intro")),
							List.of(new DuplicateUrl("https://dup.example.com"))),
					new PreparationStats(0, 0));

			// When:
			LessonGenPrompt prompt = builder.buildTheoreticalLessonPrompt(preparedMaterials);

			// Then:
			assertThat(prompt.input())
					.contains("- Overlap check: Duplicate or repeated titles: \"Intro\".\nRepeated URLs: https://dup" +
							".example.com.")
					.contains("- Lesson signals: Possible examples from sources:\n- Source 1: Use UTM tags."
							+ "\n\nPossible caveats or warnings from sources:\n- Source 2: Do not exceed budget.");
		}
	}

	@Nested
	class BuildTranscriptCondensationPrompt {

		@Test
		void shouldReturnFixedVersionAndInputEqualToTranscriptTextTest() {
			// Given:
			String transcriptText = "00:00 hey everyone um so today we're gonna talk about onboarding";

			// When:
			LessonGenPrompt prompt = builder.buildTranscriptCondensationPrompt(transcriptText);

			// Then:
			assertThat(prompt.version()).isEqualTo("transcript-condensation-v1");
			assertThat(prompt.cacheKey()).isEqualTo("transcript-condensation-v1");
			assertThat(prompt.input()).isEqualTo(transcriptText);
			assertThat(prompt.fileInputs()).isEqualTo(List.of());
			assertThat(prompt.instructions())
					.contains("Condense the transcript below.")
					.contains("Output only the condensed text, with no")
					.contains("introduction or commentary.")
					.doesNotStartWith("\n")
					.doesNotEndWith("\n");
		}
	}

	@Nested
	class FormatSourceMaterial {

		@Test
		void shouldIncludeAllPresentSectionsInSourceOrderTest() {
			// Given: a material with description, extracted text, a YouTube URL with a
			// non-condensed transcript, a link asset with every optional field, and an attachment
			// with an attached OpenAI file id
			Map<String, Object> material = new LinkedHashMap<>();
			material.put("sourceNumber", 1);
			material.put("title", "Company handbook");
			material.put("description", "Policies and procedures");
			material.put("text", "Full extracted text body");
			material.put("youtubeUrls", List.of("https://youtube.com/watch?v=abc"));
			material.put("youtubeTranscripts", List.of(Map.of(
					"url", "https://youtube.com/watch?v=abc",
					"preparedText", "Transcript body",
					"wasCondensed", false)));
			material.put("links", List.of("https://example.com"));
			material.put("linkAssets", List.of(Map.of(
					"title", "Example site",
					"url", "https://example.com",
					"siteName", "Example",
					"description", "An example website",
					"extractedText", "Page body text")));
			material.put("attachments", List.of(Map.of(
					"name", "policy.pdf",
					"kind", "file",
					"mimeType", "application/pdf",
					"openaiFileId", "file-9")));

			// When:
			String formatted = builder.formatSourceMaterial(material);

			// Then:
			assertThat(formatted)
					.contains("Source 1: Company handbook")
					.contains("Description:\nPolicies and procedures")
					.contains("Extracted text:\nFull extracted text body")
					.contains("YouTube URLs:\n- https://youtube.com/watch?v=abc")
					.contains("YouTube transcript context:\nYouTube transcript for https://youtube" +
							".com/watch?v=abc:\nTranscript body")
					.contains("Links:\n- https://example.com")
					.contains("Parsed web link context:\nWeb source: Example site\nURL: https://example.com\nSite: " +
							"Example"
							+ "\nDescription:\nAn example website\nExtracted page text:\nPage body text")
					.contains("Attachments:\n- policy.pdf (file, application/pdf, attached to this OpenAI request as " +
							"file-9)");
		}

		@Test
		void shouldLabelCondensedYoutubeTranscriptWhenWasCondensedIsTrueTest() {
			// Given: a transcript flagged as condensed
			Map<String, Object> material = new LinkedHashMap<>();
			material.put("sourceNumber", 3);
			material.put("title", "Webinar");
			material.put("youtubeTranscripts", List.of(Map.of(
					"url", "https://youtube.com/watch?v=xyz",
					"preparedText", "Condensed body",
					"wasCondensed", true)));

			// When:
			String formatted = builder.formatSourceMaterial(material);

			// Then:
			assertThat(formatted)
					.contains("Condensed YouTube transcript, filler removed for https://youtube" +
							".com/watch?v=xyz:\nCondensed body");
		}

		@Test
		void shouldMarkTranscriptUnavailableWithReasonWhenPreparedTextIsBlankAndErrorIsPresentTest() {
			// Given: a transcript with no prepared text and an explicit error
			Map<String, Object> material = new LinkedHashMap<>();
			material.put("sourceNumber", 4);
			material.put("title", "Broken video");
			material.put("youtubeTranscripts", List.of(Map.of(
					"url", "https://youtube.com/watch?v=err",
					"error", "Transcript disabled by uploader")));

			// When:
			String formatted = builder.formatSourceMaterial(material);

			// Then:
			assertThat(formatted)
					.contains("YouTube transcript unavailable for https://youtube.com/watch?v=err.\n"
							+ "Reason: Transcript disabled by uploader");
		}

		@Test
		void shouldMarkTranscriptUnavailableWithoutReasonWhenErrorIsBlankTest() {
			// Given: a transcript with no prepared text and no error
			Map<String, Object> material = new LinkedHashMap<>();
			material.put("sourceNumber", 5);
			material.put("title", "Silent video");
			material.put("youtubeTranscripts", List.of(Map.of("url", "https://youtube.com/watch?v=noerr")));

			// When:
			String formatted = builder.formatSourceMaterial(material);

			// Then:
			assertThat(formatted)
					.contains("YouTube transcript unavailable for https://youtube.com/watch?v=noerr.")
					.doesNotContain("Reason:");
		}

		@Test
		void shouldFallBackToNumberedWebLinkLabelAndIncludeMetadataErrorWhenTitleIsBlankTest() {
			// Given: a link asset with no title and a metadata parsing error
			Map<String, Object> material = new LinkedHashMap<>();
			material.put("sourceNumber", 6);
			material.put("title", "Link roundup");
			material.put("linkAssets", List.of(Map.of(
					"url", "https://example.org",
					"metadataError", "Could not parse metadata")));

			// When:
			String formatted = builder.formatSourceMaterial(material);

			// Then:
			assertThat(formatted)
					.contains("Web source: Web link 1")
					.contains("Link parsing note: Could not parse metadata");
		}

		@Test
		void shouldMarkAttachmentAsMetadataOnlyAndFallBackMimeTypeWhenOpenaiFileIdAndMimeTypeAreAbsentTest() {
			// Given: an attachment with no OpenAI file id and no MIME type
			Map<String, Object> material = new LinkedHashMap<>();
			material.put("sourceNumber", 7);
			material.put("title", "Files only");
			material.put("attachments", List.of(Map.of(
					"name", "clip.mov",
					"kind", "file")));

			// When:
			String formatted = builder.formatSourceMaterial(material);

			// Then:
			assertThat(formatted).contains("Attachments:\n- clip.mov (file, unknown MIME, metadata only)");
		}

		@Test
		void shouldReturnOnlySourceHeaderWhenNoOtherFieldsArePresentTest() {
			// Given: a material with only the required header fields
			Map<String, Object> material = new LinkedHashMap<>();
			material.put("sourceNumber", 8);
			material.put("title", "Solo title");

			// When:
			String formatted = builder.formatSourceMaterial(material);

			// Then:
			assertThat(formatted).isEqualTo("Source 8: Solo title");
		}
	}

	@Nested
	class FormatOverlaps {

		@Test
		void shouldReturnNoDuplicatesMessageWhenBothListsAreEmptyTest() {
			// Given:
			OverlapNotes overlaps = new OverlapNotes(List.of(), List.of());

			// When:
			String formatted = builder.formatOverlaps(overlaps);

			// Then:
			assertThat(formatted).isEqualTo("No obvious duplicate titles or URLs detected.");
		}

		@Test
		void shouldFormatMultipleDuplicateTitlesJoinedByCommaTest() {
			// Given:
			OverlapNotes overlaps = new OverlapNotes(
					List.of(new DuplicateTitle("Intro"), new DuplicateTitle("Onboarding basics")),
					List.of());

			// When:
			String formatted = builder.formatOverlaps(overlaps);

			// Then:
			assertThat(formatted).isEqualTo("Duplicate or repeated titles: \"Intro\", \"Onboarding basics\".");
		}

		@Test
		void shouldFormatMultipleDuplicateUrlsJoinedByCommaTest() {
			// Given:
			OverlapNotes overlaps = new OverlapNotes(
					List.of(),
					List.of(new DuplicateUrl("https://a.example.com"), new DuplicateUrl("https://b.example.com")));

			// When:
			String formatted = builder.formatOverlaps(overlaps);

			// Then:
			assertThat(formatted).isEqualTo("Repeated URLs: https://a.example.com, https://b.example.com.");
		}

		@Test
		void shouldJoinDuplicateTitlesAndUrlsNotesByNewlineWhenBothPresentTest() {
			// Given:
			OverlapNotes overlaps = new OverlapNotes(
					List.of(new DuplicateTitle("Intro")),
					List.of(new DuplicateUrl("https://dup.example.com")));

			// When:
			String formatted = builder.formatOverlaps(overlaps);

			// Then:
			assertThat(formatted).isEqualTo(
					"Duplicate or repeated titles: \"Intro\".\nRepeated URLs: https://dup.example.com.");
		}
	}

	@Nested
	class FormatSignals {

		@Test
		void shouldReturnNoSignalsMessageWhenBothListsAreEmptyTest() {
			// Given:
			SignalNotes signals = new SignalNotes(List.of(), List.of());

			// When:
			String formatted = builder.formatSignals(signals);

			// Then:
			assertThat(formatted).isEqualTo("No example or caveat signals detected.");
		}

		@Test
		void shouldFormatExamplesOnlyTest() {
			// Given:
			SignalNotes signals = new SignalNotes(
					List.of(new SignalItem(1, "Use UTM tags."), new SignalItem(2, "Segment by campaign.")),
					List.of());

			// When:
			String formatted = builder.formatSignals(signals);

			// Then:
			assertThat(formatted).isEqualTo(
					"Possible examples from sources:\n- Source 1: Use UTM tags.\n- Source 2: Segment by campaign.");
		}

		@Test
		void shouldFormatCaveatsOnlyTest() {
			// Given:
			SignalNotes signals = new SignalNotes(
					List.of(),
					List.of(new SignalItem(3, "Do not exceed daily budget.")));

			// When:
			String formatted = builder.formatSignals(signals);

			// Then:
			assertThat(formatted).isEqualTo(
					"Possible caveats or warnings from sources:\n- Source 3: Do not exceed daily budget.");
		}

		@Test
		void shouldJoinExamplesAndCaveatsByBlankLineWhenBothPresentTest() {
			// Given:
			SignalNotes signals = new SignalNotes(
					List.of(new SignalItem(1, "Use UTM tags.")),
					List.of(new SignalItem(2, "Do not exceed budget.")));

			// When:
			String formatted = builder.formatSignals(signals);

			// Then:
			assertThat(formatted).isEqualTo(
					"Possible examples from sources:\n- Source 1: Use UTM tags."
							+ "\n\nPossible caveats or warnings from sources:\n- Source 2: Do not exceed budget.");
		}
	}
}
