package com.aidigital.aionboarding.service.link.services;

import java.util.Map;

/**
 * Fetches and parses metadata from external HTTP links.
 */
public interface LinkMetadataService {

    /**
     * Retrieves title, description, image, site name, and extracted text for a URL.
     *
     * @param url HTTP or HTTPS link to fetch
     * @return metadata map with keys {@code title}, {@code description}, {@code imageUrl},
     *     {@code siteName}, {@code extractedText}, and {@code error}; {@code error} is empty on success
     */
    Map<String, Object> fetch(String url);
}
