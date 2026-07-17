package com.aidigital.aionboarding.service.lesson.models;

import java.util.List;

/**
 * A learner's saved lesson-assistant chat for one lesson. The provider continuation id is
 * intentionally not part of this record: it never leaves the server, so a client cannot select
 * or replay another conversation's continuation state.
 *
 * @param messages saved chat turns in chronological order
 * @param preset   assistant response mode active when the chat was last saved
 */
public record LessonAssistantConversationRecord(
		List<ChatTurn> messages,
		String preset
) {

}
