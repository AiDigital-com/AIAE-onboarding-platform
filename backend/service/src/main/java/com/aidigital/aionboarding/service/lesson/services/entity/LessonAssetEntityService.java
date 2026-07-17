package com.aidigital.aionboarding.service.lesson.services.entity;

import com.aidigital.aionboarding.domain.lesson.entities.LessonAsset;
import com.aidigital.aionboarding.domain.lesson.repositories.LessonAssetRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Short-transaction CRUD helpers for the {@link LessonAsset} entity.
 * <p>
 * This is the only service that may inject {@link LessonAssetRepository} directly.
 * All other services that require lesson-asset data must depend on this service.
 */
@Service
@RequiredArgsConstructor
public class LessonAssetEntityService {

    private final LessonAssetRepository lessonAssetRepository;

    /**
     * Loads every lesson asset for a lesson, ordered by creation time ascending.
     *
     * @param lessonId the owning lesson's primary key
     * @return every {@link LessonAsset} row for the given lesson, oldest first
     */
    @Transactional(readOnly = true)
    public List<LessonAsset> findByLessonIdOrderByCreatedAtAsc(Long lessonId) {
        return lessonAssetRepository.findByLessonIdOrderByCreatedAtAsc(lessonId);
    }

    /**
     * Loads lesson assets for several lessons, ordered by lesson and creation time.
     *
     * @param lessonIds owning lesson IDs
     * @return matching lesson assets
     */
    @Transactional(readOnly = true)
    public List<LessonAsset> findByLessonIdsOrderByCreatedAtAsc(Collection<Long> lessonIds) {
        if (lessonIds == null || lessonIds.isEmpty()) {
            return List.of();
        }
        return lessonAssetRepository.findByLessonIdsOrderByLessonIdAscCreatedAtAsc(lessonIds);
    }

    /**
     * Persists a lesson asset row.
     *
     * @param lessonAsset the lesson asset to save
     * @return the saved {@link LessonAsset}
     */
    @Transactional
    public LessonAsset save(LessonAsset lessonAsset) {
        return lessonAssetRepository.save(lessonAsset);
    }

    /**
     * Finds a lesson asset by id.
     *
     * @param assetId lesson asset identifier
     * @return matching lesson asset, when present
     */
    @Transactional(readOnly = true)
    public Optional<LessonAsset> findById(Long assetId) {
        return lessonAssetRepository.findById(assetId);
    }

    /**
     * Deletes a lesson asset row.
     *
     * @param lessonAsset lesson asset to delete
     */
    @Transactional
    public void delete(LessonAsset lessonAsset) {
        lessonAssetRepository.delete(lessonAsset);
    }
}
