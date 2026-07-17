package com.aidigital.aionboarding.service.material.models;

import java.time.LocalDateTime;

public record MaterialAttachmentInput(
    Long id,
    String originalName,
    String storageKey,
    String mimeType,
    Long sizeBytes,
    String kind,
    String openaiFileId,
    String openaiFilePurpose,
    String openaiFileStatus,
    String openaiFileError,
    LocalDateTime openaiUploadedAt
) { }
