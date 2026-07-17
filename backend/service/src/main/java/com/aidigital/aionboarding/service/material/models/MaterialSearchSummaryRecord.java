package com.aidigital.aionboarding.service.material.models;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Bounded per-card summary for Library material search: the full body text is replaced by a
 * presence flag, and link/attachment children carry only their preview fields — never the
 * extracted link text or OpenAI file-upload internals of {@link MaterialRecord}.
 */
public record MaterialSearchSummaryRecord(
    Long id,
    String title,
    String description,
    boolean hasText,
    String coverImageStorageKey,
    String coverImageOriginalName,
    String coverImageMimeType,
    Long createdByUserId,
    String createdBy,
    List<String> tags,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    Long usageCount,
    List<String> youtubeUrls,
    List<MaterialYoutubeVideoRecord> youtubeVideos,
    List<String> links,
    List<MaterialLinkSummaryRecord> linkAssets,
    List<MaterialFileSummaryRecord> attachments
) { }
