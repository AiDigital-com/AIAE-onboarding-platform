package com.aidigital.aionboarding.domain.material.repositories;

import com.aidigital.aionboarding.domain.material.entities.MaterialFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MaterialFileRepository extends JpaRepository<MaterialFile, Long> {

    @Query("SELECT f FROM MaterialFile f JOIN FETCH f.kind WHERE f.material.id = :materialId")
    List<MaterialFile> findByMaterialId(@Param("materialId") Long materialId);

    @Query("SELECT f FROM MaterialFile f JOIN FETCH f.kind WHERE f.material.id = :materialId ORDER BY f.createdAt ASC")
    List<MaterialFile> findByMaterialIdOrderByCreatedAtAsc(@Param("materialId") Long materialId);

    @Query("SELECT f FROM MaterialFile f JOIN FETCH f.kind WHERE f.material.id IN :materialIds ORDER BY f.material.id ASC, f.createdAt ASC")
    List<MaterialFile> findByMaterialIdInOrderByCreatedAtAsc(@Param("materialIds") Collection<Long> materialIds);

    void deleteByMaterial_Id(Long materialId);

    @Query("SELECT f FROM MaterialFile f WHERE f.storageKey = :storageKey")
    Optional<MaterialFile> findByStorageKey(@Param("storageKey") String storageKey);

    /**
     * Returns bounded attachment summary projections for the given materials, omitting the
     * OpenAI file-upload internals a Library search card does not need.
     *
     * @param materialIds material identifiers to restrict to
     * @return attachment summaries ordered by material id, then creation time
     */
    @Query("""
        SELECT NEW com.aidigital.aionboarding.domain.material.repositories.MaterialFileSummaryProjection(
            f.material.id, f.id, f.originalName, f.storageKey, f.mimeType, f.sizeBytes, f.kind.code, f.createdAt
        )
        FROM MaterialFile f
        WHERE f.material.id IN :materialIds
        ORDER BY f.material.id ASC, f.createdAt ASC
        """)
    List<MaterialFileSummaryProjection> findSummariesByMaterialIdIn(@Param("materialIds") Collection<Long> materialIds);
}
