package com.aidigital.aionboarding.domain.material.entities;

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

@Entity
@Table(name = "material_youtube_urls")
@Getter
@Setter
@NoArgsConstructor
public class MaterialYoutubeUrl extends IdAwareEntity {

	@Column(name = "legacy_source_id", unique = true)
	private String legacySourceId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "material_id", nullable = false)
	private Material material;

	@Column(nullable = false)
	private String url;

	@Column(name = "sort_order", nullable = false)
	private Integer sortOrder;

	@Column(nullable = false)
	private String title;

	@Column(name = "author_name", nullable = false)
	private String authorName;

	@Column(name = "author_url", nullable = false)
	private String authorUrl;

	@Column(name = "thumbnail_url", nullable = false)
	private String thumbnailUrl;

	@Column(name = "thumbnail_width")
	private Integer thumbnailWidth;

	@Column(name = "thumbnail_height")
	private Integer thumbnailHeight;

	@Column(name = "provider_name", nullable = false)
	private String providerName;

	@Column(name = "metadata_error", nullable = false)
	private String metadataError;
}
