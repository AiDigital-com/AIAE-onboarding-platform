package com.aidigital.aionboarding.service.lessonactivity.support;

import com.aidigital.aionboarding.service.lessonactivity.enums.QuizQuestionType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Validates and normalizes one author-submitted or model-generated quiz question into its stored
 * payload shape, applying type-specific option and correct-answer rules.
 */
@Component
public class QuizQuestionValidator {

    /** Blank marker shown to authors as the canonical example for fill-in-the-blanks questions. */
    public static final String FILL_IN_BLANKS_MARKER = "_____";

    /**
     * Detects a fill-in-the-blanks marker as a run of 3 or more underscores. Models are
     * inconsistent about the exact underscore count even when instructed, so an exact-length
     * match against {@link #FILL_IN_BLANKS_MARKER} would silently drop otherwise-valid questions.
     */
    private static final Pattern BLANK_MARKER_PATTERN = Pattern.compile("_{3,}");

    static final List<String> TRUE_FALSE_OPTIONS = List.of("True", "False");
    private static final int MAX_OPTIONS = 6;

    /**
     * Validates and normalizes one quiz question, applying type-specific option and answer rules.
     *
     * @param rawType question type value; missing or unrecognized values default to multiple_choice
     * @param rawQuestion question text
     * @param rawOptions candidate answer options in author/model order
     * @param rawCorrectAnswer correct answer value
     * @param rawExplanation optional explanation shown after answering
     * @return normalized question payload map, or {@code null} when the question fails validation
     */
    public Map<String, Object> normalize(
        String rawType, String rawQuestion, List<String> rawOptions, String rawCorrectAnswer, String rawExplanation
    ) {
        return normalize(rawType, rawQuestion, rawOptions, rawCorrectAnswer, null, rawExplanation);
    }

    /**
     * Validates and normalizes one quiz question, including optional multi-correct answers.
     *
     * @param rawType question type value
     * @param rawQuestion question text
     * @param rawOptions candidate answer options
     * @param rawCorrectAnswer primary correct answer
     * @param rawCorrectAnswers optional multi-correct answers (multiple_choice)
     * @param rawExplanation optional explanation
     * @return normalized question payload map, or {@code null} when invalid
     */
    public Map<String, Object> normalize(
        String rawType,
        String rawQuestion,
        List<String> rawOptions,
        String rawCorrectAnswer,
        List<String> rawCorrectAnswers,
        String rawExplanation
    ) {
        QuizQuestionType type = QuizQuestionType.fromValue(rawType);
        String question = rawQuestion == null ? "" : rawQuestion.trim();
        if (question.isBlank()) {
            return null;
        }
        if (type == QuizQuestionType.FILL_IN_BLANKS_WITH_OPTIONS && !hasSingleBlankMarker(question)) {
            return null;
        }

        List<String> options = type == QuizQuestionType.TRUE_FALSE
            ? TRUE_FALSE_OPTIONS
            : normalizeOptions(rawOptions);
        if (options.size() < 2 || hasDuplicateOptions(options)) {
            return null;
        }

        List<String> correctAnswers = resolveCorrectAnswers(type, options, rawCorrectAnswer, rawCorrectAnswers);
        if (correctAnswers.isEmpty()) {
            return null;
        }

        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("type", type.value());
        normalized.put("question", question);
        normalized.put("options", options);
        normalized.put("correctAnswer", correctAnswers.get(0));
        if (type == QuizQuestionType.MULTIPLE_CHOICE && correctAnswers.size() > 1) {
            normalized.put("correctAnswers", correctAnswers);
        }
        normalized.put("explanation", rawExplanation == null ? "" : rawExplanation.trim());
        return normalized;
    }

    /**
     * Checks whether the question text contains exactly one blank marker (a run of 3 or more
     * underscores), regardless of the exact underscore count used.
     *
     * @param question trimmed question text
     * @return {@code true} when exactly one blank marker is present
     */
    boolean hasSingleBlankMarker(String question) {
        Matcher matcher = BLANK_MARKER_PATTERN.matcher(question);
        int matchCount = 0;
        while (matcher.find()) {
            matchCount++;
            if (matchCount > 1) {
                return false;
            }
        }
        return matchCount == 1;
    }

