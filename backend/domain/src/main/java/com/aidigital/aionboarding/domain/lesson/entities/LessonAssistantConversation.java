package com.aidigital.aionboarding.domain.lesson.entities;

import com.aidigital.aionboarding.domain.common.entities.IdAwareEntity;
import com.aidigital.aionboarding.domain.user.entities.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * One learner's saved lesson-assistant chat for one lesson, restored when they return to the
 * lesson. One row per (user, lesson) pair.
 */
@Entity
@Table(name = "lesson_assistant_conversations")
@Getter
@Setter
@NoArgsConstructor
public class LessonAssistantConversation extends IdAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Column(name = "messages", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private List<Map<String, Object>> messages;

    @Column(nullable = false)
    private String preset;

    @Column(name = "last_response_id")
    private String lastResponseId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Optimistic-locking version, so two browser tabs saving turns to the same conversation at
     * once cannot silently overwrite one another's state.
     */
    @Version
    @Column(nullable = false)
    private Long version;
}
