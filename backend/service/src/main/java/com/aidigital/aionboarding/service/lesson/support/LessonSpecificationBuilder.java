package com.aidigital.aionboarding.service.lesson.support;

import com.aidigital.aionboarding.domain.common.dictionary.entities.ActivityType;
import com.aidigital.aionboarding.domain.common.dictionary.entities.ActivityType_;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonPublicationStatus_;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonStatus_;
import com.aidigital.aionboarding.domain.learning.entities.UserLesson;
import com.aidigital.aionboarding.domain.learning.entities.UserLesson_;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson_;
import com.aidigital.aionboarding.domain.lessonactivity.entities.LessonActivity;
import com.aidigital.aionboarding.domain.lessonactivity.entities.LessonActivity_;
import com.aidigital.aionboarding.domain.user.entities.User_;
import com.aidigital.aionboarding.service.common.mapping.TagsFilterSupport;
import com.aidigital.aionboarding.service.lesson.models.LessonListQuery;
import com.aidigital.aionboarding.service.lesson.models.LessonVisibilityFilter;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the JPA Criteria {@link Specification} for lesson list search, translating typed filters
 * and the visibility rule into predicates, eagerly fetching dictionary associations needed by the
 * summary mapper, and resolving the whitelisted sort field into an explicit {@link Order}. Uses
 * the {@code hibernate-jpamodelgen}-generated static metamodel ({@code Lesson_}, ...) instead of
 * string attribute names.
 */
@Component
@RequiredArgsConstructor
public class LessonSpecificationBuilder {

    private final TagsFilterSupport tagsFilterSupport;

