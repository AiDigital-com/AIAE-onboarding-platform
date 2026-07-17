package com.aidigital.aionboarding.service.material.models;

import java.time.LocalDateTime;
import java.util.List;

public record MaterialRecord(
    Long id,
    String title,
    String description,
    String text,
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
    List<MaterialLinkAssetRecord> linkAssets,
    List<MaterialFileRecord> attachments
) { }
