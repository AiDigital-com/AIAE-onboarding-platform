package com.aidigital.aionboarding.service.lessongen.support;

import com.aidigital.aionboarding.external.openai.OpenAiClient;
import com.aidigital.aionboarding.external.openai.OpenAiExternalException;
import com.aidigital.aionboarding.external.openai.model.OpenAiFileInput;
import com.aidigital.aionboarding.external.openai.model.OpenAiFileUploadResponse;
import com.aidigital.aionboarding.external.openai.model.OpenAiResponsesResult;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.lessongen.model.LessonGenPrompt;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Single boundary between lesson generation and the OpenAI adapter: resolves the client, turns a
 * {@link LessonGenPrompt} into a Responses API call, and translates provider failures into
 * {@link AppException}.
 *
 * <p>Kept out of {@code LessonGenServiceImpl} so every generation entry point (lesson, revision
 * brief, activity, transcript condensing, assistant answer) shares one place where provider
 * internals are logged and one place where the author-facing message is chosen.
 */
@Component
@RequiredArgsConstructor
public class OpenAiPromptExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(OpenAiPromptExecutor.class);

    /** Key holding the Responses API content-part type in a raw file-input map. */
    private static final String FILE_INPUT_TYPE_KEY = "type";

    /** Key holding the OpenAI file id in a raw file-input map. */
    private static final String FILE_INPUT_ID_KEY = "file_id";

    /** Content-part type assumed when a file-input map carries none. */
    private static final String DEFAULT_FILE_INPUT_TYPE = "input_file";

    /** Author-facing text for a rejected or unusable provider response; details go to the log. */
    private static final String UNAVAILABLE_USER_MESSAGE =
        "the AI service is temporarily unavailable. Please try again.";

    /** Author-facing text for a provider timeout; details go to the log. */
    private static final String TIMEOUT_USER_MESSAGE =
        "the AI service did not respond in time. Please try again.";

    private final ObjectProvider<OpenAiClient> openAiClientProvider;

    /**
     * Calls the OpenAI Responses API with the given prompt and model and trims the output text.
     * Translates {@link OpenAiExternalException} into an {@link AppException}: {@code C008} for a
     * provider timeout, {@code C003} otherwise. Provider internals are logged, never put in the
     * thrown message.
     *
     * @param prompt the generation prompt containing instructions, input, and cache metadata
     * @param model  the OpenAI model identifier to use
     * @return the typed API result with response ID, usage, and trimmed text
     * @throws AppException if the API call fails
     */
    public OpenAiResponsesResult execute(LessonGenPrompt prompt, String model) {
        try {
            OpenAiResponsesResult raw = requireClient().createResponse(
                prompt.instructions(), prompt.input(), model, prompt.cacheKey(), fileInputsOf(prompt),
                prompt.store() ? Boolean.TRUE : null, prompt.previousResponseId());
            return new OpenAiResponsesResult(raw.responseId(), raw.usage(), raw.text().trim());
        } catch (OpenAiExternalException ex) {
            // Provider internals ("OpenAI API returned HTTP 400", response bodies, model ids) stay
            // in the logs; the API surfaces a message an author can act on. The HTTP status still
            // distinguishes the two cases: C008 maps to 504, C003 to 502.
            LOG.warn("OpenAI call failed (model={}, timeout={}): {}", model, ex.isTimeout(), ex.getMessage(), ex);
            ErrorReason reason = ex.isTimeout() ? ErrorReason.C008 : ErrorReason.C003;
            String userMessage = ex.isTimeout() ? TIMEOUT_USER_MESSAGE : UNAVAILABLE_USER_MESSAGE;
            throw new AppException(reason, userMessage, ex);
        }
    }

    /**
     * Uploads one file through the OpenAI Files API.
     *
     * @param content  raw file bytes
     * @param filename original file name, used by the provider to infer the format
     * @param purpose  OpenAI file purpose
     * @return the provider's upload response
     * @throws AppException if the upload fails
     */
    public OpenAiFileUploadResponse uploadFile(byte[] content, String filename, String purpose) {
        try {
            return requireClient().uploadFile(content, filename, purpose);
        } catch (OpenAiExternalException ex) {
            LOG.warn("OpenAI file upload failed (filename={}): {}", filename, ex.getMessage(), ex);
            throw new AppException(ErrorReason.C003, UNAVAILABLE_USER_MESSAGE, ex);
        }
    }

    /**
     * Converts the prompt's raw file-input maps into typed inputs, dropping entries without a
     * usable file id.
     *
     * @param prompt the generation prompt
     * @return typed file inputs; empty when the prompt carries none
     */
    List<OpenAiFileInput> fileInputsOf(LessonGenPrompt prompt) {
        if (prompt.fileInputs() == null) {
            return List.of();
        }
        return prompt.fileInputs().stream()
            .map(m -> new OpenAiFileInput(inputTypeOf(m), (String) m.get(FILE_INPUT_ID_KEY)))
            .filter(f -> f.fileId() != null && !f.fileId().isBlank())
            .toList();
    }

    /**
     * Reads the caller-supplied Responses API content-part type from one raw file-input map,
     * defaulting to {@code input_file}. Callers such as
     * {@code MaterialOpenAiFilePreparationServiceImpl} and {@code LessonAssistantPromptBuilder}
     * already classify images as {@code input_image}; overwriting that here would make every
     * image attachment fail with HTTP 400.
     *
     * @param fileInput one raw file-input map from the prompt
     * @return the supplied type, or {@code input_file} when absent or blank
     */
    String inputTypeOf(Map<String, Object> fileInput) {
        Object type = fileInput.get(FILE_INPUT_TYPE_KEY);
        if (type instanceof String stringType && !stringType.isBlank()) {
            return stringType;
        }
        return DEFAULT_FILE_INPUT_TYPE;
    }

    /**
     * Returns the available {@link OpenAiClient}. Throws {@link AppException} with reason
     * {@code C003} when the API key is not configured and no client bean is registered; the
     * missing configuration is named in the log, not in the thrown message.
     *
     * @return the resolved OpenAI client
     * @throws AppException if no OpenAI client is available
     */
    OpenAiClient requireClient() {
        OpenAiClient client = openAiClientProvider.getIfAvailable();
        if (client == null) {
            // A deployment misconfiguration an author cannot act on: name the missing variable in
            // the log, not in the response.
            LOG.error("No OpenAI client bean is registered — set OPENAI_ENABLED=true and OPENAI_API_KEY");
            throw new AppException(ErrorReason.C003, UNAVAILABLE_USER_MESSAGE);
        }
        return client;
    }
}
