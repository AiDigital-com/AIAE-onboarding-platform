package com.aidigital.aionboarding.service.material.services.impl;

import com.aidigital.aionboarding.domain.material.entities.Material;
import com.aidigital.aionboarding.domain.material.entities.MaterialYoutubeUrl;
import com.aidigital.aionboarding.external.youtube.YoutubeClient;
import com.aidigital.aionboarding.external.youtube.model.YoutubeOEmbedMetadata;
import com.aidigital.aionboarding.service.common.mapping.TextValueNormalizer;
import com.aidigital.aionboarding.service.mappers.material.MaterialMapper;
import com.aidigital.aionboarding.service.material.services.MaterialYoutubeService;
import com.aidigital.aionboarding.service.material.services.entity.MaterialYoutubeUrlEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MaterialYoutubeServiceImpl implements MaterialYoutubeService {

    private static final int YOUTUBE_METADATA_BACKFILL_LIMIT = 12;

    private final MaterialYoutubeUrlEntityService materialYoutubeUrlEntityService;
    private final YoutubeClient youtubeClient;
    private final TextValueNormalizer textValueNormalizer;
    private final MaterialMapper materialMapper;

    /**
     * Claims a bounded batch of rows still missing oEmbed metadata (short transaction,
     * {@code FOR UPDATE SKIP LOCKED}), then fetches each row's metadata from YouTube with no
     * transaction open, then persists each fetched result in its own short transaction. This
     * three-phase shape is required by {@code .claude/rules/14-performance.md}: a database
     * transaction must never span a third-party HTTP call. A row that fails to fetch aborts
     * the remaining rows in this tick (matching the previous behaviour) but does not roll back
     * rows already saved earlier in the same tick, since each save already committed on its own.
     */
    @Override
    public void backfillMissingYoutubeMetadata() {
        List<MaterialYoutubeUrl> claimed =
            materialYoutubeUrlEntityService.claimMissingMetadataBatch(YOUTUBE_METADATA_BACKFILL_LIMIT);
        for (MaterialYoutubeUrl row : claimed) {
            YoutubeOEmbedMetadata metadata = youtubeClient.fetchOembed(row.getUrl());
            applyFetchedMetadata(row, metadata);
            materialYoutubeUrlEntityService.save(row);
        }
    }

    /**
     * Copies fetched oEmbed metadata onto a claimed row, in memory only.
     *
     * @param row      claimed row to update
     * @param metadata fetched oEmbed metadata
     */
    void applyFetchedMetadata(MaterialYoutubeUrl row, YoutubeOEmbedMetadata metadata) {
        row.setTitle(textValueNormalizer.raw(metadata.title()));
        row.setAuthorName(textValueNormalizer.raw(metadata.authorName()));
        row.setAuthorUrl(textValueNormalizer.raw(metadata.authorUrl()));
        row.setThumbnailUrl(textValueNormalizer.raw(metadata.thumbnailUrl()));
        row.setThumbnailWidth(metadata.thumbnailWidth());
        row.setThumbnailHeight(metadata.thumbnailHeight());
        row.setProviderName(textValueNormalizer.raw(metadata.providerName()));
        row.setMetadataError(textValueNormalizer.raw(metadata.error()));
    }

    @Override
    public List<PreparedYoutubeRecord> prepareYoutubeMetadata(List<String> urls) {
        List<PreparedYoutubeRecord> records = new ArrayList<>();
        for (String url : urls) {
            YoutubeOEmbedMetadata metadata = youtubeClient.fetchOembed(url);
            records.add(new PreparedYoutubeRecord(
                url,
                textValueNormalizer.raw(metadata.title()),
                textValueNormalizer.raw(metadata.authorName()),
                textValueNormalizer.raw(metadata.authorUrl()),
                textValueNormalizer.raw(metadata.thumbnailUrl()),
                metadata.thumbnailWidth(),
                metadata.thumbnailHeight(),
                textValueNormalizer.raw(metadata.providerName()),
                textValueNormalizer.raw(metadata.error())
            ));
        }
        return records;
    }

    @Override
    public void saveYoutubeUrls(Material material, List<PreparedYoutubeRecord> records) {
        for (int index = 0; index < records.size(); index += 1) {
            PreparedYoutubeRecord record = records.get(index);
            MaterialYoutubeUrl entity = materialMapper.toNewMaterialYoutubeUrl(material, record, index);
            materialYoutubeUrlEntityService.save(entity);
        }
    }

    @Override
    public void deleteByMaterialId(Long materialId) {
        materialYoutubeUrlEntityService.deleteByMaterialId(materialId);
    }

    @Override
    public List<MaterialYoutubeUrl> findByMaterialIdOrderBySortOrderAsc(Long materialId) {
        return materialYoutubeUrlEntityService.findByMaterialIdOrderBySortOrderAsc(materialId);
    }

    @Override
    public List<MaterialYoutubeUrl> findByMaterialIdsOrderBySortOrderAsc(Collection<Long> materialIds) {
        if (materialIds == null || materialIds.isEmpty()) {
            return List.of();
        }
        return materialYoutubeUrlEntityService.findByMaterialIdsOrderBySortOrderAsc(materialIds);
    }

}
