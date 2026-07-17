package com.aidigital.aionboarding.service.lesson.prompt;

import com.aidigital.aionboarding.service.common.mapping.JsonMapReader;
import com.aidigital.aionboarding.service.common.mapping.TextValueNormalizer;
import com.aidigital.aionboarding.service.lesson.models.LessonDetailRecord;
import com.aidigital.aionboarding.service.lesson.models.LessonRevisionPromptRecord;
import com.aidigital.aionboarding.service.lesson.models.RevisionBriefRecord;
import com.aidigital.aionboarding.service.lesson.util.LessonTextUtil;
import com.aidigital.aionboarding.service.lesson.util.OpenAiPromptCacheUtil;
import com.aidigital.aionboarding.service.material.models.MaterialPreparationItem;
import com.aidigital.aionboarding.service.material.models.PreparedMaterialsResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.instancio.Instancio;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.Select.field;

class LessonRevisionPromptBuilderTest {

	private final OpenAiPromptCacheUtil openAiPromptCacheUtil = new OpenAiPromptCacheUtil();

	private final LessonRevisionPromptBuilder builder = new LessonRevisionPromptBuilder(
			new LessonTextUtil(), openAiPromptCacheUtil, new TextValueNormalizer(), new JsonMapReader());

	private PreparedMaterialsResult preparedMaterialsWith(List<Map<String, Object>> materialDataMaps) {
		List<MaterialPreparationItem> items = new java.util.ArrayList<>();
		for (int index = 0; index < materialDataMaps.size(); index++) {
			items.add(new MaterialPreparationItem((long) (index + 1), index + 1, materialDataMaps.get(index)));
		}
		return new PreparedMaterialsResult(items, null, null, null, null, null);
	}

	@Nested
	class BuildLessonRevisionPlannerPrompt {

		@Test
		void shouldIncludeSourceMaterialsAndAllLessonContextWhenFieldsArePresentTest() {
			// Given: a fully-populated lesson, two source materials, and preset options
			LessonDetailRecord lesson = Instancio.of(LessonDetailRecord.class)
					.set(field(LessonDetailRecord::id), 7L)
					.set(field(LessonDetailRecord::title), "Intro to APIs")
					.set(field(LessonDetailRecord::contentHtml), "<p>Lesson body</p>")
					.set(field(LessonDetailRecord::depth), "deep")
					.set(field(LessonDetailRecord::tone), "friendly")
					.set(field(LessonDetailRecord::desiredFormat), "story-based")
					.set(field(LessonDetailRecord::userInstructions), "Keep it upbeat")
					.create();
			Map<String, Object> materialOne = Map.of("sourceNumber", 1, "title", "Doc A", "text", "Body A");
			Map<String, Object> materialTwo = Map.of("sourceNumber", 2, "title", "Doc B", "text", "Body B");
			PreparedMaterialsResult preparedMaterials = preparedMaterialsWith(List.of(materialOne, materialTwo));

			// When:
			LessonRevisionPromptRecord prompt = builder.buildLessonRevisionPlannerPrompt(
					lesson, preparedMaterials, "  Please   simplify   this lesson.  ", List.of("simpler", "deeper"));

			// Then:
			assertThat(prompt.version()).isEqualTo(LessonRevisionPromptBuilder.LESSON_REVISION_PLANNER_VERSION);
			assertThat(prompt.cacheKey()).isEqualTo(openAiPromptCacheUtil.buildOpenAIPromptCacheKey(
					"lesson-revision-plan", LessonRevisionPromptBuilder.LESSON_REVISION_PLANNER_VERSION, 7L));
			assertThat(prompt.instructions())
					.contains("You analyze user feedback for an existing lesson and prepare a revision brief for " +
							"another model.")
					.contains("\"changeScope\": \"targeted\" | \"substantial\" | \"near-complete\",");
			String expectedSourceText = builder.formatSourceMaterial(materialOne)
					+ "\n\n---\n\n" + builder.formatSourceMaterial(materialTwo);
			assertThat(prompt.input())
					.contains("Current lesson title:\nIntro to APIs")
					.contains("Current lesson HTML:\n<p>Lesson body</p>")
					.contains("- Depth: deep")
					.contains("- Tone: friendly")
					.contains("- Desired format: story-based")
					.contains("- Original extra instructions: Keep it upbeat")
					.contains("Selected preset revision options:\n"
							+ "- Make the explanation simpler and easier to follow.\n"
							+ "- Add more depth and useful theoretical detail.")
					.contains("User revision request:\nPlease simplify this lesson.")
					.contains("Current linked source materials:\n" + expectedSourceText);
		}

