package com.aidigital.aionboarding.service.lessongen.prompt;

import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.mapping.JsonMapReader;
import com.aidigital.aionboarding.service.common.mapping.TextValueNormalizer;
import com.aidigital.aionboarding.service.lessongen.model.ActivityCountLimits;
import com.aidigital.aionboarding.service.lessongen.model.ActivityRequest;
import com.aidigital.aionboarding.service.lessongen.model.LessonGenPrompt;
import com.aidigital.aionboarding.service.lesson.util.OpenAiPromptCacheUtil;
import com.aidigital.aionboarding.service.lessonactivity.models.FlashcardRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.QuizItemRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.UpdateActivityInput;
import com.aidigital.aionboarding.service.lessonactivity.support.QuizQuestionValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ActivityPromptBuilder {

    private static final Map<String, String> ACTIVITY_LABELS = Map.of(
        "quiz", "quiz",
        "flashcards", "flashcards"
    );

    private final OpenAiPromptCacheUtil openAiPromptCacheUtil;
    private final TextValueNormalizer textValueNormalizer;
    private final JsonMapReader jsonMapReader;
    private final QuizQuestionValidator quizQuestionValidator;

    /**
     * Normalizes a raw activity request map into supported type/count values.
     *
     * @param input raw request map
     * @return normalized activity request
     */
    public ActivityRequest normalizeActivityRequest(Map<String, Object> input) {
        String type = ACTIVITY_LABELS.containsKey(textValueNormalizer.trimmed(input.get("type")))
            ? textValueNormalizer.trimmed(input.get("type"))
            : "quiz";
        int count = clampCount(type, input.get("count"));
        return new ActivityRequest(type, count, ActivityPromptConstants.LESSON_ACTIVITY_LIMITS);
    }

    /**
     * Normalizes an activity type/count pair into supported values.
     *
     * @param type requested activity type
     * @param count requested item count
     * @return normalized activity request
     */
    public ActivityRequest normalizeActivityRequest(String type, Integer count) {
        Map<String, Object> input = new HashMap<>();
        input.put("type", type);
        input.put("count", count);
        return normalizeActivityRequest(input);
    }

    /**
     * Builds the OpenAI prompt for lesson activity generation.
     *
     * @param lesson source lesson entity
     * @param type requested activity type
     * @param count requested item count
     * @return lesson activity generation prompt
     */
    public LessonGenPrompt buildLessonActivityPrompt(Lesson lesson, String type, int count) {
        String normalizedType = ACTIVITY_LABELS.containsKey(type) ? type : "quiz";
        int normalizedCount = clampCount(normalizedType, count);
        String lessonText = stripHtml(textValueNormalizer.firstNonBlankTrimmed(
            lesson.getContentHtml(), lesson.getContentMarkdown()));

        List<String> instructions = new ArrayList<>();
        instructions.add("You create practical learning activities from an internal onboarding lesson.");
        instructions.add("Use only the lesson content as the factual source. Do not invent company-specific rules, metrics, or platform behavior.");
        instructions.add("Return valid JSON only. Do not wrap the JSON in Markdown fences.");
        instructions.add("Generate exactly " + normalizedCount + " "
            + ("quiz".equals(normalizedType) ? "quiz questions" : "flashcards") + ".");
        instructions.add("");

        if ("quiz".equals(normalizedType)) {
            instructions.add(String.join("\n",
                "Quiz requirements:",
                "- Make questions varied and useful for checking understanding.",
                "- Include a mix of definitions, terminology, applied situations, conceptual distinctions, correct sequence/order questions, and common mistake checks when the lesson supports them.",
                "- Every question must include a \"type\" field set to exactly one of: single_choice, multiple_choice, true_false, fill_in_blanks_with_options. Generate a reasonable mix of these four types.",
                "- single_choice: provide 2 to 6 answer options; use this whenever exactly one option is correct. The correctAnswer must exactly match one option.",
                "- multiple_choice: provide 2 to 6 answer options; use this only when more than one option is genuinely correct, and list every correct option in correctAnswers.",
                "- true_false: provide exactly two options, \"True\" and \"False\"; the correctAnswer must be \"True\" or \"False\".",
                "- fill_in_blanks_with_options: the question text must contain exactly one blank marker \"_____\"; provide 2 to 4 answer options for the blank; the correctAnswer must exactly match one option.",
                "- Explanation should be helpful, not just a quote. It may explain the whole concept or use elimination logic to say why wrong options do not fit.",
                "- Avoid trick questions."
            ));
        } else {
            instructions.add(String.join("\n",
                "Flashcard requirements:",
                "- Focus on terms, abbreviations, key concepts, platform mechanics, useful distinctions, and compact scenario prompts from the lesson.",
                "- Front must be a complete study prompt that tells the learner what to recall or explain.",
                "- Never put only a bare term, abbreviation, or phrase on the front. For definitions, write a question like \"What is broad match?\" or a recall prompt like \"Explain what broad match means and when it is used.\"",
                "- If the card checks a distinction, make the front explicit, for example \"How is broad match different from phrase match?\"",
                "- If the card checks a scenario, make the task explicit, for example \"What match type would you choose in this situation, and why?\"",
                "- Front should still be short enough to study from, usually one sentence.",
                "- Back should be concise but complete. Include the explanation directly in the back text when it helps retention.",
                "- Do not make cards that are just vague trivia."
            ));
        }

        instructions.add("");
        instructions.add("Return this exact JSON shape:");
        instructions.add(getActivityShape(normalizedType));

        String input = String.join("\n",
            "Lesson title:",
            textValueNormalizer.firstNonBlankTrimmed(lesson.getTitle(), "Untitled lesson"),
            "",
            "Lesson content:",
            lessonText.isBlank() ? "No lesson content found." : lessonText
        );

        return new LessonGenPrompt(
            ActivityPromptConstants.LESSON_ACTIVITY_PROMPT_VERSION,
            openAiPromptCacheUtil.build("lesson-activity",
                ActivityPromptConstants.LESSON_ACTIVITY_PROMPT_VERSION,
                lesson.getId() == null ? "unknown-lesson" : lesson.getId(),
                normalizedType,
                normalizedCount),
            String.join("\n", instructions),
            input
        );
    }

    public Map<String, Object> normalizeGeneratedActivityPayload(
        Map<String, Object> rawPayload,
        ActivityRequest request
    ) {
        if (rawPayload == null || rawPayload.isEmpty()) {
            throw new AppException(ErrorReason.C003, "OpenAI returned an invalid activity.");
        }

        if ("flashcards".equals(request.type())) {
            return normalizeFlashcardsPayload(rawPayload, request.count());
        }
        return normalizeQuizPayload(rawPayload, request.count());
    }

    /**
     * Normalizes a user-edited quiz payload before persistence.
     *
     * @param input raw update input
     * @return normalized quiz payload
     */
    public Map<String, Object> normalizeQuizUpdatePayload(UpdateActivityInput input) {
        List<QuizItemRecord> items = input.items() == null ? List.of() : input.items();
        if (items.isEmpty()) {
            throw new AppException(ErrorReason.C002, "Add at least one valid quiz question.");
        }

        List<Map<String, Object>> normalizedItems = new ArrayList<>();
        for (int index = 0; index < items.size(); index++) {
            QuizItemRecord item = items.get(index);
            Map<String, Object> normalized = quizQuestionValidator.normalize(
                item.type(),
                item.question(),
                item.options(),
                item.correctAnswer(),
                item.correctAnswers(),
                item.explanation());
            if (normalized == null) {
                String reason = quizQuestionValidator.describeFailure(
                    item.type(), item.question(), item.options(), item.correctAnswer(), item.correctAnswers());
                throw new AppException(ErrorReason.C002, "Question " + (index + 1) + ": " + reason + ".");
            }
            normalizedItems.add(normalized);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "quiz");
        payload.put("title", textValueNormalizer.firstNonBlankTrimmed(normalizeString(input.title()), "Lesson quiz"));
        payload.put("items", normalizedItems);
        return payload;
    }

    /**
     * Normalizes a user-edited flashcards payload before persistence.
     *
     * @param input raw update input
     * @return normalized flashcards payload
     */
    public Map<String, Object> normalizeFlashcardsUpdatePayload(UpdateActivityInput input) {
        List<FlashcardRecord> cards = input.cards() == null ? List.of() : input.cards();
        List<Map<String, Object>> normalizedCards = new ArrayList<>();

        for (int index = 0; index < cards.size(); index++) {
            FlashcardRecord card = cards.get(index);
            String front = normalizeString(card.front());
            String back = normalizeString(card.back());
            String explanation = normalizeString(card.explanation());

            if (front.isBlank() || back.isBlank()) {
                throw new AppException(
                    ErrorReason.C002,
                    "Card " + (index + 1) + ": Front and Back are required.");
            }

            Map<String, Object> normalized = new HashMap<>();
            normalized.put("front", front);
            normalized.put("back", back);
            normalized.put("explanation", explanation);
            normalizedCards.add(normalized);
        }

        if (normalizedCards.isEmpty()) {
            throw new AppException(ErrorReason.C002, "Add at least one valid flashcard.");
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "flashcards");
        payload.put("title", textValueNormalizer.firstNonBlankTrimmed(normalizeString(input.title()), "Lesson flashcards"));
        payload.put("cards", normalizedCards);
        return payload;
    }

    /**
     * Normalizes a generated quiz payload into the stored activity payload shape.
     *
     * @param rawPayload raw model JSON payload
     * @param expectedCount maximum number of quiz items to retain
     * @return normalized quiz payload
     */
    Map<String, Object> normalizeQuizPayload(Map<String, Object> rawPayload, int expectedCount) {
        List<Map<String, Object>> items = jsonMapReader.mapList(rawPayload.get("items"));
        List<Map<String, Object>> normalizedItems = new ArrayList<>();

        for (Map<String, Object> item : items) {
            Map<String, Object> normalized = quizQuestionValidator.normalize(
                normalizeString(item.get("type")),
                normalizeString(item.get("question")),
                jsonMapReader.stringList(item.get("options")),
                normalizeString(item.get("correctAnswer")),
                jsonMapReader.stringList(item.get("correctAnswers")),
                normalizeString(item.get("explanation")));
            if (normalized != null) {
                normalizedItems.add(normalized);
            }
        }

        if (normalizedItems.isEmpty()) {
            throw new AppException(ErrorReason.C003, "OpenAI returned an invalid quiz.");
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "quiz");
        payload.put("title", textValueNormalizer.firstNonBlankTrimmed(normalizeString(rawPayload.get("title")), "Lesson quiz"));
        payload.put("items", normalizedItems.subList(0, Math.min(normalizedItems.size(), expectedCount)));
        return payload;
    }

    /**
     * Normalizes a generated flashcards payload into the stored activity payload shape.
     *
     * @param rawPayload raw model JSON payload
     * @param expectedCount maximum number of cards to retain
     * @return normalized flashcards payload
     */
    Map<String, Object> normalizeFlashcardsPayload(Map<String, Object> rawPayload, int expectedCount) {
        List<Map<String, Object>> cards = jsonMapReader.mapList(rawPayload.get("cards"));
        List<Map<String, Object>> normalizedCards = new ArrayList<>();

        for (Map<String, Object> card : cards) {
            String front = normalizeString(card.get("front"));
            String back = normalizeString(card.get("back"));
            String explanation = normalizeString(card.get("explanation"));

            if (front.isBlank() || back.isBlank()) {
                continue;
            }

            Map<String, Object> normalized = new HashMap<>();
            normalized.put("front", front);
            normalized.put("back", back);
            normalized.put("explanation", explanation);
            normalizedCards.add(normalized);
        }

        if (normalizedCards.isEmpty()) {
            throw new AppException(ErrorReason.C003, "OpenAI returned invalid flashcards.");
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "flashcards");
        payload.put("title", textValueNormalizer.firstNonBlankTrimmed(normalizeString(rawPayload.get("title")), "Lesson flashcards"));
        payload.put("cards", normalizedCards.subList(0, Math.min(normalizedCards.size(), expectedCount)));
        return payload;
    }

    /**
     * Returns the exact JSON shape requested from the model for an activity type.
     *
     * @param type normalized activity type
     * @return prompt JSON schema text
     */
    String getActivityShape(String type) {
        if ("flashcards".equals(type)) {
            return String.join("\n",
                "{",
                "  \"type\": \"flashcards\",",
                "  \"title\": \"short activity title\",",
                "  \"cards\": [",
                "    {",
                "      \"front\": \"complete study question or recall prompt, not just a term\",",
                "      \"back\": \"clear answer and any useful explanation\"",
                "    }",
                "  ]",
                "}"
            );
        }

        return String.join("\n",
            "{",
            "  \"type\": \"quiz\",",
            "  \"title\": \"short activity title\",",
            "  \"items\": [",
            "    {",
            "      \"type\": \"single_choice\",",
            "      \"question\": \"question text\",",
            "      \"options\": [\"answer option A\", \"answer option B\", \"answer option C\", \"answer option D\"],",
            "      \"correctAnswer\": \"exact text of the correct option\",",
            "      \"explanation\": \"explain why the answer is correct; when useful, explain why other options are wrong\"",
            "    },",
            "    {",
            "      \"type\": \"multiple_choice\",",
            "      \"question\": \"question text with more than one correct option\",",
            "      \"options\": [\"answer option A\", \"answer option B\", \"answer option C\", \"answer option D\"],",
            "      \"correctAnswers\": [\"exact text of a correct option\", \"exact text of another correct option\"],",
            "      \"explanation\": \"explain why each listed answer is correct; when useful, explain why other options are wrong\"",
            "    },",
            "    {",
            "      \"type\": \"true_false\",",
            "      \"question\": \"statement to evaluate as true or false\",",
            "      \"options\": [\"True\", \"False\"],",
            "      \"correctAnswer\": \"True\",",
            "      \"explanation\": \"explain why the statement is true or false\"",
            "    },",
            "    {",
            "      \"type\": \"fill_in_blanks_with_options\",",
            "      \"question\": \"sentence with a single blank marker, for example: The capital of France is _____.\",",
            "      \"options\": [\"Paris\", \"Berlin\", \"Madrid\"],",
            "      \"correctAnswer\": \"Paris\",",
            "      \"explanation\": \"explain why this option fills the blank correctly\"",
            "    }",
            "  ]",
            "}"
        );
    }

    /**
     * Clamps a requested activity item count to the configured type-specific limits.
     *
     * @param type normalized activity type
     * @param count raw requested count
     * @return count inside configured limits
     */
    int clampCount(String type, Object count) {
        ActivityCountLimits limits = ActivityPromptConstants.LESSON_ACTIVITY_LIMITS.getOrDefault(
            type,
            ActivityPromptConstants.LESSON_ACTIVITY_LIMITS.get("quiz")
        );
        int parsedCount;
        try {
            parsedCount = count == null ? limits.defaultCount() : Integer.parseInt(String.valueOf(count));
        } catch (NumberFormatException ex) {
            return limits.defaultCount();
        }
        return Math.min(limits.max(), Math.max(limits.min(), parsedCount));
    }

    /**
     * Removes HTML markup from lesson content before sending it to the activity prompt.
     *
     * @param value HTML or text value
     * @return plain text value
     */
    String stripHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
            .replaceAll("(?is)<style.*?</style>", " ")
            .replaceAll("(?is)<script.*?</script>", " ")
            .replaceAll("<[^>]+>", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }

    /**
     * Trims string values while rejecting non-string inputs.
     *
     * @param value raw value
     * @return trimmed string or empty string
     */
    String normalizeString(Object value) {
        return value instanceof String stringValue ? stringValue.trim() : "";
    }

}
