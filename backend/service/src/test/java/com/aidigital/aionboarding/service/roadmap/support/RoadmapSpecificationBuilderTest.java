package com.aidigital.aionboarding.service.roadmap.support;

import com.aidigital.aionboarding.domain.learning.entities.UserRoadmap;
import com.aidigital.aionboarding.domain.roadmap.entities.Roadmap;
import com.aidigital.aionboarding.domain.roadmap.entities.Roadmap_;
import com.aidigital.aionboarding.service.common.mapping.TagsFilterSupport;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapListQuery;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapSortField;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.metamodel.SingularAttribute;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoadmapSpecificationBuilderTest {

	@Mock
	private TagsFilterSupport tagsFilterSupport;

	// The static Hibernate metamodel fields this builder relies on (Roadmap_.title,
	// Roadmap_.description, Roadmap_.createdBy, Roadmap_.tags, Roadmap_.authorUser, ...) are only
	// populated by a real JPA provider bootstrap; in this pure-unit-test context they remain null,
	// so every root.get(...) attribute access on a given mock collapses to the identical
	// root.get((SingularAttribute) null) invocation and must share one stubbed Path rather than a
	// distinct mock per field (see LessonSpecificationBuilderTest, which documents and applies the
	// same constraint). Tests instead assert on the criteria-builder method invoked and on
	// genuinely distinct, non-metamodel values (ids, strings), plus the composition/order of the
	// final predicate array passed to cb.and(...).
	@Test
	void shouldBuildPredicateWithAllFiltersForRowQueryTest() {
		// Given:
		RoadmapSpecificationBuilder builder = new RoadmapSpecificationBuilder(tagsFilterSupport);
		RoadmapListQuery filter = new RoadmapListQuery(
				"Search",
				List.of("tag1"),
				5L,
				true,
				RoadmapSortField.TITLE,
				Sort.Direction.ASC
		);
		Long viewerId = 7L;
		when(tagsFilterSupport.toContainmentJson(filter.tags())).thenReturn("[\"tag1\"]");

		CriteriaBuilder cb = mock(CriteriaBuilder.class);
		CriteriaQuery query = mock(CriteriaQuery.class);
		Root root = mock(Root.class);

		Path sharedPath = mock(Path.class);
		when(root.get((SingularAttribute) null)).thenReturn(sharedPath);

		Expression lowerExpr = mock(Expression.class);
		Predicate likePredicate = mock(Predicate.class);
		when(cb.lower(sharedPath)).thenReturn(lowerExpr);
		when(cb.like(lowerExpr, "%search%")).thenReturn(likePredicate);

		Expression searchLiteral = mock(Expression.class);
		Expression tagsContainExpr = mock(Expression.class);
		Predicate tagsContainPredicate = mock(Predicate.class);
		when(cb.literal("Search")).thenReturn(searchLiteral);
		when(cb.function("jsonb_array_contains_ci", Boolean.class, sharedPath, searchLiteral)).thenReturn(tagsContainExpr);
		when(cb.isTrue(tagsContainExpr)).thenReturn(tagsContainPredicate);

		// title/description/createdBy each resolve to the same shared path, so all three
		// cb.like(...) calls are the identical invocation and produce the same predicate.
		Predicate searchOrPredicate = mock(Predicate.class);
		when(cb.or(likePredicate, likePredicate, likePredicate, tagsContainPredicate)).thenReturn(searchOrPredicate);

		Expression containmentLiteral = mock(Expression.class);
		Expression jsonbCastExpr = mock(Expression.class);
		Expression jsonbContainsExpr = mock(Expression.class);
		Predicate jsonbContainsPredicate = mock(Predicate.class);
		when(cb.literal("[\"tag1\"]")).thenReturn(containmentLiteral);
		when(cb.function("jsonb", Object.class, containmentLiteral)).thenReturn(jsonbCastExpr);
		when(cb.function("jsonb_contains", Boolean.class, sharedPath, jsonbCastExpr)).thenReturn(jsonbContainsExpr);
		when(cb.isTrue(jsonbContainsExpr)).thenReturn(jsonbContainsPredicate);

		// createdByUserId: root.get(authorUser).get(id) - the first hop resolves to sharedPath;
		// the second hop is invoked on that mock without further stubbing and returns Mockito's
		// default null, which is the literal argument cb.equal(...) receives here.
		Predicate createdByUserPredicate = mock(Predicate.class);
		when(cb.equal(null, 5L)).thenReturn(createdByUserPredicate);

		Expression literalOne = mock(Expression.class);
		when(cb.literal(1L)).thenReturn(literalOne);

		Subquery enrollmentSubquery = mock(Subquery.class);
		Root correlatedRoadmap = mock(Root.class);
		Root userRoadmapRoot = mock(Root.class);
		when(query.subquery(Long.class)).thenReturn(enrollmentSubquery);
		when(enrollmentSubquery.correlate(root)).thenReturn(correlatedRoadmap);
		when(enrollmentSubquery.from(UserRoadmap.class)).thenReturn(userRoadmapRoot);
		when(enrollmentSubquery.select(literalOne)).thenReturn(enrollmentSubquery);

		// userRoadmap.get(UserRoadmap_.roadmap) and userRoadmap.get(UserRoadmap_.user) are the
		// same collapsed invocation on this mock and therefore share one stubbed Path; the
		// second-level .get(User_.id) chain off that path is unstubbed and returns null.
		Path userRoadmapSharedPath = mock(Path.class);
		when(userRoadmapRoot.get((SingularAttribute) null)).thenReturn(userRoadmapSharedPath);
		Predicate roadmapEqualPredicate = mock(Predicate.class);
		when(cb.equal(userRoadmapSharedPath, correlatedRoadmap)).thenReturn(roadmapEqualPredicate);
		Predicate userIdPredicate = mock(Predicate.class);
		when(cb.equal(null, viewerId)).thenReturn(userIdPredicate);
		when(enrollmentSubquery.where(roadmapEqualPredicate, userIdPredicate)).thenReturn(enrollmentSubquery);
		Predicate enrollmentExists = mock(Predicate.class);
		when(cb.exists(enrollmentSubquery)).thenReturn(enrollmentExists);

		Order titleOrder = mock(Order.class);
		when(cb.asc(sharedPath)).thenReturn(titleOrder);
		when(query.getResultType()).thenReturn(Roadmap.class);

		Predicate finalAndPredicate = mock(Predicate.class);
		when(cb.and(searchOrPredicate, jsonbContainsPredicate, createdByUserPredicate, enrollmentExists))
				.thenReturn(finalAndPredicate);

		// When:
		Specification<Roadmap> spec = builder.build(filter, viewerId);
		Predicate result = spec.toPredicate(root, query, cb);

		// Then:
		assertThat(result).isSameAs(finalAndPredicate);
		verify(query).orderBy(titleOrder);
	}

	@Test
	void shouldSkipOrderForCountQueryTest() {
		// Given:
		RoadmapSpecificationBuilder builder = new RoadmapSpecificationBuilder(tagsFilterSupport);
		RoadmapListQuery filter = new RoadmapListQuery(
				null, null, null, false, RoadmapSortField.CREATED_AT, Sort.Direction.DESC
		);

		CriteriaBuilder cb = mock(CriteriaBuilder.class);
		CriteriaQuery<Long> query = mock(CriteriaQuery.class);
		Root<Roadmap> root = mock(Root.class);
		when(query.getResultType()).thenReturn(Long.class);

		Predicate finalPredicate = mock(Predicate.class);
		when(cb.and()).thenReturn(finalPredicate);

		// When:
		Specification<Roadmap> spec = builder.build(filter, 1L);
		Predicate result = spec.toPredicate(root, query, cb);

		// Then:
		assertThat(result).isSameAs(finalPredicate);
	}

	@Test
	void shouldBuildOrderByCreatedAtDescendingTest() {
		// Given:
		RoadmapSpecificationBuilder builder = new RoadmapSpecificationBuilder(tagsFilterSupport);
		RoadmapListQuery filter = new RoadmapListQuery(
				null, null, null, false, RoadmapSortField.CREATED_AT, Sort.Direction.DESC
		);

		CriteriaBuilder cb = mock(CriteriaBuilder.class);
		CriteriaQuery<Roadmap> query = mock(CriteriaQuery.class);
		Root<Roadmap> root = mock(Root.class);
		Path<LocalDateTime> createdAtPath = mock(Path.class);
		when(root.get(Roadmap_.createdAt)).thenReturn(createdAtPath);
		Order createdAtOrder = mock(Order.class);
		when(cb.desc(createdAtPath)).thenReturn(createdAtOrder);
		when(query.getResultType()).thenReturn(Roadmap.class);
		Predicate finalPredicate = mock(Predicate.class);
		when(cb.and()).thenReturn(finalPredicate);

		// When:
		Specification<Roadmap> spec = builder.build(filter, 1L);
		spec.toPredicate(root, query, cb);

		// Then:
		verify(query).orderBy(createdAtOrder);
	}

	@Test
	void shouldBuildOrderByUpdatedAtAscendingTest() {
		// Given:
		RoadmapSpecificationBuilder builder = new RoadmapSpecificationBuilder(tagsFilterSupport);
		RoadmapListQuery filter = new RoadmapListQuery(
				null, null, null, false, RoadmapSortField.UPDATED_AT, Sort.Direction.ASC
		);

		CriteriaBuilder cb = mock(CriteriaBuilder.class);
		CriteriaQuery<Roadmap> query = mock(CriteriaQuery.class);
		Root<Roadmap> root = mock(Root.class);
		Path<LocalDateTime> updatedAtPath = mock(Path.class);
		when(root.get(Roadmap_.updatedAt)).thenReturn(updatedAtPath);
		Order updatedAtOrder = mock(Order.class);
		when(cb.asc(updatedAtPath)).thenReturn(updatedAtOrder);
		when(query.getResultType()).thenReturn(Roadmap.class);
		Predicate finalPredicate = mock(Predicate.class);
		when(cb.and()).thenReturn(finalPredicate);

		// When:
		Specification<Roadmap> spec = builder.build(filter, 1L);
		spec.toPredicate(root, query, cb);

		// Then:
		verify(query).orderBy(updatedAtOrder);
	}
}
