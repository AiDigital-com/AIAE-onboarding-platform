package com.aidigital.aionboarding.external.openai;

import com.aidigital.aionboarding.external.openai.model.OpenAiFileInput;
import com.aidigital.aionboarding.external.openai.model.OpenAiFileUploadResponse;
import com.aidigital.aionboarding.external.openai.model.OpenAiResponsesResult;

import java.util.List;

/**
 * Narrow application-facing interface for the OpenAI Responses and Files APIs.
 *
 * <p>Callers submit prompts or file bytes; the implementation handles
 * authentication, request serialization, and response extraction.
 * Vendor types are never exposed through this interface.
 *
 * <p>Non-2xx responses, timeouts, and malformed responses are mapped to
 * {@link OpenAiExternalException}.
 */
public interface OpenAiClient {

	/**
	 * Sends a single-turn completion request using the configured default model.
	 * The {@code systemPrompt} maps to the top-level {@code instructions} field per D-01;
	 * no token cap is applied by the transport (D-02).
	 *
	 * @param systemPrompt instruction context for the model (may be blank)
	 * @param userMessage  user-facing prompt text
	 * @return typed transport result containing response ID, token usage, and output text
	 * @throws OpenAiExternalException on any HTTP, timeout, or parse failure
	 */
	OpenAiResponsesResult complete(String systemPrompt, String userMessage);

	/**
	 * Sends a single-turn Responses API request with an explicit model override.
	 * The {@code instructions} parameter maps to the top-level {@code instructions} field per D-01;
	 * no token cap is applied by the transport (D-02).
	 *
	 * @param instructions instruction context for the model (may be blank)
	 * @param userMessage  user-facing prompt text
	 * @param model        OpenAI model identifier
	 * @return typed transport result containing response ID, token usage, and output text
	 * @throws OpenAiExternalException on any HTTP, timeout, or parse failure
	 */
	OpenAiResponsesResult createResponse(String instructions, String userMessage, String model);

	/**
	 * Sends a single-turn Responses API request with an explicit model override and typed file inputs.
	 * The {@code instructions} parameter maps to the top-level {@code instructions} field per D-01;
	 * no token cap is applied by the transport (D-02). File inputs are carried as typed
	 * {@link OpenAiFileInput} items in the structured input array (D-09).
	 *
	 * @param instructions instruction context for the model (may be blank)
	 * @param userMessage  user-facing prompt text
	 * @param model        OpenAI model identifier
	 * @param fileInputs   typed file inputs; may be empty
	 * @return typed transport result containing response ID, token usage, and output text
	 * @throws OpenAiExternalException on any HTTP, timeout, or parse failure
	 */
	OpenAiResponsesResult createResponse(String instructions, String userMessage, String model,
	                                     List<OpenAiFileInput> fileInputs);

	/**
	 * Sends a single-turn Responses API request with an explicit model override, prompt cache
	 * key, and typed file inputs. The {@code promptCacheKey} maps to the top-level
	 * {@code prompt_cache_key} request field and may be blank when no cache key should be sent.
	 *
	 * @param instructions   instruction context for the model (may be blank)
	 * @param userMessage    user-facing prompt text
	 * @param model          OpenAI model identifier
	 * @param promptCacheKey optional prompt cache key
	 * @param fileInputs     typed file inputs; may be empty
	 * @return typed transport result containing response ID, token usage, and output text
	 * @throws OpenAiExternalException on any HTTP, timeout, or parse failure
	 */
	OpenAiResponsesResult createResponse(String instructions, String userMessage, String model,
	                                     String promptCacheKey, List<OpenAiFileInput> fileInputs);

	/**
	 * Sends a Responses API request that can chain onto and/or be chained from another response,
	 * enabling real multi-turn conversations without resending prior turns' full context.
	 * The {@code store} flag maps to the top-level {@code store} request field — pass {@code true}
	 * so this response can be referenced by a later {@code previousResponseId}. The
	 * {@code previousResponseId} maps to the top-level {@code previous_response_id} field; pass
	 * {@code null} to start a new conversation.
	 *
	 * @param instructions       instruction context for the model (may be blank)
	 * @param userMessage        user-facing prompt text
	 * @param model              OpenAI model identifier
	 * @param promptCacheKey     optional prompt cache key
	 * @param fileInputs         typed file inputs; may be empty
	 * @param store              whether OpenAI should retain this response for later chaining
	 * @param previousResponseId prior response id to continue from, or {@code null}
	 * @return typed transport result containing response ID, token usage, and output text
	 * @throws OpenAiExternalException on any HTTP, timeout, or parse failure
	 */
	OpenAiResponsesResult createResponse(String instructions, String userMessage, String model,
	                                     String promptCacheKey, List<OpenAiFileInput> fileInputs,
	                                     Boolean store, String previousResponseId);

	/**
	 * Uploads a file to the OpenAI Files API.
	 *
	 * @param content  file bytes
	 * @param filename original filename
	 * @param purpose  OpenAI file purpose (for example {@code user_data} or {@code vision})
	 * @return uploaded file metadata
	 * @throws OpenAiExternalException on any HTTP, timeout, or parse failure
	 */
	OpenAiFileUploadResponse uploadFile(byte[] content, String filename, String purpose);
}
