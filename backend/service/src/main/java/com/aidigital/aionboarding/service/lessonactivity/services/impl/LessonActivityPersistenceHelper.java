package com.aidigital.aionboarding.service.lessonactivity.services.impl;

import com.aidigital.aionboarding.domain.lessonactivity.entities.LessonActivity;
import com.aidigital.aionboarding.domain.lessonactivity.repositories.LessonActivityRepository;
import com.aidigital.aionboarding.domain.lessonactivity.repositories.LessonActivityTypeCountProjection;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Short-transaction save delegate for {@link LessonActivity} persistence.
 *
 * <p>Separating the save step into its own bean ensures that Spring AOP intercepts
 * the {@code @Transactional} boundary correctly — avoiding the self-invocation trap
 * that would occur if {@code generateActivity} called {@code save()} on itself.
 *
 * <p>This is also the paired entity service for {@link LessonActivityRepository}: all
 * other services that require {@link LessonActivity} persistence must depend on this
 * class rather than injecting the repository directly.
 */
@Component
@RequiredArgsConstructor
public class LessonActivityPersistenceHelper {

	private final LessonActivityRepository lessonActivityRepository;

	/**
	 * Persists a lesson activity row.
	 *
	 * @param activity the lesson activity to save
	 * @return the saved {@link LessonActivity}
	 */
	@Transactional
	public LessonActivity save(LessonActivity activity) {
		return lessonActivityRepository.save(activity);
	}

	/**
	 * Deletes a lesson activity row.
	 *
	 * @param activity the lesson activity to delete
	 */
	@Transactional
	public void deleteActivity(LessonActivity activity) {
		lessonActivityRepository.delete(activity);
	}

	/**
	 * Loads a single lesson activity scoped to its owning lesson, so a caller can never
	 * retrieve an activity belonging to a different lesson by id alone.
	 *
	 * @param lessonId   the owning lesson's primary key
	 * @param activityId the activity's primary key
	 * @return the matching {@link LessonActivity}
	 * @throws AppException with {@link ErrorReason#C001} when no matching activity exists
	 */
	@Transactional(readOnly = true)
	public LessonActivity findByLessonIdAndId(Long lessonId, Long activityId) {
		return lessonActivityRepository.findByLessonIdAndId(lessonId, activityId)
				.orElseThrow(() -> new AppException(ErrorReason.C001, "Activity not found."));
	}

	/**
	 * Loads a single lesson activity scoped to its owning lesson without throwing when absent.
	 *
	 * @param lessonId   the owning lesson's primary key
	 * @param activityId the activity's primary key
	 * @return matching activity, if present
	 */
	@Transactional(readOnly = true)
	public Optional<LessonActivity> findOptionalByLessonIdAndId(Long lessonId, Long activityId) {
		return lessonActivityRepository.findByLessonIdAndId(lessonId, activityId);
	}

	/**
	 * Loads every lesson activity for a lesson, ordered by creation time ascending.
	 *
	 * @param lessonId the owning lesson's primary key
	 * @return every {@link LessonActivity} row for the given lesson, oldest first
	 */
	@Transactional(readOnly = true)
	public List<LessonActivity> findByLessonIdOrderByCreatedAtAsc(Long lessonId) {
		return lessonActivityRepository.findByLessonIdOrderByCreatedAtAsc(lessonId);
	}

	/**
	 * Loads activities for several lessons in lesson/creation order.
	 *
	 * @param lessonIds lesson IDs
	 * @return matching activities
	 */
	@Transactional(readOnly = true)
	public List<LessonActivity> findByLessonIdsOrderByCreatedAtAsc(Collection<Long> lessonIds) {
		if (lessonIds == null || lessonIds.isEmpty()) {
			return List.of();
		}
		return lessonActivityRepository.findByLessonIdsOrderByLessonIdAscCreatedAtAsc(lessonIds);
	}

	/**
	 * Counts activities per (lesson, type) for a bounded set of lessons, without loading each
	 * activity's JSONB payload or generation metadata.
	 *
	 * @param lessonIds lesson IDs to restrict to
	 * @return one row per (lesson, activity type code) present among {@code lessonIds}
	 */
	@Transactional(readOnly = true)
	public List<LessonActivityTypeCountProjection> countByLessonIdsGroupedByType(Collection<Long> lessonIds) {
		if (lessonIds == null || lessonIds.isEmpty()) {
			return List.of();
		}
		return lessonActivityRepository.countByLessonIdsGroupedByType(lessonIds);
	}
}
