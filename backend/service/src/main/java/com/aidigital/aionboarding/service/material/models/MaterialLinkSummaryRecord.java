package com.aidigital.aionboarding.service.material.models;

/**
 * Bounded link preview for Library material search, omitting the extracted page text and
 * metadata error carried by {@link MaterialLinkAssetRecord}.
 */
public record MaterialLinkSummaryRecord(
		String url,
		String title,
		String description,
		String imageUrl,
		String siteName
) {

}
