package com.aidigital.aionboarding.domain.learning.repositories;

import com.aidigital.aionboarding.domain.learning.entities.UserLesson;
import com.aidigital.aionboarding.domain.learning.entities.UserLesson_;
import com.aidigital.aionboarding.domain.learning.entities.UserRoadmap;
import com.aidigital.aionboarding.domain.learning.entities.UserRoadmap_;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson_;
import com.aidigital.aionboarding.domain.roadmap.entities.Roadmap;
import com.aidigital.aionboarding.domain.roadmap.entities.Roadmap_;
import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapLesson;
import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapLesson_;
import com.aidigital.aionboarding.domain.user.entities.User_;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Criteria-API implementation of {@link UserRoadmapRepositoryCustom}. Named {@code Impl} so
 * Spring Data JPA composes it into {@link UserRoadmapRepository} automatically.
 */
@RequiredArgsConstructor
public class UserRoadmapRepositoryImpl implements UserRoadmapRepositoryCustom {

    private final EntityManager entityManager;

    /**
     * Finds fully-completed roadmaps containing a given lesson for a user.
     * <p>
     * "Fully completed" is expressed as {@code NOT EXISTS (a lesson in the roadmap that has no
     * completed UserLesson row for this user)} — a double negative that maps directly onto two
     * nested Criteria subqueries, equivalent to the more common {@code LEFT JOIN ... WHERE x IS
     * NULL} anti-join idiom but without needing an unmapped (theta) join between
     * {@code RoadmapLesson} and {@code UserLesson}, which have no direct entity association.
     *
     * @param userId   the user primary key
     * @param lessonId the lesson primary key that just changed completion state
     * @return completed roadmaps containing the lesson, ordered by title
     */
    @Override
    public List<CompletedRoadmapProjection> findCompletedRoadmapsForUserLesson(Long userId, Long lessonId) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<CompletedRoadmapRow> query = cb.createQuery(CompletedRoadmapRow.class);
        Root<UserRoadmap> userRoadmap = query.from(UserRoadmap.class);
        Join<UserRoadmap, Roadmap> roadmap = userRoadmap.join(UserRoadmap_.roadmap);

        query.select(cb.construct(CompletedRoadmapRow.class, roadmap.get(Roadmap_.id), roadmap.get(Roadmap_.title)));
        query.where(
            cb.equal(userRoadmap.get(UserRoadmap_.user).get(User_.id), userId),
            cb.exists(roadmapContainsLessonSubquery(cb, query, roadmap, lessonId)),
            cb.not(cb.exists(incompleteLessonSubquery(cb, query, roadmap, userId)))
        );
        query.orderBy(cb.asc(cb.lower(roadmap.get(Roadmap_.title))));

        return entityManager.createQuery(query).getResultList().stream()
            .map(CompletedRoadmapProjection.class::cast)
            .toList();
    }

    /**
     * Builds an EXISTS subquery matching a {@link RoadmapLesson} row proving the given lesson
     * belongs to the outer roadmap.
     */
    Subquery<Long> roadmapContainsLessonSubquery(
        CriteriaBuilder cb, CriteriaQuery<?> query, Join<UserRoadmap, Roadmap> roadmapJoin, Long lessonId
    ) {
        Subquery<Long> subquery = query.subquery(Long.class);
        Join<UserRoadmap, Roadmap> correlatedRoadmap = subquery.correlate(roadmapJoin);
        Root<RoadmapLesson> containsLesson = subquery.from(RoadmapLesson.class);
        subquery.select(cb.literal(1L));
        subquery.where(
            cb.equal(containsLesson.get(RoadmapLesson_.roadmap), correlatedRoadmap),
            cb.equal(containsLesson.get(RoadmapLesson_.lesson).get(Lesson_.id), lessonId)
        );
        return subquery;
    }

    /**
     * Builds an EXISTS subquery matching any {@link RoadmapLesson} in the outer roadmap for
     * which no completed {@link UserLesson} row exists for the given user — i.e. the roadmap is
     * not yet fully completed.
     */
    Subquery<Long> incompleteLessonSubquery(
        CriteriaBuilder cb, CriteriaQuery<?> query, Join<UserRoadmap, Roadmap> roadmapJoin, Long userId
    ) {
        Subquery<Long> subquery = query.subquery(Long.class);
        Join<UserRoadmap, Roadmap> correlatedRoadmap = subquery.correlate(roadmapJoin);
        Root<RoadmapLesson> roadmapLesson = subquery.from(RoadmapLesson.class);
        subquery.select(cb.literal(1L));
        subquery.where(
            cb.equal(roadmapLesson.get(RoadmapLesson_.roadmap), correlatedRoadmap),
            cb.not(cb.exists(lessonCompletedSubquery(cb, subquery, roadmapLesson, userId)))
        );
        return subquery;
    }

    /**
     * Builds an EXISTS subquery matching a completed {@link UserLesson} row for the given user
     * and the outer {@link RoadmapLesson}'s lesson.
     */
    Subquery<Long> lessonCompletedSubquery(
        CriteriaBuilder cb, Subquery<?> parent, Root<RoadmapLesson> roadmapLessonRoot, Long userId
    ) {
        Subquery<Long> subquery = parent.subquery(Long.class);
        Root<RoadmapLesson> correlatedRoadmapLesson = subquery.correlate(roadmapLessonRoot);
        Root<UserLesson> userLesson = subquery.from(UserLesson.class);
        subquery.select(cb.literal(1L));
        List<Predicate> predicates = List.of(
            cb.equal(userLesson.get(UserLesson_.user).get(User_.id), userId),
            cb.equal(userLesson.get(UserLesson_.lesson), correlatedRoadmapLesson.get(RoadmapLesson_.lesson)),
            cb.isNotNull(userLesson.get(UserLesson_.completedAt))
        );
        subquery.where(predicates.toArray(new Predicate[0]));
        return subquery;
    }
}
