package com.aidigital.aionboarding.service.lessongen.services.impl;

import com.aidigital.aionboarding.external.openai.OpenAiClient;
import com.aidigital.aionboarding.external.openai.OpenAiExternalException;
import com.aidigital.aionboarding.external.openai.config.OpenAiProperties;
import com.aidigital.aionboarding.external.openai.model.OpenAiFileInput;
import com.aidigital.aionboarding.external.openai.model.OpenAiFileUploadResponse;
import com.aidigital.aionboarding.external.openai.model.OpenAiResponsesResult;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.lessongen.config.LessonGenProperties;
import com.aidigital.aionboarding.service.lessongen.model.GeneratedActivityResult;
import com.aidigital.aionboarding.service.lessongen.model.GeneratedContentResult;
import com.aidigital.aionboarding.service.lessongen.model.GeneratedRevisionBriefResult;
import com.aidigital.aionboarding.service.lessongen.model.LessonGenPrompt;
import com.aidigital.aionboarding.service.lessongen.services.LessonGenService;
import com.aidigital.aionboarding.service.lessongen.support.GenerationMetadataAssembler;
import com.aidigital.aionboarding.service.lessongen.util.LessonGenJsonSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LessonGenServiceImpl implements LessonGenService {

    private static final Set<String> ALLOWED_REVISION_SCOPES = Set.of("targeted", "substantial", "near-complete");

    private final ObjectProvider<OpenAiClient> openAiClientProvider;
    private final OpenAiProperties openAiProperties;
    private final LessonGenProperties lessonGenProperties;
    private final LessonGenJsonSupport lessonGenJsonSupport;
    private final GenerationMetadataAssembler generationMetadataAssembler;

    @Override
    public GeneratedContentResult condenseSourceText(LessonGenPrompt prompt) {
        String model = firstNonBlank(
            lessonGenProperties.getTranscriptCompressionModel(),
            lessonGenProperties.getMiniModel(),
            "gpt-4o-mini"
        );
        OpenAiResponsesResult result = createResponseText(prompt, model);
        String text = result.text();
        if (text.isBlank()) {
            throw new AppException(ErrorReason.C003, "OpenAI returned an empty condensed transcript.");
        }
        return new GeneratedContentResult(text,
            generationMetadataAssembler.baseMetadata(prompt, model, text, result.responseId(), result.usage()));
    }

    @Override
    public GeneratedContentResult generateLessonContent(LessonGenPrompt prompt) {
        String model = firstNonBlank(
            lessonGenProperties.getModel(),
            lessonGenProperties.getMiniModel(),
            "gpt-4o-mini"
        );
        OpenAiResponsesResult result = createResponseText(prompt, model);
        String content = result.text();
        if (content.isBlank()) {
            throw new AppException(ErrorReason.C003, "OpenAI returned an empty lesson.");
        }
        return new GeneratedContentResult(content,
            generationMetadataAssembler.baseMetadata(prompt, model, content, result.responseId(), result.usage()));
    }

    @Override
    public GeneratedRevisionBriefResult generateLessonRevisionBrief(LessonGenPrompt prompt) {
        String model = firstNonBlank(
            lessonGenProperties.getRevisionPlannerModel(),
            lessonGenProperties.getMiniModel(),
            "gpt-4o-mini"
        );
        OpenAiResponsesResult result = createResponseText(prompt, model);
        String raw = result.text();
        Map<String, Object> parsed = lessonGenJsonSupport.extractJsonPayload(raw);
        if (parsed == null) {
            throw new AppException(ErrorReason.C003, "OpenAI returned an invalid revision brief.");
        }

        String changeScope = ALLOWED_REVISION_SCOPES.contains(stringVal(parsed.get("changeScope")))
            ? stringVal(parsed.get("changeScope"))
            : "substantial";
        String userIntent = stringVal(parsed.get("userIntent"));
        if (userIntent.isBlank()) {
            userIntent = "Revise the current lesson based on user feedback.";
        }

        Map<String, Object> brief = new HashMap<>();
        brief.put("changeScope", changeScope);
        brief.put("userIntent", userIntent);
        brief.put("editInstructions", normalizeStringList(parsed.get("editInstructions")));
        brief.put("preserveRules", normalizeStringList(parsed.get("preserveRules")));
        brief.put("riskNotes", normalizeStringList(parsed.get("riskNotes")));

        Map<String, Object> metadata = generationMetadataAssembler.baseMetadata(prompt, model, raw, result.responseId(), result.usage());
        metadata.put("rawOutput", raw);
        return new GeneratedRevisionBriefResult(brief, metadata);
    }

    @Override
    public GeneratedActivityResult generateLessonActivityPayload(LessonGenPrompt prompt) {
        String model = firstNonBlank(
            lessonGenProperties.getActivityModel(),
            lessonGenProperties.getMiniModel(),
            openAiProperties.getModel(),
            "gpt-4o-mini"
        );
        OpenAiResponsesResult result = createResponseText(prompt, model);
        String raw = result.text();
        Map<String, Object> payload = lessonGenJsonSupport.extractJsonPayload(raw);
        if (payload == null) {
            throw new AppException(ErrorReason.C003, "OpenAI returned an invalid activity JSON.");
        }

        Map<String, Object> metadata = generationMetadataAssembler.baseMetadata(prompt, model, raw, result.responseId(), result.usage());
        metadata.put("rawOutput", raw);
        return new GeneratedActivityResult(payload, metadata);
    }

    @Override
    public Map<String, Object> extractJsonPayload(String value) {
        return lessonGenJsonSupport.extractJsonPayload(value);
    }

    @Override
    public OpenAiFileUploadResponse uploadFile(byte[] content, String filename, String purpose) {
        try {
            return requireClient().uploadFile(content, filename, purpose);
        } catch (OpenAiExternalException ex) {
            throw new AppException(ErrorReason.C003, ex.getMessage(), ex);
        }
    }

    /**
     * Calls the OpenAI Responses API with the given prompt and model, trims the output text,
     * and wraps the result into an {@link OpenAiResponsesResult}. Translates
     * {@link OpenAiExternalException} into an {@link AppException} with reason {@code C003}.
     *
     * @param prompt the generation prompt containing instructions, input, and cache metadata
     * @param model  the OpenAI model identifier to use
     * @return the typed API result with response ID, usage, and trimmed text
     * @throws AppException if the API call fails
     */
    OpenAiResponsesResult createResponseText(LessonGenPrompt prompt, String model) {
        List<OpenAiFileInput> fileInputs = prompt.fileInputs() == null ? List.of()
            : prompt.fileInputs().stream()
                .map(m -> new OpenAiFileInput("input_file", (String) m.get("file_id")))
                .filter(f -> f.fileId() != null && !f.fileId().isBlank())
                .toList();
        try {
            OpenAiResponsesResult raw = requireClient().createResponse(
                prompt.instructions(), prompt.input(), model, prompt.cacheKey(), fileInputs,
                prompt.store() ? Boolean.TRUE : null, prompt.previousResponseId());
            return new OpenAiResponsesResult(raw.responseId(), raw.usage(), raw.text().trim());
        } catch (OpenAiExternalException ex) {
            throw new AppException(ErrorReason.C003, firstNonBlank(ex.getMessage(), "OpenAI lesson generation failed."), ex);
        }
    }

    /**
     * Returns the available {@link OpenAiClient}. Throws {@link AppException} with reason
     * {@code C003} when the API key is not configured and no client bean is registered.
     *
     * @return the resolved OpenAI client
     * @throws AppException if no OpenAI client is available
     */
    OpenAiClient requireClient() {
        OpenAiClient client = openAiClientProvider.getIfAvailable();
        if (client == null) {
            throw new AppException(ErrorReason.C003, "OPENAI_API_KEY is not configured.");
        }
        return client;
    }

    /**
     * Converts a raw JSON array value from the parsed model output into a {@link List} of
     * non-blank trimmed strings. Returns an empty list if {@code value} is not a {@link List}.
     *
     * @param value the raw value from the parsed map, typically a {@code List<?>}
     * @return a list of trimmed, non-blank strings
     */
    List<String> normalizeStringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<String> normalized = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof String stringValue) {
                String trimmed = stringValue.trim();
                if (!trimmed.isBlank()) {
                    normalized.add(trimmed);
                }
            }
        }
        return normalized;
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
     * Returns the first non-blank string from the supplied candidates, or an empty string
     * if all candidates are {@code null} or blank.
     *
     * @param values candidate strings in priority order
     * @return the first non-blank trimmed value, or {@code ""}
     */
    String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
