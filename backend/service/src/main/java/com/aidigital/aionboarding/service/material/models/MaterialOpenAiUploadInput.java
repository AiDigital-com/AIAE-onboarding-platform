package com.aidigital.aionboarding.service.material.models;

public record MaterialOpenAiUploadInput(
    String openaiFileId,
    String openaiFilePurpose,
    String openaiFileStatus,
    String openaiFileError
) { }
