package com.aidigital.aionboarding.domain.lesson.repositories;

import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

/**
 * Criteria-API-backed queries for {@code LessonRepository} that a derived or JPQL {@code @Query}
 * method cannot express.
 */
public interface LessonRepositoryCustom {

	/**
	 * Searches lessons matching the given specification, projecting only the bounded summary
	 * fields a Library search card needs instead of the full entity.
	 *
	 * @param specification dynamic filter, visibility, and sort predicate, built the same way as
	 *                      the full-entity search
	 * @param pageable      zero-based page index and page size
	 * @return a page of {@link LessonSearchSummaryProjection}
	 */
	Page<LessonSearchSummaryProjection> searchSummaries(Specification<Lesson> specification, Pageable pageable);
}
