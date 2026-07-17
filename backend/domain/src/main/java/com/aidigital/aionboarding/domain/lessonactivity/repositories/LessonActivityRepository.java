package com.aidigital.aionboarding.domain.lessonactivity.repositories;

import com.aidigital.aionboarding.domain.lessonactivity.entities.LessonActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LessonActivityRepository extends JpaRepository<LessonActivity, Long> {

	@Query("SELECT la FROM LessonActivity la WHERE la.lesson.id = :lessonId ORDER BY la.createdAt ASC")
	java.util.List<LessonActivity> findByLessonIdOrderByCreatedAtAsc(@Param("lessonId") Long lessonId);

	@Query("""
			SELECT la
			FROM LessonActivity la
			JOIN FETCH la.type
			WHERE la.lesson.id IN :lessonIds
			ORDER BY la.lesson.id ASC, la.createdAt ASC
			""")
	java.util.List<LessonActivity> findByLessonIdsOrderByLessonIdAscCreatedAtAsc(
			@Param("lessonIds") java.util.Collection<Long> lessonIds
	);

	@Query("SELECT la FROM LessonActivity la WHERE la.lesson.id = :lessonId AND la.id = :id")
	java.util.Optional<LessonActivity> findByLessonIdAndId(@Param("lessonId") Long lessonId, @Param("id") Long id);

	/**
	 * Counts activities per (lesson, type) for a bounded set of lessons, without loading each
	 * activity's JSONB payload or generation metadata — for card-level activity-count summaries
	 * (e.g. "2 quizzes"), not full activity content.
	 *
	 * @param lessonIds the lesson primary keys to restrict to
	 * @return one row per (lesson, activity type code) present among {@code lessonIds}
	 */
	@Query("""
			SELECT la.lesson.id AS lessonId, la.type.code AS typeCode, COUNT(la) AS activityCount
			FROM LessonActivity la
			WHERE la.lesson.id IN :lessonIds
			GROUP BY la.lesson.id, la.type.code
			""")
	java.util.List<LessonActivityTypeCountProjection> countByLessonIdsGroupedByType(
			@Param("lessonIds") java.util.Collection<Long> lessonIds
	);
}
