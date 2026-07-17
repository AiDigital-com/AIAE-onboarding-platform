package com.aidigital.aionboarding.service.lesson.support;

import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.service.lesson.models.LessonRevisionPromptRecord;
import com.aidigital.aionboarding.service.lesson.models.RevisionBriefRecord;
import com.aidigital.aionboarding.service.lesson.models.RevisionHistoryEntryRecord;
import com.aidigital.aionboarding.service.lesson.models.RevisionProviderMetadataRecord;
import com.aidigital.aionboarding.service.lesson.util.LessonContentUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Converts typed revision records to JSONB maps at the persistence boundary and assembles
 * the revision brief, provider metadata, and revised lesson content for {@code LessonRevisionServiceImpl}.
 */
@Component
@RequiredArgsConstructor
public class LessonRevisionMetadataMapper {

    private final LessonRecordAssembler lessonRecordAssembler;
    private final LessonContentUtil lessonContentUtil;
    private final LessonHtmlSanitizer lessonHtmlSanitizer;

    /**
     * Converts a typed revision prompt record into a JSONB-ready map.
     *
     * @param prompt the prompt record to convert
     * @return the JSONB map representation
     */
    public Map<String, Object> toMap(LessonRevisionPromptRecord prompt) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("version", prompt.version());
        map.put("cacheKey", prompt.cacheKey());
        map.put("instructions", prompt.instructions());
        map.put("input", prompt.input());
        return map;
    }

    /**
     * Converts a typed provider metadata record into a JSONB-ready map.
     *
     * @param metadata the provider metadata record to convert
     * @return the JSONB map representation
     */
    public Map<String, Object> toMap(RevisionProviderMetadataRecord metadata) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("provider", metadata.provider());
        map.put("model", metadata.model());
        map.put("promptVersion", metadata.promptVersion());
        map.put("promptCacheKey", metadata.promptCacheKey());
        if (metadata.rawOutput() != null) {
            map.put("rawOutput", metadata.rawOutput());
        }
        return map;
    }

    /**
     * Converts a typed revision history entry record into a JSONB-ready map.
     *
     * @param entry the revision history entry record to convert
     * @return the JSONB map representation
     */
    public Map<String, Object> toMap(RevisionHistoryEntryRecord entry) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("revisedAt", entry.revisedAt());
        map.put("revisionRequest", entry.revisionRequest());
        map.put("selectedOptions", entry.selectedOptions());
        map.put("revisionBrief", lessonRecordAssembler.toRevisionBriefMap(entry.revisionBrief()));
        map.put("plannerPrompt", toMap(entry.plannerPrompt()));
        map.put("writerPrompt", toMap(entry.writerPrompt()));
        map.put("planner", toMap(entry.planner()));
        map.put("writer", toMap(entry.writer()));
        return map;
    }

    /**
     * Merges a typed revision entry into the lesson generation metadata JSONB document.
     *
     * @param lesson        the lesson whose generation metadata is being updated
     * @param revisionEntry the revision entry to merge in
     * @return the updated generation metadata map, ready to persist
     */
    public Map<String, Object> mergeRevisionEntry(Lesson lesson, RevisionHistoryEntryRecord revisionEntry) {
        Map<String, Object> entryMap = toMap(revisionEntry);
        Map<String, Object> metadata = lesson.getGenerationMetadata() == null
            ? new HashMap<>()
            : new HashMap<>(lesson.getGenerationMetadata());
        List<Object> revisionHistory = new ArrayList<>();
        Object existingHistory = metadata.get("revisionHistory");
        if (existingHistory instanceof List<?> list) {
            int from = Math.max(0, list.size() - 9);
            revisionHistory.addAll(list.subList(from, list.size()));
        }
        revisionHistory.add(entryMap);

        metadata.put("lastRevisionAt", entryMap.get("revisedAt"));
        metadata.put("lastRevisionRequest", entryMap.get("revisionRequest"));
        metadata.put("lastRevisionScope", entryMap.get("revisionBrief") instanceof Map<?, ?> brief
            ? brief.get("changeScope")
            : revisionEntry.revisionBrief().changeScope());
        metadata.put("lastRevisionOptions", entryMap.get("selectedOptions"));
        metadata.put("lastRevisionPlannerPrompt", entryMap.get("plannerPrompt"));
        metadata.put("lastRevisionWriterPrompt", entryMap.get("writerPrompt"));
        metadata.put("lastRevisionPlanner", entryMap.get("planner"));
        metadata.put("lastRevisionWriter", entryMap.get("writer"));
        metadata.put("revisionHistory", revisionHistory);
        return metadata;
    }

    /**
     * Mutates the lesson entity in place with the revised HTML/markdown content and title.
     *
     * @param lesson         the lesson entity to update
     * @param revisedContent the raw content returned by the writer AI
     */
    public void applyRevisedContent(Lesson lesson, String revisedContent) {
        String revisedHtml = lessonContentUtil.looksLikeHtml(revisedContent)
            ? revisedContent
            : lessonContentUtil.markdownToHtml(revisedContent);
        // Sanitized before title extraction so a stripped tag/attribute can never influence the
        // title pulled from it, and so the persisted HTML is held to the same allowlist as
        // manual authoring and initial generation.
        String sanitizedHtml = lessonHtmlSanitizer.sanitize(revisedHtml);
        String revisedMarkdown = lessonContentUtil.looksLikeHtml(revisedContent)
            ? lesson.getContentMarkdown()
            : revisedContent;
        lesson.setTitle(firstNonBlank(lessonContentUtil.extractHtmlTitle(sanitizedHtml), lesson.getTitle()));
        lesson.setContentHtml(sanitizedHtml);
        lesson.setContentMarkdown(revisedMarkdown == null ? "" : revisedMarkdown);
    }

    /**
     * Builds a typed revision brief record from the planner AI's raw brief map.
     *
     * @param briefMap the raw brief map returned by the planner AI step
     * @return the typed revision brief record
     */
    public RevisionBriefRecord buildRevisionBrief(Map<String, Object> briefMap) {
        return new RevisionBriefRecord(
            stringVal(briefMap.get("changeScope")),
            stringVal(briefMap.get("userIntent")),
            normalizeStringList(briefMap.get("editInstructions")),
            normalizeStringList(briefMap.get("preserveRules")),
            normalizeStringList(briefMap.get("riskNotes"))
        );
    }

    /**
     * Builds a typed provider metadata record from a raw AI step metadata map.
     *
     * @param meta the raw metadata map returned by an AI generation step
     * @return the typed provider metadata record
     */
    public RevisionProviderMetadataRecord buildProviderMetadata(Map<String, Object> meta) {
        return new RevisionProviderMetadataRecord(
            stringVal(meta.get("provider")),
            stringVal(meta.get("model")),
            stringVal(meta.get("promptVersion")),
            stringVal(meta.get("promptCacheKey")),
            stringVal(meta.get("rawOutput"))
        );
    }

    /**
     * Safely converts an arbitrary value to a non-null trimmed string. Returns an empty string
     * when {@code value} is {@code null}.
     *
     * @param value the value to convert
     * @return the trimmed string representation, or {@code ""} for {@code null}
     */
    String stringVal(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    /**
     * Converts a raw value from a parsed map into a list of non-blank trimmed strings.
     * Returns an empty list if {@code value} is not a {@link List}.
     *
     * @param value the raw value from the parsed map
     * @return a list of trimmed, non-blank strings
     */
    @SuppressWarnings("unchecked")
    List<String> normalizeStringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return ((List<Object>) list).stream()
            .map(item -> item == null ? "" : String.valueOf(item).trim())
            .filter(s -> !s.isBlank())
            .toList();
    }

    /**
     * Returns the first non-blank string from the given values, or empty string if none.
     *
     * @param values candidate strings in preference order
     * @return first non-blank value, or {@code ""}
     */
    String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
