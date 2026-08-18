package com.aidigital.aionboarding.domain.lesson.repositories;

import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public interface LessonRepository extends JpaRepository<Lesson, Long>, JpaSpecificationExecutor<Lesson>,
		LessonRepositoryCustom {

	/**
	 * Atomically claims the right to refresh a lesson's teacher-video status: increments
	 * {@code version} only when it still equals {@code expectedVersion}, so at most one of two
	 * nodes racing to refresh the same read-triggered lesson proceeds to call the video
	 * provider. The other observes {@code 0} rows updated and returns the lesson unchanged
	 * instead of duplicating the provider call and risking a spurious optimistic-lock conflict
	 * on what was, from its caller's point of view, only a read.
	 * <p>
	 * Bulk update statements are exempt from JPA's automatic optimistic-lock enforcement (the
	 * JPA spec: "bulk update and delete operations are not required to enforce optimistic
	 * locking semantics"), so the version predicate and increment here are both written out
	 * explicitly. This reuses the lesson's existing {@code @Version} column rather than adding
	 * a dedicated refresh-timestamp column, and it does not weaken {@code @Version}'s normal
	 * protection for content edits: any concurrent {@code save()} that already read the
	 * pre-claim version will still fail its own version check once this claim commits, exactly
	 * as it would against any other concurrent writer.
	 *
	 * @param id             lesson primary key
	 * @param expectedVersion version the caller last observed
	 * @return {@code 1} when this call won the claim; {@code 0} when another writer already
	 *         changed the version first
	 */
	@Modifying
	@Query("UPDATE Lesson l SET l.version = l.version + 1 WHERE l.id = :id AND l.version = :expectedVersion")
	int claimForTeacherVideoRefresh(@Param("id") Long id, @Param("expectedVersion") Long expectedVersion);

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
