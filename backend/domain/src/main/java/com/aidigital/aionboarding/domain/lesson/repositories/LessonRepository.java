package com.aidigital.aionboarding.domain.lesson.repositories;

import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public interface LessonRepository extends JpaRepository<Lesson, Long>, JpaSpecificationExecutor<Lesson>,
		LessonRepositoryCustom {

	@Query("SELECT l FROM Lesson l " +
			"JOIN FETCH l.status " +
			"JOIN FETCH l.publicationStatus " +
			"JOIN FETCH l.contentFormat " +
			"LEFT JOIN FETCH l.createdByUser " +
			"WHERE l.id = :id")
	Optional<Lesson> findByIdWithFetches(@Param("id") Long id);

	@Query("SELECT COUNT(l) > 0 FROM Lesson l WHERE l.coverImageStorageKey = :storageKey")
	boolean existsByCoverImageStorageKey(@Param("storageKey") String storageKey);

	/**
	 * Returns, among the given lesson IDs, those that have a teacher video with a video URL —
	 * for a card-level "has teacher video" flag without shipping the full generation metadata
	 * JSON blob.
	 *
	 * @param lessonIds the lesson primary keys to restrict to
	 * @return the subset of {@code lessonIds} that have a teacher video URL
	 */
	@Query("""
			SELECT l.id
			FROM Lesson l
			WHERE l.id IN :lessonIds
			  AND FUNCTION('jsonb_extract_path_text', l.generationMetadata, 'teacherVideo', 'videoUrl') IS NOT NULL
			""")
	Set<Long> findIdsWithTeacherVideoIn(@Param("lessonIds") Collection<Long> lessonIds);
}
