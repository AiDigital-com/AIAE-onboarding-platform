package com.aidigital.aionboarding.external.openai.fake;

import com.aidigital.aionboarding.external.openai.OpenAiClient;
import com.aidigital.aionboarding.external.openai.model.OpenAiFileInput;
import com.aidigital.aionboarding.external.openai.model.OpenAiFileUploadResponse;
import com.aidigital.aionboarding.external.openai.model.OpenAiResponsesResult;
import com.aidigital.aionboarding.external.openai.model.OpenAiResponsesUsage;

import java.util.List;

/**
 * Test double that records the arguments passed to each {@link OpenAiClient} invocation.
 * Configure a {@link OpenAiResponsesResult} via the constructor or {@link #setStubbedResult} so
 * both {@code createResponse} overloads return predictable values. Downstream acceptance tests
 * (Phase 2+) use the captured-field getters to assert the exact wire contract: instructions at
 * the top level, no system role, prompt_cache_key handling, no max_output_tokens, and typed file
 * inputs.
 */
public class RecordingOpenAiClient implements OpenAiClient {

	private static final OpenAiResponsesResult DEFAULT_RESULT =
			new OpenAiResponsesResult("resp-default", new OpenAiResponsesUsage(0, 0, 0), "");

	private OpenAiResponsesResult stubbedResult;

	private String capturedInstructions;
	private String capturedUserMessage;
	private String capturedModel;
	private String capturedPromptCacheKey;
	private List<OpenAiFileInput> capturedFileInputs;
	private Boolean capturedStore;
	private String capturedPreviousResponseId;

	/**
	 * Constructs a recorder with a default empty stubbed result.
	 */
	public RecordingOpenAiClient() {
		this.stubbedResult = DEFAULT_RESULT;
	}

	/**
	 * Constructs a recorder pre-configured with the given stubbed result.
	 *
	 * @param stubbedResult result returned by all {@code createResponse} overloads
	 */
	public RecordingOpenAiClient(OpenAiResponsesResult stubbedResult) {
		this.stubbedResult = stubbedResult;
	}

	/**
	 * Records arguments and returns the stubbed result.
	 *
	 * @param systemPrompt the system prompt passed as instructions
	 * @param userMessage  the user turn content
	 * @return the configured stubbed result
	 */
	@Override
	public OpenAiResponsesResult complete(String systemPrompt, String userMessage) {
		this.capturedInstructions = systemPrompt;
		this.capturedUserMessage = userMessage;
		this.capturedModel = null;
		this.capturedPromptCacheKey = null;
		this.capturedFileInputs = List.of();
		return stubbedResult;
	}

	/**
	 * Records arguments and returns the stubbed result.
	 *
	 * @param instructions top-level Responses instructions field (D-01)
	 * @param userMessage  the user turn content
	 * @param model        the model identifier
	 * @return the configured stubbed result
	 */
	@Override
	public OpenAiResponsesResult createResponse(String instructions, String userMessage, String model) {
		this.capturedInstructions = instructions;
		this.capturedUserMessage = userMessage;
		this.capturedModel = model;
		this.capturedPromptCacheKey = null;
		this.capturedFileInputs = List.of();
		return stubbedResult;
	}

	/**
	 * Records arguments including typed file inputs and returns the stubbed result.
	 *
	 * @param instructions top-level Responses instructions field (D-01)
	 * @param userMessage  the user turn content
	 * @param model        the model identifier
	 * @param fileInputs   typed file inputs for the structured input array (D-09)
	 * @return the configured stubbed result
	 */
	@Override
	public OpenAiResponsesResult createResponse(String instructions, String userMessage, String model,
	                                            List<OpenAiFileInput> fileInputs) {
		this.capturedInstructions = instructions;
		this.capturedUserMessage = userMessage;
		this.capturedModel = model;
		this.capturedPromptCacheKey = null;
		this.capturedFileInputs = fileInputs != null ? fileInputs : List.of();
		return stubbedResult;
	}

