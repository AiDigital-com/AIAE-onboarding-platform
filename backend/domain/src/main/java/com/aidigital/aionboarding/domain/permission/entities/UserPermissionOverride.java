package com.aidigital.aionboarding.domain.permission.entities;

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
@Table(name = "user_permission_overrides")
@Getter
@Setter
@NoArgsConstructor
public class UserPermissionOverride {

	@EmbeddedId
	private UserPermissionOverrideId id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId("userId")
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "is_allowed", nullable = false)
	private Boolean allowed;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "granted_by_user_id")
	private User grantedByUser;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@Embeddable
	@Getter
	@Setter
	@NoArgsConstructor
	public static class UserPermissionOverrideId implements Serializable {

		@Column(name = "user_id")
		private Long userId;

		@Column(name = "permission_key")
		private String permissionKey;

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) {
				return false;
			}
			UserPermissionOverrideId that = (UserPermissionOverrideId) other;
			return Objects.equals(userId, that.userId)
					&& Objects.equals(permissionKey, that.permissionKey);
		}

		@Override
		public int hashCode() {
			return Objects.hash(userId, permissionKey);
		}
	}
}
