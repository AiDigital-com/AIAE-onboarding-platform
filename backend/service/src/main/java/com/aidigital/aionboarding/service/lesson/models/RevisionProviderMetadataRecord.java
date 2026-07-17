package com.aidigital.aionboarding.service.lesson.models;

public record RevisionProviderMetadataRecord(
    String provider,
    String model,
    String promptVersion,
    String promptCacheKey,
    String rawOutput
) { }
