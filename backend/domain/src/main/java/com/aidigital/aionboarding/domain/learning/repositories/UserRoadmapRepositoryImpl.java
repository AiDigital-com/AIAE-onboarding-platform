package com.aidigital.aionboarding.domain.learning.repositories;

import com.aidigital.aionboarding.domain.common.dictionary.DictionaryEntity_;
import com.aidigital.aionboarding.domain.common.dictionary.LessonPublicationStatusCode;
import com.aidigital.aionboarding.domain.common.dictionary.LessonStatusCode;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonPublicationStatus;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonStatus;
import com.aidigital.aionboarding.domain.learning.entities.UserLesson;
import com.aidigital.aionboarding.domain.learning.entities.UserLesson_;
import com.aidigital.aionboarding.domain.learning.entities.UserRoadmap;
import com.aidigital.aionboarding.domain.learning.entities.UserRoadmap_;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
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
     * "Fully completed" is expressed as {@code NOT EXISTS (a learnable lesson in the roadmap
     * that has no completed UserLesson row for this user)} — a double negative that maps
     * directly onto two nested Criteria subqueries, equivalent to the more common
     * {@code LEFT JOIN ... WHERE x IS NULL} anti-join idiom but without needing an unmapped
     * (theta) join between {@code RoadmapLesson} and {@code UserLesson}, which have no direct
     * entity association.
     * <p>
     * Only <b>learnable</b> lessons ({@code ready} and either {@code published} or
     * {@code private} — the same rule as
     * {@code LessonLearnabilityPolicy.isLearnable}/{@code LearningEnrollmentService.isLearnable}
     * in the {@code service} module) count toward completion. This deliberately mirrors
     * {@code RoadmapEnrollmentServiceImpl.fanOutRoadmapLessons}, which only ever grants a
     * {@code UserLesson} row for a learnable lesson: an archived or still-generating
     * {@code RoadmapLesson} never receives a {@code UserLesson} row for anyone, so requiring one
     * here would make the roadmap permanently uncompletable. See {@link #incompleteLessonSubquery}.
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
     * Builds an EXISTS subquery matching any <b>learnable</b> {@link RoadmapLesson} in the
     * outer roadmap for which no completed {@link UserLesson} row exists for the given user —
     * i.e. the roadmap is not yet fully completed.
     * <p>
     * The learnable filter ({@link #learnableLessonPredicate}) is what keeps this in sync with
     * {@code RoadmapEnrollmentServiceImpl.fanOutRoadmapLessons}: fan-out only ever creates a
     * {@code UserLesson} row for a learnable lesson, so an archived or still-generating
     * {@code RoadmapLesson} row is excluded here rather than demanded — otherwise a roadmap
     * lesson archived (or reset to generating by an AI revision) after assignment would leave
     * this EXISTS permanently {@code true} and the roadmap permanently incomplete, even once
     * every lesson the learner was actually given has been completed.
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
            learnableLessonPredicate(cb, roadmapLesson),
            cb.not(cb.exists(lessonCompletedSubquery(cb, subquery, roadmapLesson, userId)))
        );
        return subquery;
    }

    /**
     * Builds the predicate matching a {@link RoadmapLesson} whose {@link Lesson} is learnable —
     * {@code ready} and either {@code published} (Public) or {@code private} (assigned-only).
     * <p>
     * This is the Criteria-API/SQL equivalent of
     * {@code LessonLearnabilityPolicy.isLearnable}/{@code LearningEnrollmentService.isLearnable}
     * in the {@code service} module, expressed here instead of delegated to that collaborator
     * because this class must stay pure JPA Criteria SQL (per
     * {@code .claude/rules/12-database.md}/{@code 14-performance.md} — no in-memory filtering of
     * a growing {@code RoadmapLesson} set) and {@code domain} cannot depend on {@code service}.
     * The two implementations must stay semantically aligned; a change to one rule is not
     * complete without the matching change here.
     */
    Predicate learnableLessonPredicate(CriteriaBuilder cb, Root<RoadmapLesson> roadmapLesson) {
        Join<RoadmapLesson, Lesson> lesson = roadmapLesson.join(RoadmapLesson_.lesson);
        Join<Lesson, LessonStatus> status = lesson.join(Lesson_.status);
        Join<Lesson, LessonPublicationStatus> publicationStatus = lesson.join(Lesson_.publicationStatus);
        return cb.and(
            cb.equal(status.get(DictionaryEntity_.code), LessonStatusCode.READY),
            cb.or(
                cb.equal(publicationStatus.get(DictionaryEntity_.code), LessonPublicationStatusCode.PUBLISHED),
                cb.equal(publicationStatus.get(DictionaryEntity_.code), LessonPublicationStatusCode.PRIVATE)
            )
        );
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
