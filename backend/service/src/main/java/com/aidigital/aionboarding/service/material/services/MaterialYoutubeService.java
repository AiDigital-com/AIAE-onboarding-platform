package com.aidigital.aionboarding.service.material.services;

import com.aidigital.aionboarding.domain.material.entities.Material;
import com.aidigital.aionboarding.domain.material.entities.MaterialYoutubeUrl;

import java.util.Collection;
import java.util.List;

/**
 * Persists YouTube URL rows for materials and backfills missing oEmbed metadata.
 */
public interface MaterialYoutubeService {

    /**
     * Backfills oEmbed metadata for stored YouTube URLs that are missing it.
     */
    void backfillMissingYoutubeMetadata();

    /**
     * Fetches oEmbed metadata for each URL without persisting anything.
     * Safe to call outside a database transaction.
     *
     * @param urls YouTube URLs to prefetch
     * @return list of prepared records containing fetched oEmbed metadata
     */
    List<PreparedYoutubeRecord> prepareYoutubeMetadata(List<String> urls);

    /**
     * Persists pre-fetched YouTube records for the given material.
     * Must be called inside a database transaction.
     *
     * @param material parent material entity
     * @param records  pre-fetched YouTube records to persist
     */
    void saveYoutubeUrls(Material material, List<PreparedYoutubeRecord> records);

    /**
     * Deletes all YouTube URL rows for the given material.
     *
     * @param materialId material identifier
     */
    void deleteByMaterialId(Long materialId);

    /**
     * Returns all YouTube URL rows for the given material, ordered by sort order ascending.
     *
     * @param materialId material identifier
     * @return YouTube URLs for the material, in sort order
     */
    List<MaterialYoutubeUrl> findByMaterialIdOrderBySortOrderAsc(Long materialId);

    /**
     * Returns YouTube URL rows for exactly the given materials, ordered by sort order ascending.
     *
     * @param materialIds material identifiers to load rows for
     * @return YouTube URLs for the given materials, in sort order
     */
    List<MaterialYoutubeUrl> findByMaterialIdsOrderBySortOrderAsc(Collection<Long> materialIds);

    /**
     * Pre-fetched oEmbed metadata for a single YouTube URL.
     */
    record PreparedYoutubeRecord(
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
}
