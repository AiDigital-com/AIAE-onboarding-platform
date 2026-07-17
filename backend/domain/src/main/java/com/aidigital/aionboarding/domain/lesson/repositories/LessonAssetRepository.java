package com.aidigital.aionboarding.domain.lesson.repositories;

import com.aidigital.aionboarding.domain.lesson.entities.LessonAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

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

    @Query("SELECT a.lesson.id FROM LessonAsset a WHERE a.storageKey = :storageKey")
    Optional<Long> findLessonIdByStorageKey(@Param("storageKey") String storageKey);
}
