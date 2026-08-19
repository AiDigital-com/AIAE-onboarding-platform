package com.aidigital.aionboarding.service.roadmap.support;

import com.aidigital.aionboarding.domain.learning.entities.UserRoadmap;
import com.aidigital.aionboarding.domain.learning.entities.UserRoadmap_;
import com.aidigital.aionboarding.domain.roadmap.entities.Roadmap;
import com.aidigital.aionboarding.domain.roadmap.entities.Roadmap_;
import com.aidigital.aionboarding.domain.team.entities.TeamMember;
import com.aidigital.aionboarding.domain.team.entities.TeamMember_;
import com.aidigital.aionboarding.domain.user.entities.User_;
import com.aidigital.aionboarding.service.common.mapping.TagsFilterSupport;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapListQuery;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapVisibilityFilter;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
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
 * Builds the JPA Criteria {@link Specification} for roadmap list search, translating typed
 * filters and the visibility rule into predicates and the whitelisted sort field into an explicit
 * {@link Order}. Uses the {@code hibernate-jpamodelgen}-generated static metamodel
 * ({@code Roadmap_}, ...) instead of string attribute names.
 */
@Component
@RequiredArgsConstructor
public class RoadmapSpecificationBuilder {

	private final TagsFilterSupport tagsFilterSupport;

	/**
	 * Builds the search specification for the given filter and visibility rule, including sorting
	 * when the query targets {@link Roadmap} rows (skipped for the derived count query).
	 *
	 * @param filter     typed filter and sort parameters
	 * @param visibility security context resolved once per request
	 * @return the JPA Criteria specification
	 */
	public Specification<Roadmap> build(RoadmapListQuery filter, RoadmapVisibilityFilter visibility) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();

			if (filter.searchText() != null && !filter.searchText().isBlank()) {
				String pattern = "%" + filter.searchText().toLowerCase() + "%";
				predicates.add(cb.or(
						cb.like(cb.lower(root.get(Roadmap_.title)), pattern),
						cb.like(cb.lower(root.get(Roadmap_.description)), pattern),
						cb.like(cb.lower(root.get(Roadmap_.createdBy)), pattern),
						// Case-insensitive substring match on any tag (same helper materials use).
						cb.isTrue(tagsContain(cb, root.get(Roadmap_.tags), filter.searchText()))
				));
			}

			if (filter.tags() != null && !filter.tags().isEmpty()) {
				String containmentJson = tagsFilterSupport.toContainmentJson(filter.tags());
				predicates.add(cb.isTrue(jsonbContains(cb, root.get(Roadmap_.tags), containmentJson)));
			}

			if (filter.createdByUserId() != null) {
				predicates.add(cb.equal(root.get(Roadmap_.authorUser).get(User_.id), filter.createdByUserId()));
			}

			if (Boolean.TRUE.equals(filter.assignedToMe())) {
				predicates.add(enrolledByViewer(query, cb, root, visibility.viewerUserId()));
			}

			predicates.add(visibilityPredicate(query, cb, root, visibility));

