package com.aidigital.aionboarding.service.material.models;

import java.time.LocalDateTime;

public record MaterialOpenAiUploadRecord(
    Long id,
    String openaiFileId,
    String openaiFilePurpose,
    String openaiFileStatus,
    String openaiFileError,
    LocalDateTime openaiUploadedAt
) { }
