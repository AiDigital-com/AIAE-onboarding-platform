package com.aidigital.aionboarding.domain.lessonactivity.entities;

import com.aidigital.aionboarding.domain.common.dictionary.entities.ActivityProgressStatus;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.user.entities.User;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Hibernate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

@Entity
@Table(name = "user_lesson_activity_progress")
@Getter
@Setter
@NoArgsConstructor
public class UserLessonActivityProgress {

    @EmbeddedId
    private UserLessonActivityProgressId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("activityId")
    @JoinColumn(name = "activity_id", nullable = false)
    private LessonActivity activity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    private ActivityProgressStatus status;

    private BigDecimal score;

    @Column(name = "metadata", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> metadata;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Optimistic-locking version, so a quiz/flashcard completion and a progress reset racing on
     * the same (user, activity) row cannot silently overwrite one another's score/status.
     */
    @Version
    @Column(nullable = false)
    private Long version;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    public static class UserLessonActivityProgressId implements Serializable {

        @Column(name = "user_id")
        private Long userId;

        @Column(name = "activity_id")
        private Long activityId;

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) {
                return false;
            }
            UserLessonActivityProgressId that = (UserLessonActivityProgressId) other;
            return Objects.equals(userId, that.userId)
                    && Objects.equals(activityId, that.activityId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, activityId);
        }
    }
}
