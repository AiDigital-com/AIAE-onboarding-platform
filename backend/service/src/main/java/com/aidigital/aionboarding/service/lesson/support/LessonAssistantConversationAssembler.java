package com.aidigital.aionboarding.service.lesson.support;

import com.aidigital.aionboarding.domain.lesson.entities.LessonAssistantConversation;
import com.aidigital.aionboarding.service.lesson.models.ChatTurn;
import com.aidigital.aionboarding.service.lesson.models.LessonAssistantConversationRecord;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts between the {@link LessonAssistantConversation} JSONB storage shape and the plain
 * {@link ChatTurn}/{@link LessonAssistantConversationRecord} service models.
 */
@Component
public class LessonAssistantConversationAssembler {

    private static final String ROLE_KEY = "role";
    private static final String CONTENT_KEY = "content";

    /**
     * Converts a saved conversation entity into its service-facing record.
     *
     * @param conversation saved conversation entity
     * @return the equivalent conversation record
     */
    public LessonAssistantConversationRecord toRecord(LessonAssistantConversation conversation) {
        return new LessonAssistantConversationRecord(toChatTurns(conversation.getMessages()), conversation.getPreset());
    }

    /**
     * Converts raw JSONB message maps into typed chat turns, skipping entries with no content.
     *
     * @param rawMessages raw {@code {role, content}} maps loaded from storage
     * @return equivalent chat turns
     */
    List<ChatTurn> toChatTurns(List<Map<String, Object>> rawMessages) {
        if (rawMessages == null) {
            return List.of();
        }
        List<ChatTurn> turns = new ArrayList<>();
        for (Map<String, Object> rawMessage : rawMessages) {
            Object role = rawMessage.get(ROLE_KEY);
            Object content = rawMessage.get(CONTENT_KEY);
            if (role != null && content != null) {
                turns.add(new ChatTurn(role.toString(), content.toString()));
            }
        }
        return turns;
    }

    /**
     * Appends the learner's question and the assistant's answer to a saved message list, ready to
     * persist back as JSONB.
     *
     * @param rawMessages existing raw {@code {role, content}} maps loaded from storage
     * @param question learner's question text
     * @param answer assistant's answer text
     * @return the extended raw message list
     */
    public List<Map<String, Object>> appendTurn(List<Map<String, Object>> rawMessages, String question, String answer) {
        List<Map<String, Object>> updated = rawMessages == null ? new ArrayList<>() : new ArrayList<>(rawMessages);
        updated.add(rawMessage("user", question));
        updated.add(rawMessage("assistant", answer));
        return updated;
    }

    /**
     * Builds one raw {@code {role, content}} map for JSONB storage.
     *
     * @param role message role ({@code user} or {@code assistant})
     * @param content message text
     * @return the raw message map
     */
    Map<String, Object> rawMessage(String role, String content) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put(ROLE_KEY, role);
        message.put(CONTENT_KEY, content);
        return message;
    }
}
