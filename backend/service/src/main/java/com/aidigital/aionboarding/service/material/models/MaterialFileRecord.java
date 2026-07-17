package com.aidigital.aionboarding.service.material.models;

import java.time.LocalDateTime;

public record MaterialFileRecord(
		Long id,
		String name,
		String storageKey,
		String mimeType,
		Long size,
		String kind,
		String openaiFileId,
		String openaiFilePurpose,
		String openaiFileStatus,
		String openaiFileError,
		LocalDateTime openaiUploadedAt
) {

}
