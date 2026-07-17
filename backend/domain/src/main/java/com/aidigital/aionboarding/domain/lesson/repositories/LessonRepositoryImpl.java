package com.aidigital.aionboarding.domain.lesson.repositories;

import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonPublicationStatus_;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonStatus_;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson_;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/**
 * Criteria-API implementation of {@link LessonRepositoryCustom}. Named {@code Impl} so Spring
 * Data JPA composes it into {@link LessonRepository} automatically.
 */
@RequiredArgsConstructor
public class LessonRepositoryImpl implements LessonRepositoryCustom {

    private static final int PREVIEW_LENGTH = 500;

    private final EntityManager entityManager;

    /**
     * Reuses the caller-built {@link Specification} (same filter, visibility, and sort predicate
     * as the full-entity search) against a projection query, so the bounded summary and full
     * detail paths never drift in what they consider a match.
     */
    @Override
    public Page<LessonSearchSummaryProjection> searchSummaries(Specification<Lesson> specification, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<LessonSearchSummaryProjection> query = cb.createQuery(LessonSearchSummaryProjection.class);
        Root<Lesson> root = query.from(Lesson.class);
        Predicate predicate = specification.toPredicate(root, query, cb);
        query.select(cb.construct(
            LessonSearchSummaryProjection.class,
            root.get(Lesson_.id),
            root.get(Lesson_.title),
            root.get(Lesson_.status).get(LessonStatus_.code),
            root.get(Lesson_.publicationStatus).get(LessonPublicationStatus_.code),
            cb.substring(root.get(Lesson_.contentHtml), 1, PREVIEW_LENGTH),
            cb.substring(root.get(Lesson_.contentMarkdown), 1, PREVIEW_LENGTH),
            root.get(Lesson_.coverImageStorageKey),
            root.get(Lesson_.coverImageOriginalName),
            root.get(Lesson_.coverImageMimeType),
            root.get(Lesson_.tags),
            root.get(Lesson_.createdBy),
            root.get(Lesson_.createdAt),
            root.get(Lesson_.updatedAt)
        )).where(predicate);

        List<LessonSearchSummaryProjection> content = entityManager.createQuery(query)
            .setFirstResult((int) pageable.getOffset())
            .setMaxResults(pageable.getPageSize())
            .getResultList();

        long total = countMatching(specification, cb);
        return new PageImpl<>(content, pageable, total);
    }

    long countMatching(Specification<Lesson> specification, CriteriaBuilder cb) {
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Lesson> countRoot = countQuery.from(Lesson.class);
        Predicate countPredicate = specification.toPredicate(countRoot, countQuery, cb);
        countQuery.select(cb.count(countRoot)).where(countPredicate);
        return entityManager.createQuery(countQuery).getSingleResult();
    }
}
