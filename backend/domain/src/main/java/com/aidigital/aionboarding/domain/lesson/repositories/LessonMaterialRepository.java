package com.aidigital.aionboarding.domain.lesson.repositories;

import com.aidigital.aionboarding.domain.lesson.entities.LessonMaterial;
import com.aidigital.aionboarding.domain.lesson.models.MaterialUsageCount;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface LessonMaterialRepository
		extends JpaRepository<LessonMaterial, LessonMaterial.LessonMaterialId> {

	@Query("SELECT lm FROM LessonMaterial lm WHERE lm.lesson.id = :lessonId ORDER BY lm.sortOrder ASC")
	java.util.List<LessonMaterial> findByLessonIdOrderBySortOrderAsc(@Param("lessonId") Long lessonId);

	@Query("""
			SELECT lm
			FROM LessonMaterial lm
			WHERE lm.lesson.id IN :lessonIds
			ORDER BY lm.lesson.id ASC, lm.sortOrder ASC
			""")
	java.util.List<LessonMaterial> findByLessonIdsOrderByLessonIdAscSortOrderAsc(
			@Param("lessonIds") Collection<Long> lessonIds
	);

	long countByMaterial_Id(Long materialId);

	@Query("""
			SELECT lm.lesson.title FROM LessonMaterial lm
			WHERE lm.material.id = :materialId
			ORDER BY lm.lesson.title ASC
			""")
	java.util.List<String> findLessonTitlesByMaterialId(
			@Param("materialId") Long materialId,
			Pageable pageable
	);

	@Query("""
			SELECT new com.aidigital.aionboarding.domain.lesson.models.MaterialUsageCount(lm.material.id, COUNT(lm))
			FROM LessonMaterial lm
			WHERE lm.material.id IN :materialIds
			GROUP BY lm.material.id
			""")
	java.util.List<MaterialUsageCount> countUsageByMaterialIds(@Param("materialIds") Collection<Long> materialIds);
}