		@Test
		void shouldFallBackToDefaultsAndUnknownLessonIdWhenFieldsAreAbsentTest() {
			// Given: a lesson with no optional fields set, no materials, no comment, no preset options
			LessonDetailRecord lesson = Instancio.of(LessonDetailRecord.class)
					.set(field(LessonDetailRecord::id), (Long) null)
					.set(field(LessonDetailRecord::title), (String) null)
					.set(field(LessonDetailRecord::contentHtml), (String) null)
					.set(field(LessonDetailRecord::depth), (String) null)
					.set(field(LessonDetailRecord::tone), (String) null)
					.set(field(LessonDetailRecord::desiredFormat), (String) null)
					.set(field(LessonDetailRecord::userInstructions), (String) null)
					.create();

			// When:
			LessonRevisionPromptRecord prompt = builder.buildLessonRevisionPlannerPrompt(
					lesson, null, "   ", null);

			// Then:
			assertThat(prompt.cacheKey()).isEqualTo(openAiPromptCacheUtil.buildOpenAIPromptCacheKey(
					"lesson-revision-plan", LessonRevisionPromptBuilder.LESSON_REVISION_PLANNER_VERSION, "unknown" +
							"-lesson"));
			assertThat(prompt.input())
					.contains("Current lesson title:\nUntitled lesson")
					.contains("- Depth: standard")
					.contains("- Tone: clear")
					.contains("- Desired format: structured theoretical lesson")
					.contains("- Original extra instructions: none")
					.contains("No preset revision options selected.")
					.contains("No freeform comment provided.")
					.contains("Current linked source materials: none. Use the original lesson and revision request as " +
							"the main context.")
					.doesNotContain("null");
		}

		@Test
		void shouldTreatBlankButNonNullUserInstructionsAsNoneTest() {
			// Given: userInstructions is set but whitespace-only, not null
			LessonDetailRecord lesson = Instancio.of(LessonDetailRecord.class)
					.set(field(LessonDetailRecord::userInstructions), "   ")
					.create();

			// When:
			LessonRevisionPromptRecord prompt = builder.buildLessonRevisionPlannerPrompt(
					lesson, null, "Feedback", List.of());

			// Then:
			assertThat(prompt.input()).contains("- Original extra instructions: none");
		}
	}

	@Nested
	class BuildLessonRevisionWriterPrompt {

		@Test
		void shouldIncludeWriterInstructionsBriefAndFullContextWhenFieldsArePresentTest() {
			// Given: a fully-populated lesson, two source materials, preset options, and a revision brief
			LessonDetailRecord lesson = Instancio.of(LessonDetailRecord.class)
					.set(field(LessonDetailRecord::id), 11L)
					.set(field(LessonDetailRecord::title), "Advanced onboarding")
					.set(field(LessonDetailRecord::contentHtml), "<p>Old content</p>")
					.set(field(LessonDetailRecord::depth), "shallow")
					.set(field(LessonDetailRecord::tone), "formal")
					.set(field(LessonDetailRecord::desiredFormat), "checklist")
					.set(field(LessonDetailRecord::userInstructions), "Mention compliance")
					.create();
			Map<String, Object> materialOne = Map.of("sourceNumber", 1, "title", "Doc A", "text", "Body A");
			Map<String, Object> materialTwo = Map.of("sourceNumber", 2, "title", "Doc B", "text", "Body B");
			PreparedMaterialsResult preparedMaterials = preparedMaterialsWith(List.of(materialOne, materialTwo));
			RevisionBriefRecord revisionBrief = new RevisionBriefRecord(
					"substantial", "Make it clearer",
					List.of("Simplify intro"), List.of("Keep the pricing table"), List.of("None"));

			// When:
			LessonRevisionPromptRecord prompt = builder.buildLessonRevisionWriterPrompt(
					lesson, preparedMaterials, "  Please   shorten   this.  ", List.of("shorter"), revisionBrief);

			// Then:
			assertThat(prompt.version()).isEqualTo(LessonRevisionPromptBuilder.LESSON_REVISION_WRITER_VERSION);
			assertThat(prompt.cacheKey()).isEqualTo(openAiPromptCacheUtil.buildOpenAIPromptCacheKey(
					"lesson-revision-write", LessonRevisionPromptBuilder.LESSON_REVISION_WRITER_VERSION, 11L));
			assertThat(prompt.instructions())
					.contains(LessonPromptConstants.LESSON_INSTRUCTIONS)
					.contains("Revision mode:")
					.contains("Return HTML only. Do not include commentary, JSON, or explanations outside the lesson " +
							"HTML.");
			String expectedSourceText = builder.formatSourceMaterial(materialOne)
					+ "\n\n---\n\n" + builder.formatSourceMaterial(materialTwo);
			String expectedBriefJson = builder.serializeRevisionBrief(revisionBrief);
			assertThat(prompt.input())
					.contains("Revise the current lesson using the revision brief.")
					.contains("Current lesson title:\nAdvanced onboarding")
					.contains("Current lesson HTML:\n<p>Old content</p>")
					.contains("Revision brief:\n" + expectedBriefJson)
					.contains("Selected preset revision options:\n- Shorten the lesson and remove unnecessary " +
							"wording.")
					.contains("User revision request:\nPlease shorten this.")
					.contains("- Depth: shallow")
					.contains("- Tone: formal")
					.contains("- Desired format: checklist")
					.contains("- Original extra instructions: Mention compliance")
					.contains("Current linked source materials:\n" + expectedSourceText);
		}

