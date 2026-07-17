package com.aidigital.aionboarding.service.material.support;

import com.aidigital.aionboarding.service.material.models.MaterialPreparationItem;
import com.aidigital.aionboarding.service.material.models.SignalItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts example and caveat sentences from prepared material text.
 */
@Component
@RequiredArgsConstructor
public class SignalExtractor {

    static final List<String> EXAMPLE_KEYWORDS = List.of(
        "for example", "for instance", "such as", "e.g.", "like ", "including ");
    static final List<String> CAVEAT_KEYWORDS = List.of(
        "however", "warning", "caution", "note:", "important:", "be careful", "avoid", "do not");
    static final int MAX_SIGNALS = 8;

    private static final Pattern SENTENCE_PATTERN = Pattern.compile("[^.!?]+[.!?]+");

    /**
     * Extracts up to {@value #MAX_SIGNALS} example sentences from the given materials.
     *
     * @param materials prepared material items to scan
     * @return list of example signal items with source attribution
     */
    public List<SignalItem> extractExamples(List<MaterialPreparationItem> materials) {
        return extractSignals(materials, EXAMPLE_KEYWORDS);
    }

    /**
     * Extracts up to {@value #MAX_SIGNALS} caveat sentences from the given materials.
     *
     * @param materials prepared material items to scan
     * @return list of caveat signal items with source attribution
     */
    public List<SignalItem> extractCaveats(List<MaterialPreparationItem> materials) {
        return extractSignals(materials, CAVEAT_KEYWORDS);
    }

    List<SignalItem> extractSignals(List<MaterialPreparationItem> materials, List<String> keywords) {
        List<SignalItem> signals = new ArrayList<>();
        for (MaterialPreparationItem material : materials) {
            if (signals.size() >= MAX_SIGNALS) {
                break;
            }
            String sourceText = buildSourceText(material);
            Matcher matcher = SENTENCE_PATTERN.matcher(sourceText);
            while (matcher.find() && signals.size() < MAX_SIGNALS) {
                String sentence = matcher.group().trim();
                if (containsKeyword(sentence, keywords)) {
                    signals.add(new SignalItem(material.sourceNumber(), sentence));
                }
            }
        }
        return signals;
    }

    boolean containsKeyword(String sentence, List<String> keywords) {
        String lower = sentence.toLowerCase(Locale.ROOT);
        for (String keyword : keywords) {
            if (lower.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    String buildSourceText(MaterialPreparationItem material) {
        Map<String, Object> data = material.data();
        StringBuilder builder = new StringBuilder();
        appendNormalized(builder, data.get("title"));
        appendNormalized(builder, data.get("description"));
        appendNormalized(builder, data.get("text"));
        appendLinkText(builder, data.get("linkAssets"));
        appendTranscriptText(builder, data.get("youtubeTranscripts"));
        return builder.toString();
    }

    void appendNormalized(StringBuilder builder, Object value) {
        if (value == null) {
            return;
        }
        String text = String.valueOf(value).trim();
        if (!text.isBlank()) {
            builder.append(text).append("\n");
        }
    }

    @SuppressWarnings("unchecked")
    void appendLinkText(StringBuilder builder, Object linkAssets) {
        if (!(linkAssets instanceof List<?> list)) {
            return;
        }
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                appendNormalized(builder, map.get("extractedText"));
            }
        }
    }

    @SuppressWarnings("unchecked")
    void appendTranscriptText(StringBuilder builder, Object transcripts) {
        if (!(transcripts instanceof List<?> list)) {
            return;
        }
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                appendNormalized(builder, map.get("preparedText"));
            }
        }
    }
}