    /**
     * Describes why a question that failed {@link #normalize} is invalid, so the author can be
     * told exactly what to fix instead of having the question silently dropped.
     *
     * @param rawType question type value; missing or unrecognized values default to multiple_choice
     * @param rawQuestion question text
     * @param rawOptions candidate answer options in author order
     * @param rawCorrectAnswer correct answer value
     * @return a human-readable reason, or {@code null} when the question is actually valid
     */
    public String describeFailure(String rawType, String rawQuestion, List<String> rawOptions, String rawCorrectAnswer) {
        return describeFailure(rawType, rawQuestion, rawOptions, rawCorrectAnswer, null);
    }

    /**
     * Describes why a question failed validation, including multi-correct payloads.
     */
    public String describeFailure(
        String rawType,
        String rawQuestion,
        List<String> rawOptions,
        String rawCorrectAnswer,
        List<String> rawCorrectAnswers
    ) {
        QuizQuestionType type = QuizQuestionType.fromValue(rawType);
        String question = rawQuestion == null ? "" : rawQuestion.trim();
        if (question.isBlank()) {
            return "add a question";
        }
        if (type == QuizQuestionType.FILL_IN_BLANKS_WITH_OPTIONS && !hasSingleBlankMarker(question)) {
            return "include exactly one blank marker, e.g. " + FILL_IN_BLANKS_MARKER;
        }
        List<String> options = type == QuizQuestionType.TRUE_FALSE
            ? TRUE_FALSE_OPTIONS
            : normalizeOptions(rawOptions);
        if (options.size() < 2) {
            return "add at least two answer options";
        }
        if (hasDuplicateOptions(options)) {
            return "option text must be unique within a question";
        }
        List<String> correctAnswers = resolveCorrectAnswers(type, options, rawCorrectAnswer, rawCorrectAnswers);
        if (correctAnswers.isEmpty()) {
            return "select the correct answer";
        }
        return null;
    }

    /**
     * Trims, drops blank values from, and caps a raw options list at the supported maximum.
     *
     * @param rawOptions raw option values
     * @return normalized options
     */
    List<String> normalizeOptions(List<String> rawOptions) {
        if (rawOptions == null) {
            return List.of();
        }
        return rawOptions.stream()
            .map(value -> value == null ? "" : value.trim())
            .filter(value -> !value.isBlank())
            .limit(MAX_OPTIONS)
            .toList();
    }

    /**
     * Matches a correct-answer value against the normalized option list.
     *
     * @param options normalized options
     * @param rawCorrectAnswer raw correct-answer value
     * @return the matching option, or {@code null} when it matches none of the options
     */
    String matchOption(List<String> options, String rawCorrectAnswer) {
        String correctAnswer = rawCorrectAnswer == null ? "" : rawCorrectAnswer.trim();
        return options.contains(correctAnswer) ? correctAnswer : null;
    }

    /**
     * Matches a correct-answer value against the canonical True/False options, case-insensitively.
     *
     * @param rawCorrectAnswer raw correct-answer value
     * @return {@code "True"} or {@code "False"}, or {@code null} when it matches neither
     */
    String matchTrueFalse(String rawCorrectAnswer) {
        String correctAnswer = rawCorrectAnswer == null ? "" : rawCorrectAnswer.trim();
        for (String option : TRUE_FALSE_OPTIONS) {
            if (option.equalsIgnoreCase(correctAnswer)) {
                return option;
            }
        }
        return null;
    }

    private boolean hasDuplicateOptions(List<String> options) {
        return options.stream().map(value -> value.toLowerCase()).distinct().count() != options.size();
    }

    private List<String> resolveCorrectAnswers(
        QuizQuestionType type,
        List<String> options,
        String rawCorrectAnswer,
        List<String> rawCorrectAnswers
    ) {
        if (type == QuizQuestionType.TRUE_FALSE) {
            String matched = matchTrueFalse(rawCorrectAnswer);
            if (matched == null && rawCorrectAnswers != null && !rawCorrectAnswers.isEmpty()) {
                matched = matchTrueFalse(rawCorrectAnswers.get(0));
            }
            return matched == null ? List.of() : List.of(matched);
        }

        List<String> candidates = new java.util.ArrayList<>();
        if (rawCorrectAnswers != null) {
            for (String value : rawCorrectAnswers) {
                String matched = matchOption(options, value);
                if (matched != null && !candidates.contains(matched)) {
                    candidates.add(matched);
                }
            }
        }
        if (candidates.isEmpty()) {
            String matched = matchOption(options, rawCorrectAnswer);
            if (matched != null) {
                candidates.add(matched);
            }
        }
        if (type != QuizQuestionType.MULTIPLE_CHOICE && candidates.size() > 1) {
            return List.of(candidates.get(0));
        }
        return candidates;
    }
}
