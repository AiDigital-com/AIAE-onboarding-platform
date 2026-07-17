package com.aidigital.aionboarding.domain.learning.repositories;

import com.aidigital.aionboarding.domain.learning.entities.UserLesson;
import com.aidigital.aionboarding.domain.learning.entities.UserLesson_;
import com.aidigital.aionboarding.domain.learning.entities.UserRoadmap;
import com.aidigital.aionboarding.domain.learning.entities.UserRoadmap_;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson_;
import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapLesson;
import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapLesson_;
import com.aidigital.aionboarding.domain.roadmap.entities.Roadmap_;
import com.aidigital.aionboarding.domain.user.entities.User_;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;

import java.util.Collection;

/**
 * Criteria-API implementation of {@link UserLessonRepositoryCustom}. Named {@code Impl} so
 * Spring Data JPA composes it into {@link UserLessonRepository} automatically.
 */
@RequiredArgsConstructor
public class UserLessonRepositoryImpl implements UserLessonRepositoryCustom {

	private final EntityManager entityManager;

	/**
	 * Deletes a user-lesson enrollment row only when no other roadmap the same user remains
	 * enrolled in also grants that lesson, so a lesson kept alive by a second roadmap survives
	 * one roadmap's revocation. One set-based {@link CriteriaDelete} statement regardless of
	 * user or lesson count.
	 *
	 * @param userIds   users whose roadmap-derived lesson enrollments are being revoked
	 * @param roadmapId roadmap being revoked
	 * @return number of deleted lesson-enrollment rows
	 */
	@Override
	public int deleteRoadmapDerivedLessonEnrollments(Collection<Long> userIds, Long roadmapId) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaDelete<UserLesson> delete = cb.createCriteriaDelete(UserLesson.class);
		Root<UserLesson> userLesson = delete.from(UserLesson.class);

		Subquery<Long> roadmapLessonIds = delete.subquery(Long.class);
		Root<RoadmapLesson> roadmapLesson = roadmapLessonIds.from(RoadmapLesson.class);
		roadmapLessonIds.select(roadmapLesson.get(RoadmapLesson_.lesson).get(Lesson_.id));
		roadmapLessonIds.where(cb.equal(roadmapLesson.get(RoadmapLesson_.roadmap).get(Roadmap_.id), roadmapId));

		delete.where(
				userLesson.get(UserLesson_.user).get(User_.id).in(userIds),
				userLesson.get(UserLesson_.lesson).get(Lesson_.id).in(roadmapLessonIds),
				cb.not(cb.exists(otherRoadmapStillGrantsLessonSubquery(cb, delete, userLesson, roadmapId)))
		);

		int deletedCount = entityManager.createQuery(delete).executeUpdate();
		// Bulk DML bypasses the persistence context, so any already-loaded UserLesson rows must
		// be cleared to avoid a caller re-reading now-stale entities (mirrors the Spring Data
		// @Modifying(clearAutomatically = true) behavior this method previously relied on).
		entityManager.clear();
		return deletedCount;
	}

	/**
	 * Builds an EXISTS subquery matching another roadmap enrollment (not {@code roadmapId}) of
	 * the outer row's user whose lesson list still contains the outer row's lesson.
	 */
	Subquery<Long> otherRoadmapStillGrantsLessonSubquery(
			CriteriaBuilder cb, CriteriaDelete<UserLesson> delete, Root<UserLesson> userLessonRoot, Long roadmapId
	) {
		Subquery<Long> subquery = delete.subquery(Long.class);
		Root<UserLesson> correlatedUserLesson = subquery.correlate(userLessonRoot);
		Root<UserRoadmap> otherUserRoadmap = subquery.from(UserRoadmap.class);
		Root<RoadmapLesson> otherRoadmapLesson = subquery.from(RoadmapLesson.class);
		subquery.select(cb.literal(1L));
		subquery.where(
				cb.equal(otherUserRoadmap.get(UserRoadmap_.user), correlatedUserLesson.get(UserLesson_.user)),
				cb.notEqual(otherUserRoadmap.get(UserRoadmap_.roadmap).get(Roadmap_.id), roadmapId),
				cb.equal(otherRoadmapLesson.get(RoadmapLesson_.roadmap), otherUserRoadmap.get(UserRoadmap_.roadmap)),
				cb.equal(otherRoadmapLesson.get(RoadmapLesson_.lesson), correlatedUserLesson.get(UserLesson_.lesson))
		);
		return subquery;
	}
}
