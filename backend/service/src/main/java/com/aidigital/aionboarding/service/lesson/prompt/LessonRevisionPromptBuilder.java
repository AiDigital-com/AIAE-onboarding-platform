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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Verbatim port of {@code lessonRevision.js} prompt builders.
 */
@Component
@RequiredArgsConstructor
public class LessonRevisionPromptBuilder {

	public static final String LESSON_REVISION_PLANNER_VERSION = "lesson-revision-planner-v1";
	public static final String LESSON_REVISION_WRITER_VERSION = "lesson-revision-writer-v1";

	private static final Map<String, String> REVISION_OPTION_LABELS = Map.of(
			"simpler", "Make the explanation simpler and easier to follow.",
			"deeper", "Add more depth and useful theoretical detail.",
			"examples", "Add examples only where they genuinely improve understanding.",
			"structured", "Improve structure and flow.",
			"shorter", "Shorten the lesson and remove unnecessary wording."
	);

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private final LessonTextUtil lessonTextUtil;
	private final OpenAiPromptCacheUtil openAiPromptCacheUtil;
	private final TextValueNormalizer textValueNormalizer;
	private final JsonMapReader jsonMapReader;

	public LessonRevisionPromptRecord buildLessonRevisionPlannerPrompt(
			LessonDetailRecord lesson,
			PreparedMaterialsResult preparedMaterials,
			String revisionRequest,
			List<String> selectedOptions
	) {
		List<Map<String, Object>> materials = materials(preparedMaterials);
		String sourceText = materials.stream()
				.map(this::formatSourceMaterial)
				.reduce((a, b) -> a + "\n\n---\n\n" + b)
				.orElse("");
		String normalizedRequest = lessonTextUtil.normalizeText(revisionRequest);
		boolean hasSourceMaterials = !materials.isEmpty();

		String instructions = String.join("\n",
				"You analyze user feedback for an existing lesson and prepare a revision brief for another model.",
				"Do not rewrite the lesson yourself.",
				"Infer from the user request whether the lesson needs light edits, a substantial rewrite of many " +
						"parts, or a near-complete rewrite.",
				"Preserve the user intent exactly and avoid adding your own product ideas.",
				"Base preservation requirements on the current lesson and the provided source materials.",
				"Return valid JSON only. Do not wrap it in Markdown fences.",
				"",
				"Return this exact JSON shape:",
				"{",
				"  \"changeScope\": \"targeted\" | \"substantial\" | \"near-complete\",",
				"  \"userIntent\": \"short string\",",
				"  \"editInstructions\": [\"string\"],",
				"  \"preserveRules\": [\"string\"],",
				"  \"riskNotes\": [\"string\"]",
				"}",
				"",
				"Rules:",
				"- \"changeScope\" should reflect how much of the lesson likely needs to change to satisfy the " +
						"request.",
				"- \"editInstructions\" should be concrete writing instructions for the revising model.",
				"- \"preserveRules\" should capture what must stay accurate, present, or consistent unless the user " +
						"explicitly asks otherwise.",
				"- \"riskNotes\" should mention contradictions, unsupported requests, or factual risks when relevant" +
						".");

		String input = String.join("\n",
				"Current lesson title:",
				valueOrFallback(lesson.title(), "Untitled lesson"),
				"",
				"Current lesson HTML:",
				valueOrFallback(lesson.contentHtml(), ""),
				"",
				"Original lesson settings:",
				"- Depth: " + valueOrFallback(lesson.depth(), "standard"),
				"- Tone: " + valueOrFallback(lesson.tone(), "clear"),
				"- Desired format: " + valueOrFallback(lesson.desiredFormat(), "structured theoretical lesson"),
				lesson.userInstructions() != null && !valueOrFallback(lesson.userInstructions(), "").isBlank()
						? "- Original extra instructions: " + lesson.userInstructions()
						: "- Original extra instructions: none",
				"",
				"Selected preset revision options:",
				buildRevisionOptionsText(selectedOptions),
				"",
				"User revision request:",
				normalizedRequest.isBlank() ? "No freeform comment provided." : normalizedRequest,
				"",
				hasSourceMaterials
						? "Current linked source materials:"
						: "Current linked source materials: none. Use the original lesson and revision request as the " +
						  "main context.",
				hasSourceMaterials ? sourceText : "");

		return new LessonRevisionPromptRecord(
				LESSON_REVISION_PLANNER_VERSION,
				openAiPromptCacheUtil.buildOpenAIPromptCacheKey(
						"lesson-revision-plan",
						LESSON_REVISION_PLANNER_VERSION,
						lesson.id() == null ? "unknown-lesson" : lesson.id()),
				instructions,
				input
		);
	}

