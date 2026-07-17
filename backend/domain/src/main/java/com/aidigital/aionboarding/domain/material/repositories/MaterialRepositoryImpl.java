package com.aidigital.aionboarding.domain.material.repositories;

import com.aidigital.aionboarding.domain.material.entities.Material;
import com.aidigital.aionboarding.domain.material.entities.Material_;
import com.aidigital.aionboarding.domain.user.entities.User_;
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
 * Criteria-API implementation of {@link MaterialRepositoryCustom}. Named {@code Impl} so Spring
 * Data JPA composes it into {@link MaterialRepository} automatically.
 */
@RequiredArgsConstructor
public class MaterialRepositoryImpl implements MaterialRepositoryCustom {

    private final EntityManager entityManager;

    /**
     * Reuses the caller-built {@link Specification} (same filter and sort predicate as the
     * full-entity search) against a projection query, so the bounded summary and full detail
     * paths never drift in what they consider a match.
     */
    @Override
    public Page<MaterialSearchSummaryProjection> searchSummaries(Specification<Material> specification, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<MaterialSearchSummaryProjection> query = cb.createQuery(MaterialSearchSummaryProjection.class);
        Root<Material> root = query.from(Material.class);
        Predicate predicate = specification.toPredicate(root, query, cb);
        query.select(cb.construct(
            MaterialSearchSummaryProjection.class,
            root.get(Material_.id),
            root.get(Material_.title),
            root.get(Material_.description),
            cb.greaterThan(cb.length(root.get(Material_.textContent)), 0),
            root.get(Material_.coverImageStorageKey),
            root.get(Material_.coverImageOriginalName),
            root.get(Material_.coverImageMimeType),
            root.get(Material_.createdByUser).get(User_.id),
            root.get(Material_.createdBy),
            root.get(Material_.tags),
            root.get(Material_.createdAt),
            root.get(Material_.updatedAt)
        )).where(predicate);

        List<MaterialSearchSummaryProjection> content = entityManager.createQuery(query)
            .setFirstResult((int) pageable.getOffset())
            .setMaxResults(pageable.getPageSize())
            .getResultList();

        long total = countMatching(specification, cb);
        return new PageImpl<>(content, pageable, total);
    }

    long countMatching(Specification<Material> specification, CriteriaBuilder cb) {
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Material> countRoot = countQuery.from(Material.class);
        Predicate countPredicate = specification.toPredicate(countRoot, countQuery, cb);
        countQuery.select(cb.count(countRoot)).where(countPredicate);
        return entityManager.createQuery(countQuery).getSingleResult();
    }
}
