package com.aidigital.aionboarding.domain.learning.entities;

import com.aidigital.aionboarding.domain.roadmap.entities.Roadmap;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Hibernate;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "user_roadmaps")
@Getter
@Setter
@NoArgsConstructor
public class UserRoadmap {

	@EmbeddedId
	private UserRoadmapId id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId("userId")
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId("roadmapId")
	@JoinColumn(name = "roadmap_id", nullable = false)
	private Roadmap roadmap;

	@Column(name = "enrolled_at", nullable = false)
	private LocalDateTime enrolledAt;

	@Embeddable
	@Getter
	@Setter
	@NoArgsConstructor
	public static class UserRoadmapId implements Serializable {

		@Column(name = "user_id")
		private Long userId;

		@Column(name = "roadmap_id")
		private Long roadmapId;

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) {
				return false;
			}
			UserRoadmapId that = (UserRoadmapId) other;
			return Objects.equals(userId, that.userId)
					&& Objects.equals(roadmapId, that.roadmapId);
		}

		@Override
		public int hashCode() {
			return Objects.hash(userId, roadmapId);
		}
	}
}
