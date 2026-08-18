package com.aidigital.aionboarding.service.link.services;

import com.aidigital.aionboarding.service.link.models.LinkMetadataRecord;

/**
 * Fetches and parses metadata from external HTTP links.
 */
public interface LinkMetadataService {

    /**
     * Retrieves title, description, image, site name, and extracted text for a URL.
     *
     * @param url HTTP or HTTPS link to fetch
     * @return metadata record; {@link LinkMetadataRecord#error} is empty on success
     */
    LinkMetadataRecord fetch(String url);
}