	public LessonRevisionPromptRecord buildLessonRevisionWriterPrompt(
			LessonDetailRecord lesson,
			PreparedMaterialsResult preparedMaterials,
			String revisionRequest,
			List<String> selectedOptions,
			RevisionBriefRecord revisionBrief
	) {
		List<Map<String, Object>> materials = materials(preparedMaterials);
		String sourceText = materials.stream()
				.map(this::formatSourceMaterial)
				.reduce((a, b) -> a + "\n\n---\n\n" + b)
				.orElse("");
		boolean hasSourceMaterials = !materials.isEmpty();
		String normalizedRequest = lessonTextUtil.normalizeText(revisionRequest);

		String instructions = String.join("\n",
				LessonPromptConstants.LESSON_INSTRUCTIONS,
				"",
				"Revision mode:",
				"- You are revising an existing lesson, not generating a fresh lesson from scratch unless the revision" +
						" brief clearly requires a near-complete rewrite.",
				"- Apply the requested edits from the user request and revision brief.",
				"- Decide how much of the lesson to change based on the revision brief, not by default.",
				"- Keep the lesson aligned with the current lesson unless the revision brief requires broader " +
						"replacement.",
				"- Preserve factual content, useful caveats, and source-backed nuance unless the user explicitly asks " +
						"to remove or replace them.",
				"- If the user request conflicts with the sources, keep the lesson source-grounded and handle the " +
						"conflict carefully.",
				"- Return HTML only. Do not include commentary, JSON, or explanations outside the lesson HTML.");

		String input = String.join("\n",
				"Revise the current lesson using the revision brief.",
				"",
				"Current lesson title:",
				valueOrFallback(lesson.title(), "Untitled lesson"),
				"",
				"Current lesson HTML:",
				valueOrFallback(lesson.contentHtml(), ""),
				"",
				"Revision brief:",
				serializeRevisionBrief(revisionBrief),
				"",
				"Selected preset revision options:",
				buildRevisionOptionsText(selectedOptions),
				"",
				"User revision request:",
				normalizedRequest.isBlank() ? "No freeform comment provided." : normalizedRequest,
				"",
				"Original lesson settings:",
				"- Depth: " + valueOrFallback(lesson.depth(), "standard"),
				"- Tone: " + valueOrFallback(lesson.tone(), "clear"),
				"- Desired format: " + valueOrFallback(lesson.desiredFormat(), "structured theoretical lesson"),
				lesson.userInstructions() != null && !valueOrFallback(lesson.userInstructions(), "").isBlank()
						? "- Original extra instructions: " + lesson.userInstructions()
						: "- Original extra instructions: none",
				"",
				hasSourceMaterials
						? "Current linked source materials:"
						: "Current linked source materials: none. Use the current lesson as the main factual baseline " +
						  "unless the user request contradicts it.",
				hasSourceMaterials ? sourceText : "");

		return new LessonRevisionPromptRecord(
				LESSON_REVISION_WRITER_VERSION,
				openAiPromptCacheUtil.buildOpenAIPromptCacheKey(
						"lesson-revision-write",
						LESSON_REVISION_WRITER_VERSION,
						lesson.id() == null ? "unknown-lesson" : lesson.id()),
				instructions,
				input
		);
	}

	/**
	 * Extracts material maps from the prepared-material result.
	 *
	 * @param preparedMaterials prepared material result
	 * @return material maps in source order
	 */
	List<Map<String, Object>> materials(PreparedMaterialsResult preparedMaterials) {
		if (preparedMaterials == null) {
			return List.of();
		}
		return preparedMaterials.materials().stream()
				.map(MaterialPreparationItem::data)
				.toList();
	}

