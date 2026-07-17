package com.aidigital.aionboarding.domain.user.entities;

import com.aidigital.aionboarding.domain.common.dictionary.entities.UserRole;
import com.aidigital.aionboarding.domain.common.entities.IdAwareEntity;
import com.aidigital.aionboarding.domain.grade.entities.Grade;
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

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User extends IdAwareEntity {

	@Column(name = "clerk_user_id", unique = true)
	private String clerkUserId;

	@Column(name = "legacy_source_id", unique = true)
	private String legacySourceId;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false, unique = true)
	private String email;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "role_id", nullable = false)
	private UserRole role;

	/**
	 * Configurable grade (e.g. Junior/Middle/Senior); {@code null} means no grade assigned yet.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "grade_id")
	private Grade grade;

	private String position;

	@Column(name = "avatar_storage_key")
	private String avatarStorageKey;

	@Column(name = "avatar_color")
	private String avatarColor;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	/**
	 * Optimistic-locking version, so concurrent edits to this user's role, grade, or profile
	 * fields cannot silently overwrite one another.
	 */
	@Version
	@Column(nullable = false)
	private Long version;
}
