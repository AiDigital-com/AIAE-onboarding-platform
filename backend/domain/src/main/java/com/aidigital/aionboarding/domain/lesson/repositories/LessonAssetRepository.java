package com.aidigital.aionboarding.domain.lesson.repositories;

import com.aidigital.aionboarding.domain.lesson.entities.LessonAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LessonAssetRepository extends JpaRepository<LessonAsset, Long> {

    @Query("SELECT a FROM LessonAsset a JOIN FETCH a.kind WHERE a.lesson.id = :lessonId ORDER BY a.createdAt ASC")
    List<LessonAsset> findByLessonIdOrderByCreatedAtAsc(@Param("lessonId") Long lessonId);

    @Query("""
        SELECT a
        FROM LessonAsset a
        JOIN FETCH a.kind
        WHERE a.lesson.id IN :lessonIds
        ORDER BY a.lesson.id ASC, a.createdAt ASC
        """)
    List<LessonAsset> findByLessonIdsOrderByLessonIdAscCreatedAtAsc(@Param("lessonIds") java.util.Collection<Long> lessonIds);

    /**
     * Ids of every lesson that owns an asset with this storage key. More than one lesson can own
     * the same key, because a duplicated lesson shares the original's uploaded files.
     */
    @Query("SELECT DISTINCT a.lesson.id FROM LessonAsset a WHERE a.storageKey = :storageKey")
    List<Long> findLessonIdsByStorageKey(@Param("storageKey") String storageKey);
}
