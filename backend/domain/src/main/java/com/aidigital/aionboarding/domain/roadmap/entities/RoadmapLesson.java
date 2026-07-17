package com.aidigital.aionboarding.domain.roadmap.entities;

import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Hibernate;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "roadmap_lessons")
@Getter
@Setter
@NoArgsConstructor
public class RoadmapLesson {

	@EmbeddedId
	private RoadmapLessonId id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId("roadmapId")
	@JoinColumn(name = "roadmap_id", nullable = false)
	private Roadmap roadmap;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId("lessonId")
	@JoinColumn(name = "lesson_id", nullable = false)
	private Lesson lesson;

	@Column(name = "sort_order", nullable = false)
	private Integer sortOrder;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Embeddable
	@Getter
	@Setter
	@NoArgsConstructor
	public static class RoadmapLessonId implements Serializable {

		@Column(name = "roadmap_id")
		private Long roadmapId;

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
			RoadmapLessonId that = (RoadmapLessonId) other;
			return Objects.equals(roadmapId, that.roadmapId)
					&& Objects.equals(lessonId, that.lessonId);
		}

		@Override
		public int hashCode() {
			return Objects.hash(roadmapId, lessonId);
		}
	}
}
