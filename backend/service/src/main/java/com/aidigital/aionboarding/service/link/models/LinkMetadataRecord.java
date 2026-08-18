package com.aidigital.aionboarding.service.link.models;

/**
 * Metadata extracted from an external link, replacing the untyped {@code Map<String, Object>}
 * {@link com.aidigital.aionboarding.service.link.services.LinkMetadataService#fetch} previously
 * returned.
 *
 * @param title         page title, or Open Graph/Twitter card title when present
 * @param description   page description
 * @param imageUrl      representative image URL
 * @param siteName      site or host name
 * @param extractedText main page text, bounded and normalized
 * @param error         fetch/parse error message; empty on success
 */
public record LinkMetadataRecord(
    String title,
    String description,
    String imageUrl,
    String siteName,
    String extractedText,
    String error
) { }
