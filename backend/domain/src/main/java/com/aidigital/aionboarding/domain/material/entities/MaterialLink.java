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
@Table(name = "material_links")
@Getter
@Setter
@NoArgsConstructor
public class MaterialLink extends IdAwareEntity {

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

	@Column(nullable = false)
	private String description;

	@Column(name = "image_url", nullable = false)
	private String imageUrl;

	@Column(name = "site_name", nullable = false)
	private String siteName;

	@Column(name = "extracted_text", nullable = false)
	private String extractedText;

	@Column(name = "metadata_error", nullable = false)
	private String metadataError;
}
