package com.aidigital.aionboarding.domain.lesson.repositories;

import com.aidigital.aionboarding.domain.lesson.entities.LessonAssistantConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LessonAssistantConversationRepository extends JpaRepository<LessonAssistantConversation, Long> {

	Optional<LessonAssistantConversation> findByUserIdAndLessonId(Long userId, Long lessonId);

	void deleteByUserIdAndLessonId(Long userId, Long lessonId);
}