	/**
	 * Formats one source material for revision-planner and revision-writer prompts.
	 *
	 * @param material source material map
	 * @return source context text
	 */
	String formatSourceMaterial(Map<String, Object> material) {
		List<Map<String, Object>> youtubeTranscripts = jsonMapReader.mapList(material.get("youtubeTranscripts"));
		List<String> youtubeTranscriptSections = youtubeTranscripts.stream().map(transcript -> {
			boolean wasCondensed = Boolean.TRUE.equals(transcript.get("wasCondensed"));
			String label = wasCondensed
					? "Condensed YouTube transcript, filler removed"
					: "YouTube transcript";
			String preparedText = valueOrFallback(transcript.get("preparedText"), "");
			if (!preparedText.isBlank()) {
				return label + " for " + transcript.get("url") + ":\n" + preparedText;
			}
			String error = valueOrFallback(transcript.get("error"), "");
			return ("YouTube transcript unavailable for " + transcript.get("url") + ".")
					+ (error.isBlank() ? "" : "\nReason: " + error);
		}).toList();

		List<Map<String, Object>> linkAssets = jsonMapReader.mapList(material.get("linkAssets"));
		List<String> linkContextSections = new java.util.ArrayList<>();
		for (int index = 0; index < linkAssets.size(); index++) {
			Map<String, Object> linkAsset = linkAssets.get(index);
			String label = valueOrFallback(linkAsset.get("title"), "Web link " + (index + 1));
			List<String> parts = new java.util.ArrayList<>();
			parts.add("Web source: " + label);
			parts.add("URL: " + linkAsset.get("url"));
			if (!valueOrFallback(linkAsset.get("siteName"), "").isBlank()) {
				parts.add("Site: " + linkAsset.get("siteName"));
			}
			if (!valueOrFallback(linkAsset.get("description"), "").isBlank()) {
				parts.add("Description:\n" + linkAsset.get("description"));
			}
			if (!valueOrFallback(linkAsset.get("extractedText"), "").isBlank()) {
				parts.add("Extracted page text:\n" + linkAsset.get("extractedText"));
			}
			if (!valueOrFallback(linkAsset.get("metadataError"), "").isBlank()) {
				parts.add("Link parsing note: " + linkAsset.get("metadataError"));
			}
			linkContextSections.add(parts.stream().filter(part -> part != null && !part.isBlank()).reduce((a, b) -> a + "\n" + b).orElse(""));
		}

		List<String> sections = new java.util.ArrayList<>();
		sections.add("Source " + material.get("sourceNumber") + ": " + material.get("title"));
		if (!valueOrFallback(material.get("description"), "").isBlank()) {
			sections.add("Description:\n" + material.get("description"));
		}
		if (!valueOrFallback(material.get("text"), "").isBlank()) {
			sections.add("Extracted text:\n" + material.get("text"));
		}
		List<String> youtubeUrls = jsonMapReader.stringList(material.get("youtubeUrls"));
		if (!youtubeUrls.isEmpty()) {
			sections.add("YouTube URLs:\n" + youtubeUrls.stream().map(url -> "- " + url).reduce((a, b) -> a + "\n" + b).orElse(""));
		}
		if (!youtubeTranscriptSections.isEmpty()) {
			sections.add("YouTube transcript context:\n" + String.join("\n\n", youtubeTranscriptSections));
		}
		List<String> links = jsonMapReader.stringList(material.get("links"));
		if (!links.isEmpty()) {
			sections.add("Links:\n" + links.stream().map(url -> "- " + url).reduce((a, b) -> a + "\n" + b).orElse(""));
		}
		if (!linkContextSections.isEmpty()) {
			sections.add("Parsed web link context:\n" + String.join("\n\n", linkContextSections));
		}
		List<Map<String, Object>> attachments = jsonMapReader.mapList(material.get("attachments"));
		if (!attachments.isEmpty()) {
			String attachmentLines = attachments.stream()
					.map(attachment -> "- " + attachment.get("name") + " (" + attachment.get("kind") + ")")
					.reduce((a, b) -> a + "\n" + b)
					.orElse("");
			sections.add("Attachments, metadata only:\n" + attachmentLines);
		}
		return sections.stream().filter(part -> part != null && !part.isBlank()).reduce((a, b) -> a + "\n\n" + b).orElse("");
	}

	/**
	 * Formats selected revision preset options for prompt input.
	 *
	 * @param selectedOptions selected option codes
	 * @return prompt-ready selected options text
	 */
	String buildRevisionOptionsText(List<String> selectedOptions) {
		if (selectedOptions == null || selectedOptions.isEmpty()) {
			return "No preset revision options selected.";
		}
		return selectedOptions.stream()
				.map(option -> "- " + REVISION_OPTION_LABELS.getOrDefault(option, option))
				.reduce((a, b) -> a + "\n" + b)
				.orElse("No preset revision options selected.");
	}

	/**
	 * Serializes a revision brief for inclusion in the writer prompt.
	 *
	 * @param brief revision brief model
	 * @return pretty JSON when possible, otherwise fallback text
	 */
	String serializeRevisionBrief(RevisionBriefRecord brief) {
		try {
			return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(brief);
		} catch (Exception ex) {
			return String.valueOf(brief);
		}
	}

	/**
	 * Returns the supported preset revision option codes.
	 *
	 * @return supported revision option codes
	 */
	public Set<String> allowedRevisionOptions() {
		return REVISION_OPTION_LABELS.keySet();
	}

	/**
	 * Returns a non-blank raw text value or the supplied fallback.
	 *
	 * @param value    raw value
	 * @param fallback fallback text
	 * @return raw text or fallback
	 */
	String valueOrFallback(Object value, String fallback) {
		String text = textValueNormalizer.raw(value);
		return text.isBlank() ? fallback : text;
	}
}
