package com.aidigital.aionboarding.service.material.services;

import com.aidigital.aionboarding.external.openai.model.OpenAiFileInput;

import java.util.List;

/**
 * Prepares typed OpenAI file inputs for a set of material IDs by uploading compatible attachments
 * to the OpenAI Files API (or reusing a previously uploaded file ID) and returning the typed
 * {@link OpenAiFileInput} items for use in a generation request (D-05).
 */
public interface MaterialOpenAiFilePreparationService {

	/**
	 * Returns a typed {@link OpenAiFileInput} list for all compatible attachments belonging to the
	 * given material IDs. Attachments that are already uploaded are reused without a second upload;
	 * compatible attachments without an existing file ID are uploaded and their upload outcome is
	 * persisted (D-05, D-06). An empty or null input returns an empty list.
	 *
	 * @param materialIds material identifiers whose attachments should be prepared
	 * @return typed file inputs ready for the generation request; empty when the client is unavailable
	 * or no compatible attachments exist
	 */
	List<OpenAiFileInput> prepareFileInputs(List<Long> materialIds);
}