		@Test
		void shouldFallBackToDefaultsAndUnknownLessonIdWhenFieldsAreAbsentTest() {
			// Given: a lesson with no optional fields set and no materials, comment, or options
			LessonDetailRecord lesson = Instancio.of(LessonDetailRecord.class)
					.set(field(LessonDetailRecord::id), (Long) null)
					.set(field(LessonDetailRecord::title), (String) null)
					.set(field(LessonDetailRecord::contentHtml), (String) null)
					.set(field(LessonDetailRecord::depth), (String) null)
					.set(field(LessonDetailRecord::tone), (String) null)
					.set(field(LessonDetailRecord::desiredFormat), (String) null)
					.set(field(LessonDetailRecord::userInstructions), (String) null)
					.create();
			RevisionBriefRecord revisionBrief = new RevisionBriefRecord(
					"targeted", "Tidy wording", List.of(), List.of(), List.of());

			// When:
			LessonRevisionPromptRecord prompt = builder.buildLessonRevisionWriterPrompt(
					lesson, null, "   ", null, revisionBrief);

			// Then:
			assertThat(prompt.cacheKey()).isEqualTo(openAiPromptCacheUtil.buildOpenAIPromptCacheKey(
					"lesson-revision-write", LessonRevisionPromptBuilder.LESSON_REVISION_WRITER_VERSION, "unknown" +
							"-lesson"));
			assertThat(prompt.input())
					.contains("Current lesson title:\nUntitled lesson")
					.contains("- Depth: standard")
					.contains("- Tone: clear")
					.contains("- Desired format: structured theoretical lesson")
					.contains("- Original extra instructions: none")
					.contains("No preset revision options selected.")
					.contains("No freeform comment provided.")
					.contains("Current linked source materials: none. "
							+ "Use the current lesson as the main factual baseline unless the user request contradicts" +
							" it.");
		}

		@Test
		void shouldTreatBlankButNonNullUserInstructionsAsNoneTest() {
			// Given: userInstructions is set but whitespace-only, not null
			LessonDetailRecord lesson = Instancio.of(LessonDetailRecord.class)
					.set(field(LessonDetailRecord::userInstructions), "   ")
					.create();
			RevisionBriefRecord revisionBrief = new RevisionBriefRecord(
					"targeted", "Tidy wording", List.of(), List.of(), List.of());

			// When:
			LessonRevisionPromptRecord prompt = builder.buildLessonRevisionWriterPrompt(
					lesson, null, "Feedback", List.of(), revisionBrief);

			// Then:
			assertThat(prompt.input()).contains("- Original extra instructions: none");
		}
	}

	@Nested
	class Materials {

		@Test
		void shouldReturnEmptyListWhenPreparedMaterialsIsNullTest() {
			// When:
			List<Map<String, Object>> materials = builder.materials(null);

			// Then:
			assertThat(materials).isEmpty();
		}

