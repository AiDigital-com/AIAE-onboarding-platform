package com.aidigital.aionboarding.service.material.models;

public record MaterialYoutubeVideoRecord(
    String url,
    String title,
    String authorName,
    String authorUrl,
    String thumbnailUrl,
    Integer thumbnailWidth,
    Integer thumbnailHeight,
    String providerName,
    String metadataError
) { }
