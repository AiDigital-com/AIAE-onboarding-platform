package com.aidigital.aionboarding.service.lessongen.services;

import com.aidigital.aionboarding.external.openai.model.OpenAiFileUploadResponse;
import com.aidigital.aionboarding.service.lessongen.model.GeneratedActivityResult;
import com.aidigital.aionboarding.service.lessongen.model.GeneratedContentResult;
import com.aidigital.aionboarding.service.lessongen.model.GeneratedRevisionBriefResult;
import com.aidigital.aionboarding.service.lessongen.model.LessonGenPrompt;

import java.util.Map;

/**
 * Low-level OpenAI integration for lesson generation, revision planning, activity payloads, and file uploads.
 */
public interface LessonGenService {

	/**
	 * Condenses long source text into a shorter transcript suitable for downstream lesson generation.
	 *
	 * @param prompt instructions, input text, version, and cache key for the compression request
	 * @return condensed text and provider metadata
	 * @throws com.aidigital.aionboarding.service.common.error.AppException when OpenAI is not configured,
	 *                                                                      the request fails, or the model returns
	 *                                                                      empty content
	 */
	GeneratedContentResult condenseSourceText(LessonGenPrompt prompt);

	/**
	 * Generates full lesson content from a prepared prompt.
	 *
	 * @param prompt instructions, input context, version, and cache key for the generation request
	 * @return generated lesson body and provider metadata
	 * @throws com.aidigital.aionboarding.service.common.error.AppException when OpenAI is not configured,
	 *                                                                      the request fails, or the model returns
	 *                                                                      empty content
	 */
	GeneratedContentResult generateLessonContent(LessonGenPrompt prompt);

	/**
	 * Produces a structured revision brief describing scope, intent, and edit guidance.
	 *
	 * @param prompt instructions and current lesson context for revision planning
	 * @return parsed revision brief map and provider metadata
	 * @throws com.aidigital.aionboarding.service.common.error.AppException when OpenAI is not configured,
	 *                                                                      the request fails, or the model returns
	 *                                                                      invalid JSON
	 */
	GeneratedRevisionBriefResult generateLessonRevisionBrief(LessonGenPrompt prompt);

	/**
	 * Generates a JSON activity payload for a lesson activity type.
	 *
	 * @param prompt instructions and lesson context for activity generation
	 * @return parsed activity payload and provider metadata
	 * @throws com.aidigital.aionboarding.service.common.error.AppException when OpenAI is not configured,
	 *                                                                      the request fails, or the model returns
	 *                                                                      invalid JSON
	 */
	GeneratedActivityResult generateLessonActivityPayload(LessonGenPrompt prompt);

	/**
	 * Extracts the first JSON object from a model response string.
	 *
	 * @param value raw model output that may contain JSON
	 * @return parsed JSON map, or {@code null} when no valid JSON object is found
	 */
	Map<String, Object> extractJsonPayload(String value);

	/**
	 * Uploads a file to OpenAI for use in lesson-generation prompts.
	 *
	 * @param content  file bytes
	 * @param filename original filename
	 * @param purpose  OpenAI file purpose (for example {@code assistants})
	 * @return OpenAI upload response including file identifier
	 * @throws com.aidigital.aionboarding.service.common.error.AppException when OpenAI is not configured
	 *                                                                      or the upload fails
	 */
	OpenAiFileUploadResponse uploadFile(byte[] content, String filename, String purpose);
}
