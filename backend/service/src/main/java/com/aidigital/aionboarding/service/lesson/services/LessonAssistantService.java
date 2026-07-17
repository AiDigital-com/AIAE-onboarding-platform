package com.aidigital.aionboarding.service.lesson.services;

import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.lesson.enums.LessonAssistantPreset;
import com.aidigital.aionboarding.service.lesson.models.AskLessonResultRecord;
import com.aidigital.aionboarding.service.lesson.models.ChatTurn;
import com.aidigital.aionboarding.service.lesson.models.LessonAssistantConversationRecord;

import java.util.List;

/**
 * Answers learner questions about a published lesson using prepared source materials and OpenAI.
 */
public interface LessonAssistantService {

	/**
	 * Generates an assistant reply grounded in lesson content and attached materials. Whether
	 * this is a follow-up turn, and which provider response to continue from, is determined
	 * entirely from the caller's own saved conversation row — never from client input — so a
	 * browser can neither select nor inject another conversation's continuation state.
	 *
	 * @param viewer   authenticated learner enrolled in the lesson
	 * @param lessonId lesson identifier
	 * @param question learner question (required, max 2000 characters)
	 * @param history  prior chat turns supplied by the client
	 * @param preset   assistant response mode; {@code SMALL_PORTIONS} explains in short chunks and
	 *                 invites the learner to ask for the next part instead of answering in full
	 * @return assistant answer and generation metadata
	 * @throws com.aidigital.aionboarding.service.common.error.AppException when the caller lacks
	 *                                                                      {@code learning.ask}, the lesson is
	 *                                                                      missing or not published, the question is
	 *                                                                      invalid,
	 *                                                                      or OpenAI is unavailable or returns an
	 *                                                                      empty response
	 */
	AskLessonResultRecord ask(
			AppUser viewer, Long lessonId, String question, List<ChatTurn> history, LessonAssistantPreset preset);

	/**
	 * Loads a learner's saved lesson-assistant chat for one lesson, so it can be restored after
	 * navigating away.
	 *
	 * @param viewer   authenticated learner enrolled in the lesson
	 * @param lessonId lesson identifier
	 * @return the saved conversation, or an empty conversation when nothing is saved yet
	 * @throws com.aidigital.aionboarding.service.common.error.AppException when the caller lacks
	 *                                                                      {@code learning.ask} or is not enrolled in
	 *                                                                      the lesson
	 */
	LessonAssistantConversationRecord getConversation(AppUser viewer, Long lessonId);

	/**
	 * Deletes a learner's saved lesson-assistant chat for one lesson.
	 *
	 * @param viewer   authenticated learner
	 * @param lessonId lesson identifier
	 * @throws com.aidigital.aionboarding.service.common.error.AppException when the caller lacks
	 *                                                                      {@code learning.ask}
	 */
	void clearConversation(AppUser viewer, Long lessonId);
}
