package com.aidigital.aionboarding.service.material.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Typed result of material preparation, replacing the raw {@code Map<String, Object>}
 * previously returned by {@code MaterialPreparationService}.
 *
 * @param materials       prepared material items in source order
 * @param sourceReferences source reference items in source order
 * @param extractedTerms  candidate capitalized terms extracted from the materials
 * @param signals         example and caveat sentences extracted from the materials
 * @param overlaps        detected duplicate titles and URLs across materials
 * @param stats           preparation statistics
 */
public record PreparedMaterialsResult(
    List<MaterialPreparationItem> materials,
    List<SourceReferenceItem> sourceReferences,
    List<String> extractedTerms,
    SignalNotes signals,
    OverlapNotes overlaps,
    PreparationStats stats
) {

    /**
     * Returns a legacy {@code Map<String, Object>} representation matching the shape
     * previously produced by {@code MaterialPreparationServiceImpl}.
     *
     * <p>This is a transitional compatibility helper for callers that have not yet been
     * migrated to the typed record. It should be removed once all consumers consume the
     * typed fields directly.
     *
     * @return legacy map representation of this result
     */
    public Map<String, Object> toLegacyMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("materials", materials.stream().map(MaterialPreparationItem::data).toList());
        result.put("sourceReferences", sourceReferences.stream().map(SourceReferenceItem::data).toList());
        result.put("extractedTerms", extractedTerms);
        result.put("signals", signalMap(signals));
        result.put("overlaps", overlapMap(overlaps));
        result.put("stats", Map.of(
            "materialCount", stats.materialCount(),
            "combinedTextCharacters", stats.combinedTextCharacters()));
        return result;
    }

    private Map<String, Object> signalMap(SignalNotes signalNotes) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("examples", toSignalMaps(signalNotes.examples()));
        map.put("caveats", toSignalMaps(signalNotes.caveats()));
        return map;
    }

    private List<Map<String, Object>> toSignalMaps(List<SignalItem> items) {
        return items.stream().map(item -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("sourceNumber", item.sourceNumber());
            map.put("text", item.text());
            return map;
        }).toList();
    }

    private Map<String, Object> overlapMap(OverlapNotes overlapNotes) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("duplicateTitles", overlapNotes.duplicateTitles().stream()
            .map(title -> {
                Map<String, Object> m = new HashMap<>();
                m.put("title", title.title());
                return m;
            })
            .toList());
        map.put("duplicateUrls", overlapNotes.duplicateUrls().stream()
            .map(url -> {
                Map<String, Object> m = new HashMap<>();
                m.put("url", url.url());
                return m;
            })
            .toList());
        return map;
    }
}
