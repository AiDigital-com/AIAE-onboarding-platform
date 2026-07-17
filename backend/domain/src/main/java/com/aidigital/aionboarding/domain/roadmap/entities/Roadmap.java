package com.aidigital.aionboarding.domain.roadmap.entities;

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

@Entity
@Table(name = "roadmaps")
@Getter
@Setter
@NoArgsConstructor
public class Roadmap extends IdAwareEntity {

	@Column(name = "legacy_source_id", unique = true)
	private String legacySourceId;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false)
	private String description;

	@Column(name = "tags", columnDefinition = "jsonb", nullable = false)
	@JdbcTypeCode(SqlTypes.JSON)
	private List<String> tags;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "author_user_id")
	private User authorUser;

	@Column(name = "created_by", nullable = false)
	private String createdBy;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	/**
	 * Optimistic-locking version, so concurrent updates to the same roadmap (title/description/
	 * tags/lesson-list edits) cannot silently overwrite one another.
	 */
	@Version
	@Column(nullable = false)
	private Long version;
}