	/**
	 * Records arguments including prompt cache key and typed file inputs, then returns the stubbed result.
	 *
	 * @param instructions   top-level Responses instructions field (D-01)
	 * @param userMessage    the user turn content
	 * @param model          the model identifier
	 * @param promptCacheKey prompt cache key for the Responses request
	 * @param fileInputs     typed file inputs for the structured input array (D-09)
	 * @return the configured stubbed result
	 */
	@Override
	public OpenAiResponsesResult createResponse(String instructions, String userMessage, String model,
	                                            String promptCacheKey, List<OpenAiFileInput> fileInputs) {
		this.capturedInstructions = instructions;
		this.capturedUserMessage = userMessage;
		this.capturedModel = model;
		this.capturedPromptCacheKey = promptCacheKey;
		this.capturedFileInputs = fileInputs != null ? fileInputs : List.of();
		return stubbedResult;
	}

	/**
	 * Records arguments including store flag and previous response id for chaining, then returns
	 * the stubbed result.
	 *
	 * @param instructions       top-level Responses instructions field (D-01)
	 * @param userMessage        the user turn content
	 * @param model              the model identifier
	 * @param promptCacheKey     prompt cache key for the Responses request
	 * @param fileInputs         typed file inputs for the structured input array (D-09)
	 * @param store              whether the response should be retained for later chaining
	 * @param previousResponseId prior response id to continue from, or {@code null}
	 * @return the configured stubbed result
	 */
	@Override
	public OpenAiResponsesResult createResponse(String instructions, String userMessage, String model,
	                                            String promptCacheKey, List<OpenAiFileInput> fileInputs,
	                                            Boolean store, String previousResponseId) {
		this.capturedInstructions = instructions;
		this.capturedUserMessage = userMessage;
		this.capturedModel = model;
		this.capturedPromptCacheKey = promptCacheKey;
		this.capturedFileInputs = fileInputs != null ? fileInputs : List.of();
		this.capturedStore = store;
		this.capturedPreviousResponseId = previousResponseId;
		return stubbedResult;
	}

	/**
	 * Not stubbed — throws to signal callers must not invoke this on the recording double.
	 *
	 * @param fileBytes raw bytes of the file to upload
	 * @param fileName  original file name
	 * @param purpose   OpenAI file purpose (e.g. {@code "user_data"})
	 * @return never returns
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public OpenAiFileUploadResponse uploadFile(byte[] fileBytes, String fileName, String purpose) {
		throw new UnsupportedOperationException("Not stubbed");
	}

	/**
	 * Returns the instructions argument captured from the last invocation.
	 *
	 * @return captured instructions, or {@code null} if not yet invoked
	 */
	public String getCapturedInstructions() {
		return capturedInstructions;
	}

	/**
	 * Returns the user message argument captured from the last invocation.
	 *
	 * @return captured user message, or {@code null} if not yet invoked
	 */
	public String getCapturedUserMessage() {
		return capturedUserMessage;
	}

	/**
	 * Returns the model argument captured from the last invocation.
	 *
	 * @return captured model, or {@code null} if not yet invoked or if {@code complete()} was called
	 */
	public String getCapturedModel() {
		return capturedModel;
	}

	/**
	 * Returns the prompt cache key argument captured from the last invocation.
	 *
	 * @return captured prompt cache key, or {@code null} if omitted
	 */
	public String getCapturedPromptCacheKey() {
		return capturedPromptCacheKey;
	}

	/**
	 * Returns the file inputs argument captured from the last {@code createResponse} with file inputs.
	 *
	 * @return captured file inputs; empty list if not yet invoked or if the no-file-input overload was called
	 */
	public List<OpenAiFileInput> getCapturedFileInputs() {
		return capturedFileInputs;
	}

	/**
	 * Returns the store flag argument captured from the last chaining-capable invocation.
	 *
	 * @return captured store flag, or {@code null} if not yet invoked via the chaining overload
	 */
	public Boolean getCapturedStore() {
		return capturedStore;
	}

	/**
	 * Returns the previous response id argument captured from the last chaining-capable invocation.
	 *
	 * @return captured previous response id, or {@code null} if not yet invoked via the chaining overload
	 */
	public String getCapturedPreviousResponseId() {
		return capturedPreviousResponseId;
	}

	/**
	 * Replaces the stubbed result returned by all {@code createResponse} overloads.
	 *
	 * @param stubbedResult the new stubbed result
	 */
	public void setStubbedResult(OpenAiResponsesResult stubbedResult) {
		this.stubbedResult = stubbedResult;
	}
}
