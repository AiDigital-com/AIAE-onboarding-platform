package com.aidigital.aionboarding.domain.material.repositories;

/**
 * Bounded per-link projection for Library material search: preview fields only, omitting the
 * extracted page text and metadata error carried by the full {@code MaterialLink} entity.
 */
public record MaterialLinkSummaryProjection(
    Long materialId,
    String url,
    String title,
    String description,
    String imageUrl,
    String siteName
) { }