    /**
     * Builds the search specification for the given filter and visibility rule.
     * <p>
     * Mirrors {@code LessonServiceImpl.canView}: an admin sees everything, a viewer who manages
     * lessons sees their own lessons regardless of publication state, and everyone else sees only
     * published lessons. Eagerly fetches {@code status}, {@code publicationStatus}, and
     * {@code createdByUser} when the query targets {@link Lesson} rows (skipped for the derived
     * count query), since those to-one fetches are otherwise dereferenced lazily per row by the
     * summary mapper.
     *
     * @param filter              typed filter and sort parameters
     * @param visibility          security context resolved once per request
     * @param statusId            resolved id for {@code filter.statusCode()}, or {@code null}
     * @param publicationStatusId resolved id for {@code filter.publicationStatusCode()}, or {@code null}
     * @param readyStatusId       resolved id for the "ready" lesson status
     * @param publishedStatusId   resolved id for the "published" publication status
     * @return the JPA Criteria specification
     */
    public Specification<Lesson> build(
        LessonListQuery filter,
        LessonVisibilityFilter visibility,
        Long statusId,
        Long publicationStatusId,
        Long readyStatusId,
        Long publishedStatusId
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (Lesson.class.equals(query.getResultType())) {
                root.fetch(Lesson_.status);
                root.fetch(Lesson_.publicationStatus);
                root.fetch(Lesson_.createdByUser, JoinType.LEFT);
            }

            if (filter.searchText() != null && !filter.searchText().isBlank()) {
                String pattern = "%" + filter.searchText().toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get(Lesson_.title)), pattern),
                    // Case-insensitive substring match on any tag (same helper materials/roadmaps use).
                    cb.isTrue(tagsContain(cb, root.get(Lesson_.tags), filter.searchText()))
                ));
            }

            if (filter.tags() != null && !filter.tags().isEmpty()) {
                String containmentJson = tagsFilterSupport.toContainmentJson(filter.tags());
                predicates.add(cb.isTrue(jsonbContains(cb, root.get(Lesson_.tags), containmentJson)));
            }

            if (statusId != null) {
                predicates.add(cb.equal(root.get(Lesson_.status).get(LessonStatus_.id), statusId));
            }
            if (publicationStatusId != null) {
                predicates.add(cb.equal(root.get(Lesson_.publicationStatus).get(LessonPublicationStatus_.id), publicationStatusId));
            }
            if (filter.createdByUserId() != null) {
                predicates.add(cb.equal(root.get(Lesson_.createdByUser).get(User_.id), filter.createdByUserId()));
            }
            if (Boolean.TRUE.equals(filter.readyOnly())) {
                predicates.add(cb.equal(root.get(Lesson_.status).get(LessonStatus_.id), readyStatusId));
            }
            if (Boolean.TRUE.equals(filter.assignedToMe())) {
                predicates.add(enrolledByViewer(query, cb, root, visibility.viewerUserId()));
            }
            if (filter.activityTypeCode() != null) {
                predicates.add(hasActivityOfType(query, cb, root, filter.activityTypeCode()));
            }
            if (filter.hasActivities() != null) {
                predicates.add(hasAnyActivity(query, cb, root, filter.hasActivities()));
            }

            predicates.add(visibilityPredicate(cb, root, visibility, publishedStatusId));

            // Applies to every result shape except the derived COUNT(Long) query — including the
            // lean summary projection, which (unlike the full-entity query above) is never fetch-joined.
            if (!Long.class.equals(query.getResultType())) {
                query.orderBy(buildOrder(cb, root, filter));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    Order buildOrder(CriteriaBuilder cb, Root<Lesson> root, LessonListQuery filter) {
        Expression<?> sortExpression = switch (filter.sortField()) {
            case CREATED_AT -> root.get(Lesson_.createdAt);
            case UPDATED_AT -> root.get(Lesson_.updatedAt);
            case TITLE -> root.get(Lesson_.title);
        };
        return filter.direction() == Sort.Direction.ASC ? cb.asc(sortExpression) : cb.desc(sortExpression);
    }

    /**
     * Mirrors {@code LessonServiceImpl.canView}: admin sees all, a manage-holder sees their own
     * lessons regardless of publication state, everyone else sees only published lessons.
     */
    Predicate visibilityPredicate(
        CriteriaBuilder cb,
        Root<Lesson> root,
        LessonVisibilityFilter visibility,
        Long publishedStatusId
    ) {
        if (visibility.admin()) {
            return cb.conjunction();
        }
        Predicate published = cb.equal(root.get(Lesson_.publicationStatus).get(LessonPublicationStatus_.id), publishedStatusId);
        if (!visibility.canManageOwnLessons()) {
            return published;
        }
        Predicate ownedByViewer = cb.equal(root.get(Lesson_.createdByUser).get(User_.id), visibility.viewerUserId());
        return cb.or(ownedByViewer, published);
    }

    Predicate enrolledByViewer(CriteriaQuery<?> query, CriteriaBuilder cb, Root<Lesson> root, Long viewerId) {
        Subquery<Long> subquery = query.subquery(Long.class);
        Root<Lesson> correlatedLesson = subquery.correlate(root);
        Root<UserLesson> userLesson = subquery.from(UserLesson.class);
        subquery.select(cb.literal(1L));
        subquery.where(
            cb.equal(userLesson.get(UserLesson_.lesson), correlatedLesson),
            cb.equal(userLesson.get(UserLesson_.user).get(User_.id), viewerId)
        );
        return cb.exists(subquery);
    }

    Predicate hasActivityOfType(CriteriaQuery<?> query, CriteriaBuilder cb, Root<Lesson> root, String activityTypeCode) {
        Subquery<Long> subquery = query.subquery(Long.class);
        Root<Lesson> correlatedLesson = subquery.correlate(root);
        Root<LessonActivity> lessonActivity = subquery.from(LessonActivity.class);
        Join<LessonActivity, ActivityType> type = lessonActivity.join(LessonActivity_.type);
        subquery.select(cb.literal(1L));
        subquery.where(
            cb.equal(lessonActivity.get(LessonActivity_.lesson), correlatedLesson),
            cb.equal(type.get(ActivityType_.code), activityTypeCode)
        );
        return cb.exists(subquery);
    }

    Predicate hasAnyActivity(CriteriaQuery<?> query, CriteriaBuilder cb, Root<Lesson> root, boolean expected) {
        Subquery<Long> subquery = query.subquery(Long.class);
        Root<Lesson> correlatedLesson = subquery.correlate(root);
        Root<LessonActivity> lessonActivity = subquery.from(LessonActivity.class);
        subquery.select(cb.literal(1L));
        subquery.where(cb.equal(lessonActivity.get(LessonActivity_.lesson), correlatedLesson));
        Predicate exists = cb.exists(subquery);
        return expected ? exists : cb.not(exists);
    }

    /**
     * Builds a PostgreSQL JSONB containment check ({@code column @> value}) via the underlying
     * {@code jsonb_contains} function, since JPA Criteria has no native JSON containment operator.
     * The containment literal is parsed with the {@code jsonb(text)} cast function rather than
     * {@code to_jsonb(text)}, which would wrap the JSON text as a scalar string instead of parsing it.
     */
    Expression<Boolean> jsonbContains(CriteriaBuilder cb, Expression<?> jsonColumn, String containmentJson) {
        return cb.function(
            "jsonb_contains",
            Boolean.class,
            jsonColumn,
            cb.function("jsonb", Object.class, cb.literal(containmentJson))
        );
    }

    /**
     * Case-insensitive substring match against any element of a JSONB tags array via
     * {@code jsonb_array_contains_ci}, so free-text search also finds lessons by tag.
     */
    Expression<Boolean> tagsContain(CriteriaBuilder cb, Expression<?> tagsColumn, String searchText) {
        return cb.function("jsonb_array_contains_ci", Boolean.class, tagsColumn, cb.literal(searchText));
    }
}
