package com.aidigital.aionboarding.service.lesson.services.entity;

import com.aidigital.aionboarding.domain.lesson.entities.LessonAssistantConversation;
import com.aidigital.aionboarding.domain.lesson.repositories.LessonAssistantConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Short-transaction CRUD helpers for the {@link LessonAssistantConversation} entity. This is the
 * only service that may inject {@link LessonAssistantConversationRepository} directly.
 */
@Service
@RequiredArgsConstructor
public class LessonAssistantConversationEntityService {

    private final LessonAssistantConversationRepository lessonAssistantConversationRepository;

    /**
     * Loads one learner's saved chat for one lesson, if present.
     *
     * @param userId   internal user primary key
     * @param lessonId lesson primary key
     * @return the matching conversation, when present
     */
    @Transactional(readOnly = true)
    public Optional<LessonAssistantConversation> findByUserIdAndLessonId(Long userId, Long lessonId) {
        return lessonAssistantConversationRepository.findByUserIdAndLessonId(userId, lessonId);
    }

    /**
     * Persists a lesson-assistant conversation.
     *
     * @param conversation the conversation to save
     * @return the saved {@link LessonAssistantConversation}
     */
    @Transactional
    public LessonAssistantConversation save(LessonAssistantConversation conversation) {
        return lessonAssistantConversationRepository.save(conversation);
    }

    /**
     * Deletes one learner's saved chat for one lesson, if present.
     *
     * @param userId   internal user primary key
     * @param lessonId lesson primary key
     */
    @Transactional
    public void deleteByUserIdAndLessonId(Long userId, Long lessonId) {
        lessonAssistantConversationRepository.deleteByUserIdAndLessonId(userId, lessonId);
    }
}
