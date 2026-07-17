package com.aidigital.aionboarding.service.lesson.support;

import com.aidigital.aionboarding.domain.lesson.entities.LessonAssistantConversation;
import com.aidigital.aionboarding.service.lesson.models.ChatTurn;
import com.aidigital.aionboarding.service.lesson.models.LessonAssistantConversationRecord;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LessonAssistantConversationAssemblerTest {

	private final LessonAssistantConversationAssembler assembler = new LessonAssistantConversationAssembler();

	@Test
	void toRecordShouldConvertEntityMessagesAndFieldsTest() {
		// Given:
		LessonAssistantConversation conversation = new LessonAssistantConversation();
		conversation.setMessages(List.of(
				rawMessage("user", "What is this lesson about?"),
				rawMessage("assistant", "It covers onboarding basics.")
		));
		conversation.setPreset("small_portions");
		conversation.setLastResponseId("resp-1");

		// When:
		LessonAssistantConversationRecord result = assembler.toRecord(conversation);

		// Then:
		assertThat(result.messages()).containsExactly(
				new ChatTurn("user", "What is this lesson about?"),
				new ChatTurn("assistant", "It covers onboarding basics.")
		);
		assertThat(result.preset()).isEqualTo("small_portions");
	}

	@Test
	void toChatTurnsShouldSkipEntriesMissingRoleOrContentTest() {
		// Given:
		List<Map<String, Object>> rawMessages = List.of(
				rawMessage("user", "Valid question"),
				Map.of("role", "assistant"),
				Map.of("content", "No role here")
		);

		// When:
		List<ChatTurn> result = assembler.toChatTurns(rawMessages);

		// Then:
		assertThat(result).containsExactly(new ChatTurn("user", "Valid question"));
	}

	@Test
	void toChatTurnsWithNullListShouldReturnEmptyListTest() {
		// When:
		List<ChatTurn> result = assembler.toChatTurns(null);

		// Then:
		assertThat(result).isEmpty();
	}

	@Test
	void appendTurnShouldAppendQuestionThenAnswerToExistingMessagesTest() {
		// Given:
		List<Map<String, Object>> existing = List.of(rawMessage("user", "Earlier question"));

		// When:
		List<Map<String, Object>> result = assembler.appendTurn(existing, "New question", "New answer");

		// Then:
		assertThat(result).hasSize(3);
		assertThat(result.get(1)).containsEntry("role", "user").containsEntry("content", "New question");
		assertThat(result.get(2)).containsEntry("role", "assistant").containsEntry("content", "New answer");
		assertThat(existing).hasSize(1);
	}

	@Test
	void appendTurnWithNullExistingMessagesShouldStartFreshTest() {
		// When:
		List<Map<String, Object>> result = assembler.appendTurn(null, "Question", "Answer");

		// Then:
		assertThat(result).hasSize(2);
	}

	private Map<String, Object> rawMessage(String role, String content) {
		Map<String, Object> message = new LinkedHashMap<>();
		message.put("role", role);
		message.put("content", content);
		return message;
	}
}
