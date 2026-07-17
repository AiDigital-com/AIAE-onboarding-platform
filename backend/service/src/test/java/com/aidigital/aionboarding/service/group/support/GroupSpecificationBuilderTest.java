package com.aidigital.aionboarding.service.group.support;

import com.aidigital.aionboarding.domain.group.entities.Group;
import com.aidigital.aionboarding.domain.group.entities.GroupLead;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.service.group.models.GroupListQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.metamodel.SingularAttribute;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GroupSpecificationBuilderTest {

	@Test
	void shouldApplyRestrictionAndSearchAndOrderForGroupQueryTest() {
		// Given:
		GroupSpecificationBuilder builder = new GroupSpecificationBuilder();
		GroupListQuery filter = new GroupListQuery("Search", Set.of(1L, 2L));
		Root<Group> root = mock(Root.class);
		@SuppressWarnings("unchecked")
		CriteriaQuery<Group> query = mock(CriteriaQuery.class);
		CriteriaBuilder cb = mock(CriteriaBuilder.class);
		@SuppressWarnings("unchecked")
		Path<Object> path = mock(Path.class);
		@SuppressWarnings("unchecked")
		Path<Object> leadPath = mock(Path.class);
		@SuppressWarnings("unchecked")
		Expression<String> lowerExpr = mock(Expression.class);
		Predicate inPredicate = mock(Predicate.class);
		Predicate likePredicate = mock(Predicate.class);
		Predicate leadOrPredicate = mock(Predicate.class);
		Predicate existsPredicate = mock(Predicate.class);
		Predicate searchOrPredicate = mock(Predicate.class);
		Predicate finalAnd = mock(Predicate.class);
		Order ascOrder = mock(Order.class);
		Subquery<Long> subquery = mock(Subquery.class);
		Root<Group> correlated = mock(Root.class);
		Root<GroupLead> groupLead = mock(Root.class);
		Join<GroupLead, User> leadJoin = mock(Join.class);

		when(query.getResultType()).thenReturn(Group.class);
		when(root.get((SingularAttribute) null)).thenReturn(path);
		when(path.in(Set.of(1L, 2L))).thenReturn(inPredicate);
		when(cb.lower(any(Expression.class))).thenReturn(lowerExpr);
		when(cb.like(eq(lowerExpr), anyString())).thenReturn(likePredicate);
		when(cb.or(any(Predicate.class), any(Predicate.class))).thenReturn(leadOrPredicate, searchOrPredicate);
		when(query.subquery(Long.class)).thenReturn(subquery);
		when(subquery.correlate(root)).thenReturn(correlated);
		when(subquery.from(GroupLead.class)).thenReturn(groupLead);
		when(groupLead.join((SingularAttribute) null)).thenReturn(leadJoin);
		when(cb.literal(1L)).thenReturn(mock(Expression.class));
		when(cb.equal(groupLead, correlated)).thenReturn(mock(Predicate.class));
		when(cb.exists(subquery)).thenReturn(existsPredicate);
		when(cb.asc(lowerExpr)).thenReturn(ascOrder);
		when(cb.and(any(Predicate[].class))).thenReturn(finalAnd);

		// When:
		var spec = builder.build(filter);
		Predicate result = spec.toPredicate(root, query, cb);

		// Then:
		assertThat(result).isSameAs(finalAnd);
		verify(query).orderBy(ascOrder);
	}

	@Test
	void shouldSkipSearchAndOrderForCountQueryTest() {
		// Given:
		GroupSpecificationBuilder builder = new GroupSpecificationBuilder();
		GroupListQuery filter = new GroupListQuery(null, Set.of(1L));
		Root<Group> root = mock(Root.class);
		@SuppressWarnings("unchecked")
		CriteriaQuery<Long> query = mock(CriteriaQuery.class);
		CriteriaBuilder cb = mock(CriteriaBuilder.class);
		@SuppressWarnings("unchecked")
		Path<Object> path = mock(Path.class);
		Predicate inPredicate = mock(Predicate.class);
		Predicate finalAnd = mock(Predicate.class);

		when(query.getResultType()).thenReturn(Long.class);
		when(root.get((SingularAttribute) null)).thenReturn(path);
		when(path.in(Set.of(1L))).thenReturn(inPredicate);
		when(cb.and(any(Predicate[].class))).thenReturn(finalAnd);

		// When:
		var spec = builder.build(filter);
		Predicate result = spec.toPredicate(root, query, cb);

		// Then:
		assertThat(result).isSameAs(finalAnd);
		verify(query, never()).orderBy(any(Order.class));
		verify(cb, never()).or(any(Predicate.class), any(Predicate.class));
	}
}
