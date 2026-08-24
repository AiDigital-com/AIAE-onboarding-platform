package com.aidigital.aionboarding.external.openai.impl;

import com.aidigital.aionboarding.external.common.http.PooledRestClientFactory;
import com.aidigital.aionboarding.external.openai.OpenAiClient;
import com.aidigital.aionboarding.external.openai.OpenAiExternalException;
import com.aidigital.aionboarding.external.openai.config.OpenAiProperties;
import com.aidigital.aionboarding.external.openai.model.OpenAiContentPart;
import com.aidigital.aionboarding.external.openai.model.OpenAiFileInput;
import com.aidigital.aionboarding.external.openai.model.OpenAiFileUploadResponse;
import com.aidigital.aionboarding.external.openai.model.OpenAiOutputItem;
import com.aidigital.aionboarding.external.openai.model.OpenAiResponsesRequest;
import com.aidigital.aionboarding.external.openai.model.OpenAiResponsesResponse;
import com.aidigital.aionboarding.external.openai.model.OpenAiResponsesResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Production {@link OpenAiClient} that sends requests exclusively via the Responses API.
 * The Chat Completions path has been removed — exactly one transport adapter exists (D-08).
 */
public class OpenAiClientImpl implements OpenAiClient {

    private static final Logger LOG = LoggerFactory.getLogger(OpenAiClientImpl.class);

    /** Caps how many bytes of a non-2xx response body are read into the reported error, so a
     * misbehaving or oversized provider error response cannot force an unbounded allocation. */
    private static final int MAX_ERROR_BODY_BYTES = 8192;

    /** Input-item discriminator for a chat message; the only item type this adapter emits. */
    private static final String ITEM_TYPE_MESSAGE = "message";

    /** Role carried by the single input item this adapter builds. */
    private static final String ROLE_USER = "user";

    /** Content-part discriminator for the user's prompt text. */
    private static final String CONTENT_TYPE_INPUT_TEXT = "input_text";

    private static final String FIELD_TYPE = "type";
    private static final String FIELD_ROLE = "role";
    private static final String FIELD_CONTENT = "content";
    private static final String FIELD_TEXT = "text";

    private final OpenAiProperties properties;
    private final RestClient restClient;

    /**
     * Constructs a new client using the supplied configuration and HTTP client factory.
     *
     * @param properties runtime-tunable OpenAI properties
     * @param factory    pooled HTTP client factory
     */
    public OpenAiClientImpl(OpenAiProperties properties, PooledRestClientFactory factory) {
        this.properties = properties;
        this.restClient = factory.createClient("openai", properties.getBaseUrl());
    }

    @Override
    public OpenAiResponsesResult complete(String systemPrompt, String userMessage) {
        return completeViaResponses(systemPrompt, userMessage, properties.getModel(), null, List.of(), null, null, null);
    }

    @Override
    public OpenAiResponsesResult createResponse(String instructions, String userMessage, String model) {
        if (model == null || model.isBlank()) {
            throw new OpenAiExternalException("OpenAI model is required", -1, "");
        }
        return completeViaResponses(instructions, userMessage, model, null, List.of(), null, null, null);
    }

    @Override
    public OpenAiResponsesResult createResponse(String instructions, String userMessage, String model,
                                                 List<OpenAiFileInput> fileInputs) {
        if (model == null || model.isBlank()) {
            throw new OpenAiExternalException("OpenAI model is required", -1, "");
        }
        return completeViaResponses(instructions, userMessage, model, null, fileInputs, null, null, null);
    }

    @Override
    public OpenAiResponsesResult createResponse(String instructions, String userMessage, String model,
                                                String promptCacheKey, List<OpenAiFileInput> fileInputs) {
        if (model == null || model.isBlank()) {
            throw new OpenAiExternalException("OpenAI model is required", -1, "");
        }
        return completeViaResponses(instructions, userMessage, model, promptCacheKey, fileInputs, null, null, null);
    }

    @Override
    public OpenAiResponsesResult createResponse(String instructions, String userMessage, String model,
                                                String promptCacheKey, List<OpenAiFileInput> fileInputs,
                                                Boolean store, String previousResponseId) {
        if (model == null || model.isBlank()) {
            throw new OpenAiExternalException("OpenAI model is required", -1, "");
        }
        return completeViaResponses(
            instructions, userMessage, model, promptCacheKey, fileInputs, null, store, previousResponseId);
    }

    @Override
    public OpenAiFileUploadResponse uploadFile(byte[] content, String filename, String purpose) {
        if (content == null || content.length == 0) {
            throw new OpenAiExternalException("OpenAI file content is required", -1, "");
        }
        if (filename == null || filename.isBlank()) {
            throw new OpenAiExternalException("OpenAI filename is required", -1, "");
        }
        if (purpose == null || purpose.isBlank()) {
            throw new OpenAiExternalException("OpenAI file purpose is required", -1, "");
        }

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("purpose", purpose);
        body.add("file", new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        });

        LOG.debug("Uploading file to OpenAI Files API: filename={}", filename);

