package com.aidigital.aionboarding.service.material.services.impl;

import com.aidigital.aionboarding.domain.material.entities.Material;
import com.aidigital.aionboarding.domain.material.entities.MaterialYoutubeUrl;
import com.aidigital.aionboarding.domain.material.repositories.MaterialYoutubeUrlRepository;
import com.aidigital.aionboarding.external.youtube.YoutubeClient;
import com.aidigital.aionboarding.external.youtube.model.YoutubeOEmbedMetadata;
import com.aidigital.aionboarding.service.common.mapping.TextValueNormalizer;
import com.aidigital.aionboarding.service.mappers.material.MaterialMapper;
import com.aidigital.aionboarding.service.material.services.MaterialYoutubeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MaterialYoutubeServiceImpl implements MaterialYoutubeService {

    private static final int YOUTUBE_METADATA_BACKFILL_LIMIT = 12;

    private final MaterialYoutubeUrlRepository materialYoutubeUrlRepository;
    private final YoutubeClient youtubeClient;
    private final TextValueNormalizer textValueNormalizer;
    private final MaterialMapper materialMapper;

    @Override
    public void backfillMissingYoutubeMetadata() {
        List<MaterialYoutubeUrl> missing = materialYoutubeUrlRepository.findWithMissingMetadata(
            PageRequest.of(0, YOUTUBE_METADATA_BACKFILL_LIMIT)
        );
        for (MaterialYoutubeUrl row : missing) {
            YoutubeOEmbedMetadata metadata = youtubeClient.fetchOembed(row.getUrl());
            row.setTitle(textValueNormalizer.raw(metadata.title()));
            row.setAuthorName(textValueNormalizer.raw(metadata.authorName()));
            row.setAuthorUrl(textValueNormalizer.raw(metadata.authorUrl()));
            row.setThumbnailUrl(textValueNormalizer.raw(metadata.thumbnailUrl()));
            row.setThumbnailWidth(metadata.thumbnailWidth());
            row.setThumbnailHeight(metadata.thumbnailHeight());
            row.setProviderName(textValueNormalizer.raw(metadata.providerName()));
            row.setMetadataError(textValueNormalizer.raw(metadata.error()));
            materialYoutubeUrlRepository.save(row);
        }
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
            materialYoutubeUrlRepository.save(entity);
        }
    }

    @Override
    public void deleteByMaterialId(Long materialId) {
        materialYoutubeUrlRepository.deleteByMaterial_Id(materialId);
    }

    @Override
    public List<MaterialYoutubeUrl> findByMaterialIdOrderBySortOrderAsc(Long materialId) {
        return materialYoutubeUrlRepository.findByMaterialIdOrderBySortOrderAsc(materialId);
    }

    @Override
    public List<MaterialYoutubeUrl> findByMaterialIdsOrderBySortOrderAsc(Collection<Long> materialIds) {
        if (materialIds == null || materialIds.isEmpty()) {
            return List.of();
        }
        return materialYoutubeUrlRepository.findByMaterialIdInOrderBySortOrderAsc(materialIds);
    }

}
