package com.aidigital.aionboarding.service.material.models;

public record MaterialLinkAssetRecord(
		String url,
		String title,
		String description,
		String imageUrl,
		String siteName,
		String extractedText,
		String metadataError
) {

}
