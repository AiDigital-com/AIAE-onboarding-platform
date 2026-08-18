package com.aidigital.aionboarding.service.lessonactivity.support;

import com.aidigital.aionboarding.domain.common.dictionary.ActivityTypeCode;
import com.aidigital.aionboarding.service.lessonactivity.enums.QuizQuestionTypeResolver;
import com.aidigital.aionboarding.service.lessonactivity.models.QuizQuestionItemRecord;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LessonActivityPayloadAssembler {

    private final QuizQuestionTypeResolver quizQuestionTypeResolver;

    public int getActivityItemCount(String type, Map<String, Object> payload) {
        if (ActivityTypeCode.FLASHCARDS.equals(type)) {
            return asMapList(payload.get("cards")).size();
        }
        return asMapList(payload.get("items")).size();
    }

    public int parseInt(Object value, int fallback) {
        try {
            return value == null ? fallback : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    public String stringVal(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> asMapList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> maps = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                maps.add((Map<String, Object>) map);
            }
        }
        return maps;
    }

    @SuppressWarnings("unchecked")
    public List<String> asStringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<String> strings = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof String stringValue) {
                strings.add(stringValue);
            }
        }
        return strings;
    }

    /**
     * Converts a raw value into a list of string lists, one per quiz question, tolerating both a
     * fresh submission (already {@code List<List<String>>}) and a JSONB round-trip (each nested
     * list read back as {@code List<Object>}).
     *
     * @param value raw value, expected to be a list of per-question answer lists
     * @return normalized list of string lists; non-list elements are dropped
     */
    public List<List<String>> asListOfStringLists(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<List<String>> result = new ArrayList<>();
        for (Object item : list) {
            result.add(asStringList(item));
        }
        return result;
    }

    /**
     * Parses a persisted quiz activity's raw JSONB payload into typed question items, isolating
     * the {@code Map<String, Object>} boundary here so
     * {@link com.aidigital.aionboarding.service.lessonactivity.services.LessonActivityGradingService}
     * never needs to see it.
     *
     * @param activityPayload persisted quiz activity payload containing an {@code items} array
     * @return typed question items in stored order
     */
    public List<QuizQuestionItemRecord> parseQuizItems(Map<String, Object> activityPayload) {
        List<Map<String, Object>> items = asMapList(activityPayload.get("items"));
        List<QuizQuestionItemRecord> parsed = new ArrayList<>();
        for (Map<String, Object> item : items) {
            List<String> correctAnswers = asStringList(item.get("correctAnswers"));
            String correctAnswer = stringVal(item.get("correctAnswer"));
            if (correctAnswers.isEmpty() && !correctAnswer.isBlank()) {
                correctAnswers = List.of(correctAnswer);
            }
            parsed.add(new QuizQuestionItemRecord(
                    quizQuestionTypeResolver.resolve(stringVal(item.get("type"))).value(),
                    stringVal(item.get("question")),
                    asStringList(item.get("options")),
                    correctAnswers,
                    stringVal(item.get("explanation"))
            ));
        }
        return parsed;
    }
}
