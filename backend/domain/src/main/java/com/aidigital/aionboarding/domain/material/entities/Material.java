package com.aidigital.aionboarding.domain.material.entities;

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
@Table(name = "materials")
@Getter
@Setter
@NoArgsConstructor
public class Material extends IdAwareEntity {

	@Column(name = "legacy_source_id", unique = true)
	private String legacySourceId;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false)
	private String description;

	@Column(name = "text_content", nullable = false)
	private String textContent;

	@Column(name = "cover_image_storage_key", nullable = false)
	private String coverImageStorageKey;

	@Column(name = "cover_image_original_name", nullable = false)
	private String coverImageOriginalName;

	@Column(name = "cover_image_mime_type", nullable = false)
	private String coverImageMimeType;

	@Column(name = "tags", columnDefinition = "jsonb", nullable = false)
	@JdbcTypeCode(SqlTypes.JSON)
	private List<String> tags;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by_user_id")
	private User createdByUser;

	@Column(name = "created_by", nullable = false)
	private String createdBy;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	/**
	 * Optimistic-locking version, so two admins editing the same shared material (referenced by
	 * multiple lessons) concurrently cannot silently overwrite one another.
	 */
	@Version
	@Column(nullable = false)
	private Long version;
}