        try {
            OpenAiFileUploadResponse response = restClient.post()
                .uri("/v1/files")
                .header("Authorization", "Bearer " + properties.getApiKey())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    String responseBody = new String(res.getBody().readNBytes(MAX_ERROR_BODY_BYTES));
                    // The body is the only place the provider says WHAT was wrong; it is dropped
                    // by every caller above, so log it here or lose it.
                    LOG.warn("OpenAI Files API returned HTTP {}: {}", res.getStatusCode().value(), responseBody);
                    throw new OpenAiExternalException(
                        "OpenAI Files API returned HTTP " + res.getStatusCode().value(),
                        res.getStatusCode().value(),
                        responseBody);
                })
                .body(OpenAiFileUploadResponse.class);

            if (response == null || response.id() == null || response.id().isBlank()) {
                throw new OpenAiExternalException("OpenAI Files API returned empty file id", -1, "");
            }
            return response;
        } catch (OpenAiExternalException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new OpenAiExternalException("OpenAI Files API call failed: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new OpenAiExternalException("Unexpected error calling OpenAI Files API", ex);
        }
    }

    /**
     * Calls the OpenAI Responses API, builds the request with {@code instructions} as the top-level
     * field (D-01), and returns a typed result with response ID, token usage, and extracted text.
     *
     * @param instructions    top-level instructions field (D-01 — never placed in the input array)
     * @param userMessage     user-facing prompt text
     * @param model           OpenAI model identifier
     * @param promptCacheKey optional prompt cache key for server-side caching
     * @param fileInputs      typed file inputs; empty list for text-only requests (D-09)
     * @param maxOutputTokens optional output token cap; {@code null} applies no cap (D-02)
     * @param store           optional flag to retain this response for later {@code previousResponseId}
     *     chaining; {@code null} omits the field and defers to the API default
     * @param previousResponseId optional prior response id to continue a conversation from
     * @return typed result containing response ID, token usage, and output text
     */
    OpenAiResponsesResult completeViaResponses(String instructions, String userMessage, String model,
                                               String promptCacheKey, List<OpenAiFileInput> fileInputs,
                                               Integer maxOutputTokens, Boolean store, String previousResponseId) {
        Object input;
        if (fileInputs == null || fileInputs.isEmpty()) {
            input = userMessage;
        } else {
            input = List.of(buildUserMessageItem(userMessage, fileInputs));
        }
        String cacheKey = promptCacheKey == null || promptCacheKey.isBlank() ? null : promptCacheKey;
        String resolvedPreviousResponseId =
            previousResponseId == null || previousResponseId.isBlank() ? null : previousResponseId;

        OpenAiResponsesRequest request = new OpenAiResponsesRequest(
            model,
            instructions,
            input,
            cacheKey,
            store,
            resolvedPreviousResponseId,
            maxOutputTokens);

        LOG.debug("Sending OpenAI Responses request: model={}", model);

        try {
            OpenAiResponsesResponse response = restClient.post()
                .uri(properties.getResponsesPath())
                .header("Authorization", "Bearer " + properties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    String body = new String(res.getBody().readNBytes(MAX_ERROR_BODY_BYTES));
                    // The body is the only place the provider says WHAT was wrong; it is dropped
                    // by every caller above, so log it here or lose it.
                    LOG.warn("OpenAI Responses API returned HTTP {}: {}", res.getStatusCode().value(), body);
                    throw new OpenAiExternalException(
                        "OpenAI API returned HTTP " + res.getStatusCode().value(),
                        res.getStatusCode().value(),
                        body);
                })
                .body(OpenAiResponsesResponse.class);

            if (response == null) {
                throw new OpenAiExternalException("OpenAI Responses API returned a null response body", -1, "");
            }
            return new OpenAiResponsesResult(response.id(), response.usage(), extractResponsesText(response));
        } catch (OpenAiExternalException ex) {
            throw ex;
        } catch (ResourceAccessException ex) {
            throw new OpenAiExternalException("OpenAI API call timed out: " + ex.getMessage(), ex, true);
        } catch (RestClientException ex) {
            throw new OpenAiExternalException("OpenAI API call failed: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new OpenAiExternalException("Unexpected error calling OpenAI API", ex);
        }
    }

    /**
     * Builds the single {@code message} input item carrying the prompt text and every file
     * attachment.
     *
     * <p>The Responses API accepts either a plain string or an array of <em>input items</em> in
     * {@code input}. Content parts such as {@code input_text} / {@code input_file} are not input
     * items themselves — placing one at the top level is rejected with
     * {@code 400 invalid_value} on {@code input[0]}. They must be nested inside a message.
     *
     * @param userMessage user-facing prompt text
     * @param fileInputs  non-empty typed file inputs to attach alongside the text
     * @return a {@code message} input item with role {@code user}
     */
    Map<String, Object> buildUserMessageItem(String userMessage, List<OpenAiFileInput> fileInputs) {
        List<Object> content = new ArrayList<>();
        content.add(Map.of(FIELD_TYPE, CONTENT_TYPE_INPUT_TEXT, FIELD_TEXT, userMessage));
        content.addAll(fileInputs);

        Map<String, Object> message = new LinkedHashMap<>();
        message.put(FIELD_TYPE, ITEM_TYPE_MESSAGE);
        message.put(FIELD_ROLE, ROLE_USER);
        message.put(FIELD_CONTENT, content);
        return message;
    }

    /**
     * Extracts the first non-blank text part from a Responses API payload.
     *
     * @param response decoded OpenAI Responses API response
     * @return generated text
     */
    String extractResponsesText(OpenAiResponsesResponse response) {
        if (response == null || response.output() == null || response.output().isEmpty()) {
            throw new OpenAiExternalException("OpenAI Responses API returned empty output", -1, "");
        }
        for (OpenAiOutputItem item : response.output()) {
            if (item.content() == null) {
                continue;
            }
            for (OpenAiContentPart part : item.content()) {
                if (part.text() != null && !part.text().isBlank()) {
                    return part.text();
                }
            }
        }
        throw new OpenAiExternalException("OpenAI Responses API returned no text content", -1, "");
    }
}