		@Test
		void shouldMapMaterialPreparationItemsToTheirDataMapsInSourceOrderTest() {
			// Given:
			Map<String, Object> dataOne = Map.of("title", "Doc A");
			Map<String, Object> dataTwo = Map.of("title", "Doc B");
			PreparedMaterialsResult preparedMaterials = new PreparedMaterialsResult(
					List.of(new MaterialPreparationItem(1L, 1, dataOne), new MaterialPreparationItem(2L, 2, dataTwo)),
					null, null, null, null, null);

			// When:
			List<Map<String, Object>> materials = builder.materials(preparedMaterials);

			// Then:
			assertThat(materials).containsExactly(dataOne, dataTwo);
		}
	}

	@Nested
	class FormatSourceMaterial {

		@Test
		void shouldIncludeAllPresentSectionsInSourceOrderTest() {
			// Given: a material with every optional section populated
			Map<String, Object> material = new LinkedHashMap<>();
			material.put("sourceNumber", 1);
			material.put("title", "Company handbook");
			material.put("description", "Policies and procedures");
			material.put("text", "Full extracted text body");
			material.put("youtubeUrls", List.of("https://youtube.com/watch?v=abc", "https://youtube.com/watch?v=def"));
			material.put("youtubeTranscripts", List.of(Map.of(
					"url", "https://youtube.com/watch?v=abc",
					"preparedText", "Transcript body",
					"wasCondensed", true)));
			material.put("links", List.of("https://example.com", "https://example.org"));
			material.put("linkAssets", List.of(
					Map.of(
							"title", "Example site",
							"url", "https://example.com",
							"siteName", "Example",
							"description", "Site description",
							"extractedText", "Extracted page body",
							"metadataError", "Partial parse"),
					Map.of("title", "Second site", "url", "https://example.org")));
			material.put("attachments", List.of(
					Map.of("name", "policy.pdf", "kind", "file"),
					Map.of("name", "handbook.docx", "kind", "file")));

			// When:
			String formatted = builder.formatSourceMaterial(material);

			// Then:
			assertThat(formatted)
					.contains("Source 1: Company handbook")
					.contains("Description:\nPolicies and procedures")
					.contains("Extracted text:\nFull extracted text body")
					.contains("YouTube URLs:\n"
							+ "- https://youtube.com/watch?v=abc\n- https://youtube.com/watch?v=def")
					.contains("YouTube transcript context:\n"
							+ "Condensed YouTube transcript, filler removed for https://youtube" +
							".com/watch?v=abc:\nTranscript body")
					.contains("Links:\n- https://example.com\n- https://example.org")
					.contains("Parsed web link context:\nWeb source: Example site")
					.contains("URL: https://example.com")
					.contains("Site: Example")
					.contains("Description:\nSite description")
					.contains("Extracted page text:\nExtracted page body")
					.contains("Link parsing note: Partial parse")
					.contains("Web source: Second site\nURL: https://example.org")
					.contains("Attachments, metadata only:\n- policy.pdf (file)\n- handbook.docx (file)");
		}

		@Test
		void shouldFallBackToWebLinkLabelAndReportUnavailableTranscriptWithoutReasonWhenBlankTest() {
			// Given: only a title, a transcript with no prepared text or error, and a link with no title
			Map<String, Object> material = new LinkedHashMap<>();
			material.put("sourceNumber", 2);
			material.put("title", "Bare material");
			material.put("youtubeTranscripts", List.of(Map.of("url", "https://youtube.com/watch?v=missing")));
			material.put("linkAssets", List.of(Map.of("url", "https://example.com/link2")));

			// When:
			String formatted = builder.formatSourceMaterial(material);

			// Then:
			assertThat(formatted)
					.contains("Source 2: Bare material")
					.contains("YouTube transcript context:\n"
							+ "YouTube transcript unavailable for https://youtube.com/watch?v=missing.")
					.contains("Parsed web link context:\nWeb source: Web link 1")
					.contains("URL: https://example.com/link2")
					.doesNotContain("Description:")
					.doesNotContain("Attachments, metadata only:")
					.doesNotContain("Reason:")
					.doesNotContain("Site:");
		}

		@Test
		void shouldReportUnavailableTranscriptReasonWhenErrorIsPresentTest() {
			// Given: a transcript with no prepared text but a populated error message
			Map<String, Object> material = new LinkedHashMap<>();
			material.put("sourceNumber", 3);
			material.put("title", "Video source");
			material.put("youtubeTranscripts", List.of(Map.of(
					"url", "https://youtube.com/watch?v=rl", "error", "Rate limited by provider")));

			// When:
			String formatted = builder.formatSourceMaterial(material);

			// Then:
			assertThat(formatted)
					.contains("YouTube transcript unavailable for https://youtube.com/watch?v=rl.")
					.contains("Reason: Rate limited by provider");
		}
	}

