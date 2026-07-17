package com.aidigital.aionboarding.service.lesson.support;

import com.aidigital.aionboarding.domain.common.dictionary.LessonPublicationStatusCode;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.lesson.entities.LessonAsset;
import com.aidigital.aionboarding.domain.lesson.entities.LessonMaterial;
import com.aidigital.aionboarding.domain.lesson.repositories.LessonSearchSummaryProjection;
import com.aidigital.aionboarding.service.lesson.models.LessonAssetRecord;
import com.aidigital.aionboarding.service.lesson.models.LessonDetailRecord;
import com.aidigital.aionboarding.service.lesson.models.LessonSearchSummaryRecord;
import com.aidigital.aionboarding.service.lesson.models.LessonSummaryRecord;
import com.aidigital.aionboarding.service.lesson.models.RevisionBriefRecord;
import com.aidigital.aionboarding.service.lesson.models.RevisionHistoryItemRecord;
import com.aidigital.aionboarding.service.lesson.models.TeacherVideoRecord;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonAssetEntityService;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonEntityService;
import com.aidigital.aionboarding.service.material.models.MaterialRecord;
import com.aidigital.aionboarding.service.material.services.MaterialRecordQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class LessonRecordAssembler {

    private final LessonEntityService lessonEntityService;
    private final LessonAssetEntityService lessonAssetEntityService;
    private final MaterialRecordQueryService materialRecordQueryService;

    public LessonSummaryRecord toSummaryRecord(Lesson lesson) {
        return new LessonSummaryRecord(
            lesson.getId(),
            lesson.getTitle(),
            lesson.getStatus().getCode(),
            lesson.getPublicationStatus().getCode(),
            lesson.getContentMarkdown(),
            lesson.getContentHtml(),
            lesson.getCoverImageStorageKey(),
            lesson.getCoverImageOriginalName(),
            lesson.getCoverImageMimeType(),
            lesson.getCreatedBy(),
            lesson.getCreatedAt(),
            lesson.getUpdatedAt(),
            lesson.getTags() == null ? List.of() : lesson.getTags()
        );
    }

    /**
     * Converts a bounded Library search projection into its service-layer summary record.
     *
     * @param projection lean per-card projection selected directly by the repository
     * @return the assembled {@link LessonSearchSummaryRecord}
     */
    public LessonSearchSummaryRecord toListItemRecord(LessonSearchSummaryProjection projection) {
        return new LessonSearchSummaryRecord(
            projection.id(),
            projection.title(),
            projection.statusCode(),
            projection.publicationStatusCode(),
            projection.contentHtmlPreview(),
            projection.contentMarkdownPreview(),
            projection.coverImageStorageKey(),
            projection.coverImageOriginalName(),
            projection.coverImageMimeType(),
            projection.tags() == null ? List.of() : projection.tags(),
            projection.createdBy(),
            projection.createdAt(),
            projection.updatedAt()
        );
    }

    public LessonDetailRecord toDetailRecord(Lesson lesson) {
        LessonMaterialSnapshot lessonMaterials = lessonMaterialSnapshot(lesson.getId());
        return toDetailRecord(
            lesson,
            lessonMaterials.materialIds(),
            lessonMaterials.sourceReferences(),
            lessonAssets(lesson.getId())
        );
    }

    public LessonDetailRecord toDetailRecord(
        Lesson lesson,
        List<Long> materialIds,
        List<MaterialRecord> sourceReferences,
        List<LessonAssetRecord> lessonAssets
    ) {
        return new LessonDetailRecord(
            lesson.getId(),
            lesson.getTitle(),
            lesson.getDescription(),
            lesson.getStatus().getCode(),
            lesson.getPublicationStatus().getCode(),
            LessonPublicationStatusCode.PUBLISHED.equals(lesson.getPublicationStatus().getCode()),
            LessonPublicationStatusCode.ARCHIVED.equals(lesson.getPublicationStatus().getCode()),
            lesson.getUserInstructions(),
            lesson.getDepth(),
            lesson.getTone(),
            lesson.getDesiredFormat(),
            lesson.getContentFormat().getCode(),
            lesson.getContentMarkdown(),
            lesson.getContentHtml(),
            lesson.getCoverImageStorageKey(),
            lesson.getCoverImageOriginalName(),
            lesson.getCoverImageMimeType(),
            lesson.getTags() == null ? List.of() : lesson.getTags(),
            lesson.getGenerationMetadata() == null ? new HashMap<>() : lesson.getGenerationMetadata(),
            toRevisionHistoryRecords(lesson.getRevisionHistory()),
            lesson.getErrorMessage(),
            lesson.getCreatedBy(),
            lesson.getCreatedByUser() == null ? null : lesson.getCreatedByUser().getId(),
            lesson.getCreatedAt(),
            lesson.getUpdatedAt(),
            lesson.getPublishedAt(),
            materialIds,
            sourceReferences,
            lessonAssets,
            null
        );
    }

    public LessonAssetRecord toAssetRecord(LessonAsset asset) {
        return new LessonAssetRecord(
            asset.getId(),
            asset.getLesson().getId(),
            asset.getKind().getCode(),
            asset.getTitle(),
            firstNonBlank(asset.getOriginalName(), asset.getTitle(), "Untitled asset"),
            asset.getUrl(),
            asset.getDescription(),
            asset.getImageUrl(),
            asset.getSiteName(),
            asset.getStorageKey(),
            asset.getMimeType(),
            asset.getSizeBytes(),
            asset.getMetadata() == null ? Map.of() : asset.getMetadata(),
            asset.getCreatedAt()
        );
    }

    public Map<String, Object> toDetailMap(Lesson lesson) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", lesson.getId());
        map.put("title", lesson.getTitle());
        map.put("description", lesson.getDescription());
        map.put("status", lesson.getStatus().getCode());
        map.put("publicationStatus", lesson.getPublicationStatus().getCode());
        map.put("isPublished", LessonPublicationStatusCode.PUBLISHED.equals(lesson.getPublicationStatus().getCode()));
        map.put("isArchived", LessonPublicationStatusCode.ARCHIVED.equals(lesson.getPublicationStatus().getCode()));
        map.put("userInstructions", lesson.getUserInstructions());
        map.put("depth", lesson.getDepth());
        map.put("tone", lesson.getTone());
        map.put("desiredFormat", lesson.getDesiredFormat());
        map.put("contentFormat", lesson.getContentFormat().getCode());
        map.put("contentMarkdown", lesson.getContentMarkdown());
        map.put("contentHtml", lesson.getContentHtml());
        map.put("coverImageStorageKey", lesson.getCoverImageStorageKey());
        map.put("coverImageOriginalName", lesson.getCoverImageOriginalName());
        map.put("coverImageMimeType", lesson.getCoverImageMimeType());
        map.put("tags", lesson.getTags() == null ? List.of() : lesson.getTags());
        map.put("generationMetadata", lesson.getGenerationMetadata() == null ? new HashMap<>() : lesson.getGenerationMetadata());
        map.put("revisionHistory", lesson.getRevisionHistory() == null ? List.of() : lesson.getRevisionHistory());
        map.put("errorMessage", lesson.getErrorMessage());
        map.put("createdBy", lesson.getCreatedBy());
        map.put("createdByUserId", lesson.getCreatedByUser() == null ? null : lesson.getCreatedByUser().getId());
        map.put("createdAt", lesson.getCreatedAt());
        map.put("updatedAt", lesson.getUpdatedAt());
        map.put("publishedAt", lesson.getPublishedAt());
        map.put("materialIds", materialIds(lesson.getId()));
        map.put("lessonAssets", lessonAssetMaps(lesson.getId()));
        return map;
    }

    public List<RevisionHistoryItemRecord> toRevisionHistoryRecords(List<Object> history) {
        if (history == null) {
            return List.of();
        }
        return history.stream()
            .filter(item -> item instanceof Map<?, ?>)
            .map(item -> toRevisionHistoryItemRecord(mapVal(item)))
            .toList();
    }

    public RevisionHistoryItemRecord toRevisionHistoryItemRecord(Map<String, Object> map) {
        return new RevisionHistoryItemRecord(
            stringVal(map.get("revisedAt")),
            stringVal(map.get("revisionRequest")),
            stringList(map.get("selectedOptions")),
            toRevisionBriefRecord(mapVal(map.get("revisionBrief"))),
            mapVal(map.get("plannerPrompt")),
            mapVal(map.get("writerPrompt")),
            mapVal(map.get("planner")),
            mapVal(map.get("writer"))
        );
    }

    public RevisionBriefRecord toRevisionBriefRecord(Map<String, Object> brief) {
        if (brief == null || brief.isEmpty()) {
            return new RevisionBriefRecord("substantial", "", List.of(), List.of(), List.of());
        }
        return new RevisionBriefRecord(
            stringVal(brief.get("changeScope")),
            stringVal(brief.get("userIntent")),
            stringList(brief.get("editInstructions")),
            stringList(brief.get("preserveRules")),
            stringList(brief.get("riskNotes"))
        );
    }

    public Map<String, Object> toRevisionBriefMap(RevisionBriefRecord brief) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("changeScope", brief.changeScope());
        map.put("userIntent", brief.userIntent());
        map.put("editInstructions", brief.editInstructions());
        map.put("preserveRules", brief.preserveRules());
        map.put("riskNotes", brief.riskNotes());
        return map;
    }

    public TeacherVideoRecord toTeacherVideoRecord(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        return new TeacherVideoRecord(
            stringVal(map.get("provider")),
            stringVal(map.get("prompt")),
            stringVal(map.get("avatarId")),
            stringVal(map.get("voiceId")),
            stringVal(map.get("sessionId")),
            stringVal(map.get("videoId")),
            stringVal(map.get("status")),
            stringVal(map.get("createdAt")),
            map.get("durationLimitSeconds") instanceof Number number ? number.intValue() : null,
            stringVal(map.get("checkedAt")),
            stringVal(map.get("videoUrl")),
            stringVal(map.get("thumbnailUrl")),
            map.get("duration"),
            stringVal(map.get("completedAt")),
            stringVal(map.get("failedAt"))
        );
    }

    public Map<String, Object> toTeacherVideoMap(TeacherVideoRecord record) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (record == null) {
            return map;
        }
        if (record.provider() != null && !record.provider().isBlank()) {
            map.put("provider", record.provider());
        }
        putIfPresent(map, "prompt", record.prompt());
        putIfPresent(map, "avatarId", record.avatarId());
        putIfPresent(map, "voiceId", record.voiceId());
        putIfPresent(map, "sessionId", record.sessionId());
        putIfPresent(map, "videoId", record.videoId());
        putIfPresent(map, "status", record.status());
        putIfPresent(map, "createdAt", record.createdAt());
        if (record.durationLimitSeconds() != null) {
            map.put("durationLimitSeconds", record.durationLimitSeconds());
        }
        putIfPresent(map, "checkedAt", record.checkedAt());
        putIfPresent(map, "videoUrl", record.videoUrl());
        putIfPresent(map, "thumbnailUrl", record.thumbnailUrl());
        if (record.duration() != null) {
            map.put("duration", record.duration());
        }
        putIfPresent(map, "completedAt", record.completedAt());
        putIfPresent(map, "failedAt", record.failedAt());
        return map;
    }

    public TeacherVideoRecord normalizeTeacherVideoRecord(TeacherVideoRecord record, String checkedAt) {
        if (record == null) {
            return null;
        }
        return new TeacherVideoRecord(
            "heygen",
            record.prompt(),
            record.avatarId(),
            record.voiceId(),
            record.sessionId(),
            record.videoId(),
            record.status(),
            record.createdAt(),
            record.durationLimitSeconds(),
            checkedAt,
            record.videoUrl(),
            record.thumbnailUrl(),
            record.duration(),
            record.completedAt(),
            record.failedAt()
        );
    }

    List<Long> materialIds(Long lessonId) {
        return lessonEntityService.findLessonMaterialsByLessonId(lessonId).stream()
            .map(LessonMaterial::getMaterial)
            .map(material -> material.getId())
            .toList();
    }

    List<MaterialRecord> sourceReferences(Long lessonId) {
        List<Long> orderedIds = lessonEntityService.findLessonMaterialsByLessonId(lessonId).stream()
            .map(lm -> lm.getId().getMaterialId())
            .toList();
        return orderedMaterialRecords(orderedIds);
    }

    LessonMaterialSnapshot lessonMaterialSnapshot(Long lessonId) {
        List<Long> orderedIds = lessonEntityService.findLessonMaterialsByLessonId(lessonId).stream()
            .map(lm -> lm.getId().getMaterialId())
            .toList();
        return new LessonMaterialSnapshot(orderedIds, orderedMaterialRecords(orderedIds));
    }

    List<MaterialRecord> orderedMaterialRecords(List<Long> orderedIds) {
        if (orderedIds.isEmpty()) {
            return List.of();
        }
        Map<Long, MaterialRecord> recordsById = materialRecordQueryService.loadMaterialRecordsByIds(orderedIds).stream()
            .collect(LinkedHashMap::new, (map, record) -> map.put(record.id(), record), LinkedHashMap::putAll);
        return orderedIds.stream()
            .map(recordsById::get)
            .filter(Objects::nonNull)
            .toList();
    }

    List<LessonAssetRecord> lessonAssets(Long lessonId) {
        return lessonAssetEntityService.findByLessonIdOrderByCreatedAtAsc(lessonId).stream()
            .map(this::toAssetRecord)
            .toList();
    }

    List<Map<String, Object>> lessonAssetMaps(Long lessonId) {
        return lessonAssetEntityService.findByLessonIdOrderByCreatedAtAsc(lessonId).stream()
            .map(this::toAssetMap)
            .toList();
    }

    Map<String, Object> toAssetMap(LessonAsset asset) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", asset.getId());
        map.put("lessonId", asset.getLesson().getId());
        map.put("kind", asset.getKind().getCode());
        map.put("title", asset.getTitle());
        map.put("name", firstNonBlank(asset.getOriginalName(), asset.getTitle(), "Untitled asset"));
        map.put("url", asset.getUrl());
        map.put("description", asset.getDescription());
        map.put("imageUrl", asset.getImageUrl());
        map.put("siteName", asset.getSiteName());
        map.put("storageKey", asset.getStorageKey());
        map.put("mimeType", asset.getMimeType());
        map.put("size", asset.getSizeBytes());
        map.put("metadata", asset.getMetadata() == null ? Map.of() : asset.getMetadata());
        map.put("createdAt", asset.getCreatedAt());
        return map;
    }

    @SuppressWarnings("unchecked")
    List<String> stringList(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object item : list) {
            if (item != null) {
                result.add(String.valueOf(item));
            }
        }
        return result;
    }

    void putIfPresent(Map<String, Object> map, String key, String value) {
        if (value != null && !value.isBlank()) {
            map.put(key, value);
        }
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> mapVal(Object raw) {
        if (raw instanceof Map<?, ?> map) {
            return new LinkedHashMap<>((Map<String, Object>) map);
        }
        return new LinkedHashMap<>();
    }

    String stringVal(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    record LessonMaterialSnapshot(List<Long> materialIds, List<MaterialRecord> sourceReferences) { }
}