			if (Roadmap.class.equals(query.getResultType())) {
				query.orderBy(buildOrder(cb, root, filter));
			}

			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}

	/**
	 * Mirrors {@code RoadmapAccessPolicy.getManageableRoadmapIds} for the manage branch and must
	 * stay aligned with it: an admin sees every roadmap; everyone else sees roadmaps they hold an
	 * existing enrollment for (direct assignment or group fan-out, both recorded as a
	 * {@code UserRoadmap} row); a viewer who may manage roadmaps additionally sees roadmaps they
	 * authored; and a team lead who may manage roadmaps additionally sees roadmaps authored by one
	 * of their own team's members. {@code Roadmap.authorUser} is nullable, so the owned-by-viewer
	 * branch explicitly guards against a {@code NULL} author instead of relying on
	 * {@code NULL = value} evaluating to unknown, keeping the branch's intent explicit in the
	 * generated SQL.
	 */
	Predicate visibilityPredicate(CriteriaQuery<?> query, CriteriaBuilder cb, Root<Roadmap> root,
									RoadmapVisibilityFilter visibility) {
		if (visibility.admin()) {
			return cb.conjunction();
		}
		Predicate base = enrolledByViewer(query, cb, root, visibility.viewerUserId());
		if (!visibility.canManageRoadmaps()) {
			return base;
		}
		Predicate ownedByViewer = cb.and(
				cb.isNotNull(root.get(Roadmap_.authorUser)),
				cb.equal(root.get(Roadmap_.authorUser).get(User_.id), visibility.viewerUserId())
		);
		Predicate withOwned = cb.or(base, ownedByViewer);
		if (!visibility.teamLead()) {
			return withOwned;
		}
		return cb.or(withOwned, authoredByOwnTeamMember(query, cb, root, visibility.viewerUserId()));
	}

	/**
	 * Builds an EXISTS predicate matching a {@link TeamMember} row whose lead is the viewer and
	 * whose member is the outer roadmap's author, so a team lead additionally sees roadmaps
	 * authored by one of their own team's members.
	 */
	Predicate authoredByOwnTeamMember(CriteriaQuery<?> query, CriteriaBuilder cb, Root<Roadmap> root, Long viewerId) {
		Subquery<Long> subquery = query.subquery(Long.class);
		Root<Roadmap> correlatedRoadmap = subquery.correlate(root);
		Root<TeamMember> teamMember = subquery.from(TeamMember.class);
		subquery.select(cb.literal(1L));
		subquery.where(
				cb.equal(teamMember.get(TeamMember_.leadUser).get(User_.id), viewerId),
				cb.equal(teamMember.get(TeamMember_.memberUser).get(User_.id),
						correlatedRoadmap.get(Roadmap_.authorUser).get(User_.id))
		);
		return cb.exists(subquery);
	}

	Order buildOrder(CriteriaBuilder cb, Root<Roadmap> root, RoadmapListQuery filter) {
		Expression<?> sortExpression = switch (filter.sortField()) {
			case CREATED_AT -> root.get(Roadmap_.createdAt);
			case UPDATED_AT -> root.get(Roadmap_.updatedAt);
			case TITLE -> root.get(Roadmap_.title);
		};
		return filter.direction() == Sort.Direction.ASC ? cb.asc(sortExpression) : cb.desc(sortExpression);
	}

	/**
	 * Builds an EXISTS predicate matching a {@link UserRoadmap} enrollment row for the viewer
	 * against the outer roadmap. Covers both direct assignment and group assignment, since
	 * {@code RoadmapGroupAssignmentSyncServiceImpl} fans a group assignment out into one
	 * {@link UserRoadmap} row per group member, so no separate group-membership join is needed
	 * here.
	 */
	Predicate enrolledByViewer(CriteriaQuery<?> query, CriteriaBuilder cb, Root<Roadmap> root, Long viewerId) {
		Subquery<Long> subquery = query.subquery(Long.class);
		Root<Roadmap> correlatedRoadmap = subquery.correlate(root);
		Root<UserRoadmap> userRoadmap = subquery.from(UserRoadmap.class);
		subquery.select(cb.literal(1L));
		subquery.where(
				cb.equal(userRoadmap.get(UserRoadmap_.roadmap), correlatedRoadmap),
				cb.equal(userRoadmap.get(UserRoadmap_.user).get(User_.id), viewerId)
		);
		return cb.exists(subquery);
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
	 * {@code jsonb_array_contains_ci}, so free-text search also finds roadmaps by tag.
	 */
	Expression<Boolean> tagsContain(CriteriaBuilder cb, Expression<?> tagsColumn, String searchText) {
		return cb.function("jsonb_array_contains_ci", Boolean.class, tagsColumn, cb.literal(searchText));
	}
}
