package com.aidigital.aionboarding.service.material.models;

import java.util.List;

public record CreateMaterialInput(
		String title,
		String description,
		String text,
		List<String> youtubeUrls,
		List<String> links,
		List<MaterialAttachmentInput> attachments,
		List<String> tags,
		String coverImageStorageKey,
		String coverImageOriginalName,
		String coverImageMimeType
) {

}
