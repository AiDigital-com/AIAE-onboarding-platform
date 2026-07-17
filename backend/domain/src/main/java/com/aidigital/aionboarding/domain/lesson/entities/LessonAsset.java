package com.aidigital.aionboarding.domain.lesson.entities;

import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonAssetKind;
import com.aidigital.aionboarding.domain.common.entities.IdAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "lesson_assets")
@Getter
@Setter
@NoArgsConstructor
public class LessonAsset extends IdAwareEntity {

	@Column(name = "legacy_source_id", unique = true)
	private String legacySourceId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "lesson_id", nullable = false)
	private Lesson lesson;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "kind_id", nullable = false)
	private LessonAssetKind kind;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false)
	private String url;

	@Column(nullable = false)
	private String description;

	@Column(name = "image_url", nullable = false)
	private String imageUrl;

	@Column(name = "site_name", nullable = false)
	private String siteName;

	@Column(name = "original_name", nullable = false)
	private String originalName;

	@Column(name = "storage_key", nullable = false)
	private String storageKey;

	@Column(name = "mime_type", nullable = false)
	private String mimeType;

	@Column(name = "size_bytes", nullable = false)
	private Long sizeBytes;

	@Column(name = "metadata", columnDefinition = "jsonb", nullable = false)
	@JdbcTypeCode(SqlTypes.JSON)
	private Map<String, Object> metadata;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;
}
