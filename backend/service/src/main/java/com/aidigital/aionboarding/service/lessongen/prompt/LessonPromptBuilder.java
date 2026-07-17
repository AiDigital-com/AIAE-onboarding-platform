package com.aidigital.aionboarding.service.lessongen.prompt;

import com.aidigital.aionboarding.service.common.mapping.JsonMapReader;
import com.aidigital.aionboarding.service.common.mapping.TextValueNormalizer;
import com.aidigital.aionboarding.service.lessongen.model.LessonGenPrompt;
import com.aidigital.aionboarding.service.material.models.DuplicateTitle;
import com.aidigital.aionboarding.service.material.models.DuplicateUrl;
import com.aidigital.aionboarding.service.material.models.MaterialPreparationItem;
import com.aidigital.aionboarding.service.material.models.OverlapNotes;
import com.aidigital.aionboarding.service.material.models.PreparedMaterialsResult;
import com.aidigital.aionboarding.service.material.models.SignalItem;
import com.aidigital.aionboarding.service.material.models.SignalNotes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class LessonPromptBuilder {

    private final TextValueNormalizer textValueNormalizer;
    private final JsonMapReader jsonMapReader;

    /**
     * Builds a default theoretical lesson prompt from prepared materials.
     *
     * @param preparedMaterials prepared material context
     * @return lesson-generation prompt
     */
    public LessonGenPrompt buildTheoreticalLessonPrompt(PreparedMaterialsResult preparedMaterials) {
        return buildTheoreticalLessonPrompt(
            preparedMaterials,
            "",
            "standard",
            "clear",
            "structured theoretical lesson",
            List.of()
        );
    }

    /**
     * Builds a theoretical lesson prompt from prepared materials and user-selected generation settings.
     *
     * @param preparedMaterials prepared material context
     * @param userInstructions optional user instructions
     * @param depth requested depth
     * @param tone requested tone
     * @param desiredFormat requested lesson format
     * @param fileInputs OpenAI file inputs to attach
     * @return lesson-generation prompt
     */
    @SuppressWarnings("unchecked")
    public LessonGenPrompt buildTheoreticalLessonPrompt(
        PreparedMaterialsResult preparedMaterials,
        String userInstructions,
        String depth,
        String tone,
        String desiredFormat,
        List<Map<String, Object>> fileInputs
    ) {
        List<Map<String, Object>> materials = preparedMaterials.materials().stream()
            .map(MaterialPreparationItem::data)
            .toList();
        String sourceText = materials.stream()
            .map(this::formatSourceMaterial)
            .reduce((left, right) -> left + "\n\n---\n\n" + right)
            .orElse("");
        boolean hasSourceMaterials = !materials.isEmpty();
        String extractedTerms = preparedMaterials.extractedTerms().isEmpty()
            ? "No candidate terms extracted."
            : String.join(", ", preparedMaterials.extractedTerms());

        List<String> inputParts = new ArrayList<>();
        inputParts.add(hasSourceMaterials
            ? "Create one coherent theoretical lesson from the selected materials."
            : "Create one coherent theoretical lesson from the user-provided topic/instructions.");
        inputParts.add("");
        inputParts.add("Request-specific settings:");
        inputParts.add("- Source mode: " + (hasSourceMaterials ? "selected materials provided" : "prompt-only topic"));
        inputParts.add("- Depth: " + depth);
        inputParts.add("- Tone: " + tone);
        inputParts.add("- Desired format: " + desiredFormat);
        inputParts.add(userInstructions != null && !userInstructions.isBlank()
            ? "- Extra user instructions: " + userInstructions
            : "- Extra user instructions: none");
        inputParts.add("");
        inputParts.add("Preparation notes:");
        inputParts.add("- Materials count: " + preparedMaterials.stats().materialCount());
        inputParts.add("- Combined extracted text characters: " + preparedMaterials.stats().combinedTextCharacters());
        inputParts.add("- Candidate key terms: " + extractedTerms);
        inputParts.add("- Overlap check: " + formatOverlaps(preparedMaterials.overlaps()));
        inputParts.add("- Lesson signals: " + formatSignals(preparedMaterials.signals()));
        inputParts.add("");
        inputParts.add(hasSourceMaterials
            ? "Selected source materials:"
            : "Selected source materials: none. Use the extra user instructions as the topic.");
        if (hasSourceMaterials) {
            inputParts.add(sourceText);
        }

        return new LessonGenPrompt(
            LessonPromptConstants.LESSON_PROMPT_VERSION,
            LessonPromptConstants.LESSON_PROMPT_VERSION,
            LessonPromptConstants.LESSON_INSTRUCTIONS,
            String.join("\n", inputParts),
            fileInputs == null ? List.of() : fileInputs
        );
    }

    /**
     * Builds a prompt that asks the model to compress a long transcript while
     * keeping facts, explanations, and terminology intact.
     */
    public LessonGenPrompt buildTranscriptCondensationPrompt(String transcriptText) {
        String instructions = """
            Condense the transcript below. Remove filler words, repeated phrases,
            timestamps, and off-topic chatter. Keep all key facts, explanations,
            examples, and terminology. Output only the condensed text, with no
            introduction or commentary.
            """.stripIndent().trim();
        return new LessonGenPrompt(
            "transcript-condensation-v1",
            "transcript-condensation-v1",
            instructions,
            transcriptText,
            List.of()
        );
    }

    /**
     * Formats one prepared source material for prompt context.
     *
     * @param material prepared material map
     * @return source material context text
     */
    @SuppressWarnings("unchecked")
    String formatSourceMaterial(Map<String, Object> material) {
        List<String> youtubeTranscriptSections = jsonMapReader.mapList(material.get("youtubeTranscripts")).stream()
            .map(transcript -> {
                boolean wasCondensed = Boolean.TRUE.equals(transcript.get("wasCondensed"));
                String label = wasCondensed
                    ? "Condensed YouTube transcript, filler removed"
                    : "YouTube transcript";
                String preparedText = textValueNormalizer.trimmed(transcript.get("preparedText"));
                if (!preparedText.isBlank()) {
                    return label + " for " + textValueNormalizer.trimmed(transcript.get("url")) + ":\n" + preparedText;
                }
                String error = textValueNormalizer.trimmed(transcript.get("error"));
                return "YouTube transcript unavailable for " + textValueNormalizer.trimmed(transcript.get("url")) + "."
                    + (error.isBlank() ? "" : "\nReason: " + error);
            })
            .toList();

        List<String> linkContextSections = new ArrayList<>();
        List<Map<String, Object>> linkAssets = jsonMapReader.mapList(material.get("linkAssets"));
        for (int index = 0; index < linkAssets.size(); index += 1) {
            Map<String, Object> linkAsset = linkAssets.get(index);
            String label = textValueNormalizer.firstNonBlankTrimmed(
                textValueNormalizer.trimmed(linkAsset.get("title")),
                "Web link " + (index + 1));
            List<String> parts = new ArrayList<>();
            parts.add("Web source: " + label);
            parts.add("URL: " + textValueNormalizer.trimmed(linkAsset.get("url")));
            if (!textValueNormalizer.trimmed(linkAsset.get("siteName")).isBlank()) {
                parts.add("Site: " + textValueNormalizer.trimmed(linkAsset.get("siteName")));
            }
            if (!textValueNormalizer.trimmed(linkAsset.get("description")).isBlank()) {
                parts.add("Description:\n" + textValueNormalizer.trimmed(linkAsset.get("description")));
            }
            if (!textValueNormalizer.trimmed(linkAsset.get("extractedText")).isBlank()) {
                parts.add("Extracted page text:\n" + textValueNormalizer.trimmed(linkAsset.get("extractedText")));
            }
            if (!textValueNormalizer.trimmed(linkAsset.get("metadataError")).isBlank()) {
                parts.add("Link parsing note: " + textValueNormalizer.trimmed(linkAsset.get("metadataError")));
            }
            linkContextSections.add(parts.stream().filter(part -> !part.isBlank()).reduce((a, b) -> a + "\n" + b).orElse(""));
        }

        List<String> sections = new ArrayList<>();
        sections.add("Source " + material.get("sourceNumber") + ": " + textValueNormalizer.trimmed(material.get("title")));
        if (!textValueNormalizer.trimmed(material.get("description")).isBlank()) {
            sections.add("Description:\n" + textValueNormalizer.trimmed(material.get("description")));
        }
        if (!textValueNormalizer.trimmed(material.get("text")).isBlank()) {
            sections.add("Extracted text:\n" + textValueNormalizer.trimmed(material.get("text")));
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
                .map(attachment -> {
                    String openaiStatus = !textValueNormalizer.trimmed(attachment.get("openaiFileId")).isBlank()
                        ? "attached to this OpenAI request as " + textValueNormalizer.trimmed(attachment.get("openaiFileId"))
                        : "metadata only";
                    return "- " + textValueNormalizer.trimmed(attachment.get("name"))
                        + " (" + textValueNormalizer.trimmed(attachment.get("kind"))
                        + ", " + textValueNormalizer.firstNonBlankTrimmed(
                            textValueNormalizer.trimmed(attachment.get("mimeType")), "unknown MIME")
                        + ", " + openaiStatus + ")";
                })
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
            sections.add("Attachments:\n" + attachmentLines);
        }

        return sections.stream().filter(section -> !section.isBlank()).reduce((a, b) -> a + "\n\n" + b).orElse("");
    }

    /**
     * Formats duplicate-title and duplicate-URL overlap notes.
     *
     * @param overlaps overlap notes
     * @return prompt-ready overlap text
     */
    String formatOverlaps(OverlapNotes overlaps) {
        List<String> notes = new ArrayList<>();
        if (!overlaps.duplicateTitles().isEmpty()) {
            notes.add("Duplicate or repeated titles: "
                + overlaps.duplicateTitles().stream()
                .map(item -> "\"" + item.title() + "\"")
                .reduce((a, b) -> a + ", " + b)
                .orElse("")
                + ".");
        }
        if (!overlaps.duplicateUrls().isEmpty()) {
            notes.add("Repeated URLs: "
                + overlaps.duplicateUrls().stream()
                .map(DuplicateUrl::url)
                .reduce((a, b) -> a + ", " + b)
                .orElse("")
                + ".");
        }
        return notes.isEmpty() ? "No obvious duplicate titles or URLs detected." : String.join("\n", notes);
    }

    /**
     * Formats source-derived examples and caveats for prompt context.
     *
     * @param signals extracted source signals
     * @return prompt-ready signal text
     */
    String formatSignals(SignalNotes signals) {
        List<String> parts = new ArrayList<>();
        if (!signals.examples().isEmpty()) {
            parts.add("Possible examples from sources:\n"
                + signals.examples().stream()
                .map(item -> "- Source " + item.sourceNumber() + ": " + item.text())
                .reduce((a, b) -> a + "\n" + b)
                .orElse(""));
        }
        if (!signals.caveats().isEmpty()) {
            parts.add("Possible caveats or warnings from sources:\n"
                + signals.caveats().stream()
                .map(item -> "- Source " + item.sourceNumber() + ": " + item.text())
                .reduce((a, b) -> a + "\n" + b)
                .orElse(""));
        }
        return parts.isEmpty() ? "No example or caveat signals detected." : String.join("\n\n", parts);
    }

}
