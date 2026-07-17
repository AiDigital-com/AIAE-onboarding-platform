package com.aidigital.aionboarding.domain.learning.entities;

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

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "user_lessons")
@Getter
@Setter
@NoArgsConstructor
public class UserLesson {

	@EmbeddedId
	private UserLessonId id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId("userId")
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId("lessonId")
	@JoinColumn(name = "lesson_id", nullable = false)
	private Lesson lesson;

	@Column(name = "enrolled_at", nullable = false)
	private LocalDateTime enrolledAt;

	@Column(name = "completed_at")
	private LocalDateTime completedAt;

	/**
	 * Optimistic-locking version. {@code completedAt} has three independent writers (a manual
	 * complete/incomplete toggle, and two automatic side effects of quiz/flashcard submission)
	 * that can race on the same (user, lesson) row; this stops one from silently clobbering
	 * another's result.
	 */
	@Version
	@Column(nullable = false)
	private Long version;

	@Embeddable
	@Getter
	@Setter
	@NoArgsConstructor
	public static class UserLessonId implements Serializable {

		@Column(name = "user_id")
		private Long userId;

		@Column(name = "lesson_id")
		private Long lessonId;

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) {
				return false;
			}
			UserLessonId that = (UserLessonId) other;
			return Objects.equals(userId, that.userId)
					&& Objects.equals(lessonId, that.lessonId);
		}

		@Override
		public int hashCode() {
			return Objects.hash(userId, lessonId);
		}
	}
}
