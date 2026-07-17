package com.aidigital.aionboarding.domain.group.entities;

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

/**
 * One user's membership in one group. A user may belong to more than one group.
 */
@Entity
@Table(name = "group_members")
@Getter
@Setter
@NoArgsConstructor
public class GroupMember {

	@EmbeddedId
	private GroupMemberId id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId("groupId")
	@JoinColumn(name = "group_id", nullable = false)
	private Group group;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId("memberUserId")
	@JoinColumn(name = "member_user_id", nullable = false)
	private User memberUser;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Embeddable
	@Getter
	@Setter
	@NoArgsConstructor
	public static class GroupMemberId implements Serializable {

		@Column(name = "group_id")
		private Long groupId;

		@Column(name = "member_user_id")
		private Long memberUserId;

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) {
				return false;
			}
			GroupMemberId that = (GroupMemberId) other;
			return Objects.equals(groupId, that.groupId) && Objects.equals(memberUserId, that.memberUserId);
		}

		@Override
		public int hashCode() {
			return Objects.hash(groupId, memberUserId);
		}
	}
}
