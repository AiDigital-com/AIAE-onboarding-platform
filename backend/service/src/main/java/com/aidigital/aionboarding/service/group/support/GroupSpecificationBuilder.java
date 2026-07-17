package com.aidigital.aionboarding.service.group.support;

import com.aidigital.aionboarding.domain.group.entities.Group;
import com.aidigital.aionboarding.domain.group.entities.GroupLead;
import com.aidigital.aionboarding.domain.group.entities.GroupLead_;
import com.aidigital.aionboarding.domain.group.entities.Group_;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.domain.user.entities.User_;
import com.aidigital.aionboarding.service.group.models.GroupListQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the JPA Criteria {@link Specification} for group list search, matching the search text
 * against the group name or a lead's name/email, and scoping results to a Team Lead viewer's own
 * groups when {@link GroupListQuery#restrictToGroupIds()} is set. Uses the
 * {@code hibernate-jpamodelgen}-generated static metamodel instead of string attribute names.
 */
@Component
public class GroupSpecificationBuilder {

	/**
	 * Builds the search specification for the given filter, sorting by group name when the query
	 * targets {@link Group} rows (skipped for the derived count query).
	 *
	 * @param filter typed filter parameters
	 * @return the JPA Criteria specification
	 */
	public Specification<Group> build(GroupListQuery filter) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();

			if (filter.restrictToGroupIds() != null) {
				predicates.add(root.get(Group_.id).in(filter.restrictToGroupIds()));
			}

			if (filter.searchText() != null && !filter.searchText().isBlank()) {
				String pattern = "%" + filter.searchText().toLowerCase() + "%";
				predicates.add(cb.or(
						cb.like(cb.lower(root.get(Group_.name)), pattern),
						leadNameOrEmailMatches(query, cb, root, pattern)
				));
			}

			if (Group.class.equals(query.getResultType())) {
				query.orderBy(cb.asc(cb.lower(root.get(Group_.name))));
			}

			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}

	/**
	 * Builds an EXISTS predicate matching a {@link GroupLead} whose lead user's name or email
	 * contains the search pattern, correlated to the outer group.
	 */
	Predicate leadNameOrEmailMatches(CriteriaQuery<?> query, CriteriaBuilder cb, Root<Group> root, String pattern) {
		Subquery<Long> subquery = query.subquery(Long.class);
		Root<Group> correlatedGroup = subquery.correlate(root);
		Root<GroupLead> groupLead = subquery.from(GroupLead.class);
		Join<GroupLead, User> lead = groupLead.join(GroupLead_.leadUser);
		subquery.select(cb.literal(1L));
		subquery.where(
				cb.equal(groupLead.get(GroupLead_.group), correlatedGroup),
				cb.or(
						cb.like(cb.lower(lead.get(User_.name)), pattern),
						cb.like(cb.lower(lead.get(User_.email)), pattern)
				)
		);
		return cb.exists(subquery);
	}
}
