package com.aidigital.aionboarding.service.material.support;

import com.aidigital.aionboarding.domain.lesson.entities.LessonMaterial;
import com.aidigital.aionboarding.domain.lesson.entities.LessonMaterial_;
import com.aidigital.aionboarding.domain.material.entities.Material;
import com.aidigital.aionboarding.domain.material.entities.MaterialFile;
import com.aidigital.aionboarding.domain.material.entities.MaterialFile_;
import com.aidigital.aionboarding.domain.material.entities.MaterialLink;
import com.aidigital.aionboarding.domain.material.entities.MaterialLink_;
import com.aidigital.aionboarding.domain.material.entities.MaterialYoutubeUrl;
import com.aidigital.aionboarding.domain.material.entities.MaterialYoutubeUrl_;
import com.aidigital.aionboarding.domain.material.entities.Material_;
import com.aidigital.aionboarding.domain.user.entities.User_;
import com.aidigital.aionboarding.service.common.mapping.TagsFilterSupport;
import com.aidigital.aionboarding.service.material.models.MaterialListQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.metamodel.SingularAttribute;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the JPA Criteria {@link Specification} for material list search, translating typed
 * filters into predicates and the whitelisted sort field into an explicit {@link Order}. Uses the
 * {@code hibernate-jpamodelgen}-generated static metamodel ({@code Material_}, ...) instead of
 * string attribute names.
 */
@Component
@RequiredArgsConstructor
public class MaterialSpecificationBuilder {

	private final TagsFilterSupport tagsFilterSupport;

	/**
	 * Builds the search specification for the given filter, including sorting when the query
	 * targets {@link Material} rows (skipped for the derived count query).
	 *
	 * @param filter typed filter and sort parameters
	 * @return the JPA Criteria specification
	 */
	public Specification<Material> build(MaterialListQuery filter) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();

			if (filter.searchText() != null && !filter.searchText().isBlank()) {
				// Deliberately excludes text_content: EXPLAIN evidence at 12k
				// materials showed a rare/absent-term or count search scans the whole table
				// (LIMIT can't short-circuit a low-selectivity match), and the text_content branch
				// made that scan ~10x more expensive per row than lessons/roadmaps' equivalent
				// title-only predicate (no large-text column). Title/description/createdBy/tags
				// remain fully searched; only a term appearing solely in a material's body text
				// (and nowhere in its title, description, tags, or creator) won't surface it.
				String pattern = "%" + filter.searchText().toLowerCase() + "%";
				predicates.add(cb.or(
						cb.like(cb.lower(root.get(Material_.title)), pattern),
						cb.like(cb.lower(root.get(Material_.description)), pattern),
						cb.like(cb.lower(root.get(Material_.createdBy)), pattern),
						cb.isTrue(tagsContain(cb, root.get(Material_.tags), filter.searchText()))
				));
			}

			if (filter.tags() != null && !filter.tags().isEmpty()) {
				String containmentJson = tagsFilterSupport.toContainmentJson(filter.tags());
				predicates.add(cb.isTrue(jsonbContains(cb, root.get(Material_.tags), containmentJson)));
			}

			if (filter.createdByUserId() != null) {
				predicates.add(cb.equal(root.get(Material_.createdByUser).get(User_.id), filter.createdByUserId()));
			}

			if (filter.hasAttachments() != null) {
				predicates.add(existsPredicate(
						query, cb, root, MaterialFile.class, MaterialFile_.material, filter.hasAttachments()
				));
			}
			if (filter.hasYoutube() != null) {
				predicates.add(existsPredicate(
						query, cb, root, MaterialYoutubeUrl.class, MaterialYoutubeUrl_.material, filter.hasYoutube()
				));
			}
			if (filter.hasLinks() != null) {
				predicates.add(existsPredicate(
						query, cb, root, MaterialLink.class, MaterialLink_.material, filter.hasLinks()
				));
			}

			// Applies to every result shape except the derived COUNT(Long) query — including the
			// lean summary projection.
			if (!Long.class.equals(query.getResultType())) {
				query.orderBy(buildOrder(query, cb, root, filter));
			}

			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}

	/**
	 * Resolves the whitelisted sort field into an explicit {@link Order}, using a correlated
	 * count subquery for {@code usageCount} since it is not a persisted column.
	 */
	Order buildOrder(CriteriaQuery<?> query, CriteriaBuilder cb, Root<Material> root, MaterialListQuery filter) {
		Expression<?> sortExpression = switch (filter.sortField()) {
			case CREATED_AT -> root.get(Material_.createdAt);
			case UPDATED_AT -> root.get(Material_.updatedAt);
			case TITLE -> root.get(Material_.title);
			case USAGE_COUNT -> usageCountSubquery(query, cb, root);
		};
		return filter.direction() == Sort.Direction.ASC ? cb.asc(sortExpression) : cb.desc(sortExpression);
	}

	/**
	 * Builds a correlated subquery counting {@link LessonMaterial} rows referencing the outer
	 * material, used to sort by lesson usage count.
	 */
	Subquery<Long> usageCountSubquery(CriteriaQuery<?> query, CriteriaBuilder cb, Root<Material> root) {
		Subquery<Long> subquery = query.subquery(Long.class);
		Root<Material> correlatedMaterial = subquery.correlate(root);
		Root<LessonMaterial> lessonMaterial = subquery.from(LessonMaterial.class);
		subquery.select(cb.count(lessonMaterial));
		subquery.where(cb.equal(lessonMaterial.get(LessonMaterial_.material), correlatedMaterial));
		return subquery;
	}

	/**
	 * Builds an EXISTS/NOT EXISTS predicate for a child entity referencing the outer material via
	 * the given {@code material} association, used by the has* filters.
	 */
	<T> Predicate existsPredicate(
			CriteriaQuery<?> query,
			CriteriaBuilder cb,
			Root<Material> root,
			Class<T> childEntity,
			SingularAttribute<T, Material> materialAttribute,
			boolean expected
	) {
		Subquery<Long> subquery = query.subquery(Long.class);
		Root<Material> correlatedMaterial = subquery.correlate(root);
		Root<T> child = subquery.from(childEntity);
		subquery.select(cb.literal(1L));
		subquery.where(cb.equal(child.get(materialAttribute), correlatedMaterial));
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
	 * Builds a case-insensitive substring match against every element of a JSONB tags array via
	 * the {@code jsonb_array_contains_ci} database function, so free-text search also matches tags.
	 */
	Expression<Boolean> tagsContain(CriteriaBuilder cb, Expression<?> tagsColumn, String searchText) {
		return cb.function("jsonb_array_contains_ci", Boolean.class, tagsColumn, cb.literal(searchText));
	}
}
