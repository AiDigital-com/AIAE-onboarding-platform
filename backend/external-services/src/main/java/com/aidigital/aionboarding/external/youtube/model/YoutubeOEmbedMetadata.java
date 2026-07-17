package com.aidigital.aionboarding.external.youtube.model;

/**
 * oEmbed metadata for a YouTube video.
 */
public record YoutubeOEmbedMetadata(
    String title,
    String authorName,
    String authorUrl,
    String thumbnailUrl,
    Integer thumbnailWidth,
    Integer thumbnailHeight,
    String providerName,
    String error
) {
}
