package com.aidigital.aionboarding.service.lesson.services.entity;

import com.aidigital.aionboarding.domain.lesson.entities.LessonMaterial;
import com.aidigital.aionboarding.domain.lesson.models.MaterialUsageCount;
import com.aidigital.aionboarding.domain.lesson.repositories.LessonMaterialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * Short-transaction CRUD helpers for the {@link LessonMaterial} join entity.
 * <p>
 * This is the only service that may inject {@link LessonMaterialRepository} directly.
 */
@Service
@RequiredArgsConstructor
public class LessonMaterialEntityService {

    private final LessonMaterialRepository lessonMaterialRepository;

    /**
     * Returns all lesson-material joins for a lesson, ordered by sort order.
     *
     * @param lessonId lesson identifier
     * @return lesson-material joins in display order
     */
    @Transactional(readOnly = true)
    public List<LessonMaterial> findByLessonIdOrderBySortOrderAsc(Long lessonId) {
        return lessonMaterialRepository.findByLessonIdOrderBySortOrderAsc(lessonId);
    }

    /**
     * Returns lesson-material joins for several lessons, ordered by lesson and sort order.
     *
     * @param lessonIds lesson identifiers
     * @return lesson-material joins for the given lessons
     */
    @Transactional(readOnly = true)
    public List<LessonMaterial> findByLessonIdsOrderByLessonIdAscSortOrderAsc(Collection<Long> lessonIds) {
        if (lessonIds == null || lessonIds.isEmpty()) {
            return List.of();
        }
        return lessonMaterialRepository.findByLessonIdsOrderByLessonIdAscSortOrderAsc(lessonIds);
    }

    /**
     * Counts how many lessons reference a material.
     *
     * @param materialId material identifier
     * @return lesson usage count
     */
    @Transactional(readOnly = true)
    public long countByMaterialId(Long materialId) {
        return lessonMaterialRepository.countByMaterial_Id(materialId);
    }

    /**
     * Returns lesson titles that reference a material.
     *
     * @param materialId material identifier
     * @param pageable   page bounds
     * @return matching lesson titles
     */
    @Transactional(readOnly = true)
    public List<String> findLessonTitlesByMaterialId(Long materialId, Pageable pageable) {
        return lessonMaterialRepository.findLessonTitlesByMaterialId(materialId, pageable);
    }

    /**
     * Counts material usage for a batch of material identifiers.
     *
     * @param materialIds material identifiers
     * @return material usage counts
     */
    @Transactional(readOnly = true)
    public List<MaterialUsageCount> countUsageByMaterialIds(Collection<Long> materialIds) {
        return lessonMaterialRepository.countUsageByMaterialIds(materialIds);
    }

    /**
     * Persists a batch of lesson-material joins.
     *
     * @param links lesson-material joins to save
     * @return saved joins
     */
    @Transactional
    public List<LessonMaterial> saveAll(List<LessonMaterial> links) {
        return lessonMaterialRepository.saveAll(links);
    }
}