	@Nested
	class BuildRevisionOptionsText {

		@Test
		void shouldReturnPlaceholderWhenSelectedOptionsIsNullTest() {
			// When-Then:
			assertThat(builder.buildRevisionOptionsText(null)).isEqualTo("No preset revision options selected.");
		}

		@Test
		void shouldReturnPlaceholderWhenSelectedOptionsIsEmptyTest() {
			// When-Then:
			assertThat(builder.buildRevisionOptionsText(List.of())).isEqualTo("No preset revision options selected.");
		}

		@Test
		void shouldMapKnownOptionCodesToTheirLabelsJoinedByNewlineTest() {
			// When:
			String text = builder.buildRevisionOptionsText(List.of("simpler", "shorter"));

			// Then:
			assertThat(text).isEqualTo(
					"- Make the explanation simpler and easier to follow.\n"
							+ "- Shorten the lesson and remove unnecessary wording.");
		}

		@Test
		void shouldFallBackToRawOptionCodeForAnUnknownOptionTest() {
			// When:
			String text = builder.buildRevisionOptionsText(List.of("custom-option"));

			// Then:
			assertThat(text).isEqualTo("- custom-option");
		}
	}

	@Nested
	class SerializeRevisionBrief {

		@Test
		void shouldReturnPrettyPrintedJsonMatchingTheBriefFieldsTest() throws Exception {
			// Given:
			RevisionBriefRecord brief = new RevisionBriefRecord(
					"substantial",
					"Make it clearer",
					List.of("Simplify intro", "Add a summary"),
					List.of("Keep the pricing table"),
					List.of("User request conflicts with source material"));

			// When:
			String json = builder.serializeRevisionBrief(brief);

			// Then: it is pretty-printed (multi-line) and round-trips to the original field values
			assertThat(json).contains("\n");
			Map<?, ?> parsed = new ObjectMapper().readValue(json, Map.class);
			assertThat(parsed.get("changeScope")).isEqualTo("substantial");
			assertThat(parsed.get("userIntent")).isEqualTo("Make it clearer");
			@SuppressWarnings("unchecked")
			List<String> editInstructions = (List<String>) parsed.get("editInstructions");
			@SuppressWarnings("unchecked")
			List<String> preserveRules = (List<String>) parsed.get("preserveRules");
			@SuppressWarnings("unchecked")
			List<String> riskNotes = (List<String>) parsed.get("riskNotes");
			assertThat(editInstructions).containsExactly("Simplify intro", "Add a summary");
			assertThat(preserveRules).containsExactly("Keep the pricing table");
			assertThat(riskNotes).containsExactly("User request conflicts with source material");
		}

		@Test
		void shouldReturnJsonNullLiteralWhenBriefIsNullTest() {
			// When:
			String json = builder.serializeRevisionBrief(null);

			// Then:
			assertThat(json).isEqualTo("null");
		}
	}

	@Nested
	class AllowedRevisionOptions {

		@Test
		void shouldReturnAllSupportedRevisionOptionCodesTest() {
			// When-Then:
			assertThat(builder.allowedRevisionOptions())
					.containsExactlyInAnyOrder("simpler", "deeper", "examples", "structured", "shorter");
		}
	}

	@Nested
	class ValueOrFallback {

		@Test
		void shouldReturnFallbackWhenValueIsNullTest() {
			// When-Then:
			assertThat(builder.valueOrFallback(null, "fallback")).isEqualTo("fallback");
		}

		@Test
		void shouldReturnFallbackWhenValueIsBlankTest() {
			// When-Then:
			assertThat(builder.valueOrFallback("   ", "fallback")).isEqualTo("fallback");
		}

		@Test
		void shouldReturnRawTextWhenValueIsNonBlankTest() {
			// When-Then:
			assertThat(builder.valueOrFallback("Hello there", "fallback")).isEqualTo("Hello there");
		}

		@Test
		void shouldReturnStringRepresentationForNonStringValuesTest() {
			// When-Then:
			assertThat(builder.valueOrFallback(42, "fallback")).isEqualTo("42");
		}
	}
}
