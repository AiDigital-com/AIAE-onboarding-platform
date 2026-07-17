package com.aidigital.aionboarding.domain.lesson.entities;

import com.aidigital.aionboarding.domain.material.entities.Material;
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
@Table(name = "lesson_materials")
@Getter
@Setter
@NoArgsConstructor
public class LessonMaterial {

	@EmbeddedId
	private LessonMaterialId id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId("lessonId")
	@JoinColumn(name = "lesson_id", nullable = false)
	private Lesson lesson;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId("materialId")
	@JoinColumn(name = "material_id", nullable = false)
	private Material material;

	@Column(name = "sort_order", nullable = false)
	private Integer sortOrder;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Embeddable
	@Getter
	@Setter
	@NoArgsConstructor
	public static class LessonMaterialId implements Serializable {

		@Column(name = "lesson_id")
		private Long lessonId;

		@Column(name = "material_id")
		private Long materialId;

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) {
				return false;
			}
			LessonMaterialId that = (LessonMaterialId) other;
			return Objects.equals(lessonId, that.lessonId)
					&& Objects.equals(materialId, that.materialId);
		}

		@Override
		public int hashCode() {
			return Objects.hash(lessonId, materialId);
		}
	}
}
