package com.aidigital.aionboarding.service.material.support;

import com.aidigital.aionboarding.service.material.models.DuplicateTitle;
import com.aidigital.aionboarding.service.material.models.DuplicateUrl;
import com.aidigital.aionboarding.service.material.models.MaterialPreparationItem;
import com.aidigital.aionboarding.service.material.models.OverlapNotes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Detects duplicate titles and repeated URLs across prepared materials.
 */
@Component
@RequiredArgsConstructor
public class OverlapDetector {

    /**
     * Detects title and URL overlaps across the given materials.
     *
     * @param materials prepared material items to compare
     * @return overlap notes containing duplicate titles and URLs
     */
    public OverlapNotes detectOverlaps(List<MaterialPreparationItem> materials) {
        List<DuplicateTitle> duplicateTitles = detectDuplicateTitles(materials);
        List<DuplicateUrl> duplicateUrls = detectDuplicateUrls(materials);
        return new OverlapNotes(duplicateTitles, duplicateUrls);
    }

    List<DuplicateTitle> detectDuplicateTitles(List<MaterialPreparationItem> materials) {
        Map<String, DuplicateTitle> seen = new HashMap<>();
        Set<String> duplicates = new HashSet<>();
        for (MaterialPreparationItem material : materials) {
            Object titleValue = material.data().get("title");
            if (titleValue == null) {
                continue;
            }
            String title = String.valueOf(titleValue).trim();
            if (title.isBlank()) {
                continue;
            }
            String key = title.toLowerCase();
            if (seen.containsKey(key)) {
                duplicates.add(key);
            } else {
                seen.put(key, new DuplicateTitle(title));
            }
        }
        return duplicates.stream().map(seen::get).toList();
    }

    List<DuplicateUrl> detectDuplicateUrls(List<MaterialPreparationItem> materials) {
        Map<String, Integer> urlCounts = new HashMap<>();
        for (MaterialPreparationItem material : materials) {
            collectUrls(material).forEach(url -> urlCounts.merge(url, 1, Integer::sum));
        }
        return urlCounts.entrySet().stream()
            .filter(entry -> entry.getValue() > 1)
            .map(entry -> new DuplicateUrl(entry.getKey()))
            .toList();
    }

    List<String> collectUrls(MaterialPreparationItem material) {
        List<String> urls = new ArrayList<>();
        Map<String, Object> data = material.data();
        Object links = data.get("links");
        if (links instanceof List<?> list) {
            for (Object item : list) {
                if (item != null) {
                    String url = String.valueOf(item).trim();
                    if (!url.isBlank()) {
                        urls.add(url);
                    }
                }
            }
        }
        Object youtubeUrls = data.get("youtubeUrls");
        if (youtubeUrls instanceof List<?> list) {
            for (Object item : list) {
                if (item != null) {
                    String url = String.valueOf(item).trim();
                    if (!url.isBlank()) {
                        urls.add(url);
                    }
                }
            }
        }
        Object linkAssets = data.get("linkAssets");
        if (linkAssets instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map && map.get("url") != null) {
                    String url = String.valueOf(map.get("url")).trim();
                    if (!url.isBlank()) {
                        urls.add(url);
                    }
                }
            }
        }
        return urls;
    }
}
