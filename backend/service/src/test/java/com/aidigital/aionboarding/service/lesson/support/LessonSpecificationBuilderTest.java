package com.aidigital.aionboarding.service.lesson.support;

import com.aidigital.aionboarding.domain.learning.entities.UserLesson;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.lessonactivity.entities.LessonActivity;
import com.aidigital.aionboarding.service.common.mapping.TagsFilterSupport;
import com.aidigital.aionboarding.service.lesson.models.LessonListQuery;
import com.aidigital.aionboarding.service.lesson.models.LessonSortField;
import com.aidigital.aionboarding.service.lesson.models.LessonVisibilityFilter;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.metamodel.SingularAttribute;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LessonSpecificationBuilder}.
 * <p>
 * The static Hibernate metamodel fields this class relies on ({@code Lesson_.title},
 * {@code LessonStatus_.id}, ...) are only populated by a real JPA provider bootstrap; in a
 * pure-unit-test context (no {@code EntityManagerFactory}) they remain {@code null}. Since
 * {@code Root.get(SingularAttribute)} is overloaded, every attribute access in these tests is
 * therefore invoked, and stubbed, as an explicit {@code (SingularAttribute) null} argument rather
 * than a per-field matcher: this is not a wildcard match, it is the literal, deterministic value
 * the production code passes at test time. Consequently individual attribute paths (e.g. "title"
 * vs "tags") cannot be distinguished by argument identity; tests instead assert on the
 * criteria-builder method invoked (like/equal/function/...), on genuinely distinct values that do
 * flow from the filter (ids, strings, booleans, {@link JoinType}), and on the composition/order of
 * the final predicate array passed to {@code cb.and(...)}.
 */
class LessonSpecificationBuilderTest {

	// ------------------------------------------------------------------
	// build() - search text
	// ------------------------------------------------------------------

	@Test
	void shouldAddSearchTextPredicateWhenSearchTextPresentTest() {
		// Given:
		TagsFilterSupport tagsFilterSupport = mock(TagsFilterSupport.class);
		LessonSpecificationBuilder builder = new LessonSpecificationBuilder(tagsFilterSupport);
		LessonListQuery filter = new LessonListQuery(
				"Abc", null, null, null, null, null, null, null, null,
				LessonSortField.CREATED_AT, Sort.Direction.DESC
		);
		LessonVisibilityFilter visibility = new LessonVisibilityFilter(true, false, null);

		Root root = mock(Root.class);
		CriteriaQuery query = mock(CriteriaQuery.class);
		CriteriaBuilder cb = mock(CriteriaBuilder.class);

		Path sharedPath = mock(Path.class);
		Expression lowerExpr = mock(Expression.class);
		Predicate likePredicate = mock(Predicate.class);
		Expression searchLiteral = mock(Expression.class);
		Expression tagsFunctionExpr = mock(Expression.class);
		Predicate isTruePredicate = mock(Predicate.class);
		Predicate orPredicate = mock(Predicate.class);
		Predicate visibilityConjunction = mock(Predicate.class);
		Predicate andPredicate = mock(Predicate.class);

		when(query.getResultType()).thenReturn(Long.class);
		when(root.get((SingularAttribute) null)).thenReturn(sharedPath);
		when(cb.lower(sharedPath)).thenReturn(lowerExpr);
		when(cb.like(lowerExpr, "%abc%")).thenReturn(likePredicate);
		when(cb.literal("Abc")).thenReturn(searchLiteral);
		when(cb.function("jsonb_array_contains_ci", Boolean.class, sharedPath, searchLiteral)).thenReturn(tagsFunctionExpr);
		when(cb.isTrue(tagsFunctionExpr)).thenReturn(isTruePredicate);
		when(cb.or(likePredicate, isTruePredicate)).thenReturn(orPredicate);
		when(cb.conjunction()).thenReturn(visibilityConjunction);
		when(cb.and(any(Predicate[].class))).thenReturn(andPredicate);

		// When:
		Specification<Lesson> spec = builder.build(filter, visibility, null, null, null, null);
		Predicate result = spec.toPredicate(root, query, cb);

		// Then:
		assertThat(result).isSameAs(andPredicate);
		ArgumentCaptor<Predicate[]> captor = ArgumentCaptor.forClass(Predicate[].class);
		verify(cb).and(captor.capture());
		assertThat(captor.getValue()).containsExactly(orPredicate, visibilityConjunction);
		verify(cb).like(lowerExpr, "%abc%");
	}

	@Test
	void shouldSkipSearchTextPredicateWhenSearchTextBlankOrNullTest() {
		// Given: blank search text
		TagsFilterSupport tagsFilterSupport = mock(TagsFilterSupport.class);
		LessonSpecificationBuilder builder = new LessonSpecificationBuilder(tagsFilterSupport);
		LessonVisibilityFilter visibility = new LessonVisibilityFilter(true, false, null);

		LessonListQuery blankFilter = new LessonListQuery(
				"   ", null, null, null, null, null, null, null, null,
				LessonSortField.CREATED_AT, Sort.Direction.DESC
		);
		Root blankRoot = mock(Root.class);
		CriteriaQuery blankQuery = mock(CriteriaQuery.class);
		CriteriaBuilder blankCb = mock(CriteriaBuilder.class);
		Predicate blankConjunction = mock(Predicate.class);
		Predicate blankAnd = mock(Predicate.class);
		when(blankQuery.getResultType()).thenReturn(Long.class);
		when(blankCb.conjunction()).thenReturn(blankConjunction);
		when(blankCb.and(any(Predicate[].class))).thenReturn(blankAnd);

		// When:
		Specification<Lesson> blankSpec = builder.build(blankFilter, visibility, null, null, null, null);
		blankSpec.toPredicate(blankRoot, blankQuery, blankCb);

		// Then: only the visibility predicate is present
		ArgumentCaptor<Predicate[]> blankCaptor = ArgumentCaptor.forClass(Predicate[].class);
		verify(blankCb).and(blankCaptor.capture());
		assertThat(blankCaptor.getValue()).containsExactly(blankConjunction);
		verify(blankRoot, never()).get((SingularAttribute) null);

		// Given: null search text
		LessonListQuery nullFilter = new LessonListQuery(
				null, null, null, null, null, null, null, null, null,
				LessonSortField.CREATED_AT, Sort.Direction.DESC
		);
		Root nullRoot = mock(Root.class);
		CriteriaQuery nullQuery = mock(CriteriaQuery.class);
		CriteriaBuilder nullCb = mock(CriteriaBuilder.class);
		Predicate nullConjunction = mock(Predicate.class);
		Predicate nullAnd = mock(Predicate.class);
		when(nullQuery.getResultType()).thenReturn(Long.class);
		when(nullCb.conjunction()).thenReturn(nullConjunction);
		when(nullCb.and(any(Predicate[].class))).thenReturn(nullAnd);

		// When:
		Specification<Lesson> nullSpec = builder.build(nullFilter, visibility, null, null, null, null);
		nullSpec.toPredicate(nullRoot, nullQuery, nullCb);

		// Then:
		ArgumentCaptor<Predicate[]> nullCaptor = ArgumentCaptor.forClass(Predicate[].class);
		verify(nullCb).and(nullCaptor.capture());
		assertThat(nullCaptor.getValue()).containsExactly(nullConjunction);
	}

	// ------------------------------------------------------------------
	// build() - tags
	// ------------------------------------------------------------------

	@Test
	void shouldAddTagsPredicateWhenTagsPresentTest() {
		// Given:
		TagsFilterSupport tagsFilterSupport = mock(TagsFilterSupport.class);
		LessonSpecificationBuilder builder = new LessonSpecificationBuilder(tagsFilterSupport);
		List<String> tags = List.of("design", "ux");
		LessonListQuery filter = new LessonListQuery(
				null, tags, null, null, null, null, null, null, null,
				LessonSortField.CREATED_AT, Sort.Direction.DESC
		);
		LessonVisibilityFilter visibility = new LessonVisibilityFilter(true, false, null);

		Root root = mock(Root.class);
		CriteriaQuery query = mock(CriteriaQuery.class);
		CriteriaBuilder cb = mock(CriteriaBuilder.class);
		Path tagsPath = mock(Path.class);
		Expression literalExpr = mock(Expression.class);
		Expression jsonbCastExpr = mock(Expression.class);
		Expression containsExpr = mock(Expression.class);
		Predicate isTruePredicate = mock(Predicate.class);
		Predicate visibilityConjunction = mock(Predicate.class);
		Predicate andPredicate = mock(Predicate.class);
		String containmentJson = "[\"design\",\"ux\"]";

		when(query.getResultType()).thenReturn(Long.class);
		when(tagsFilterSupport.toContainmentJson(tags)).thenReturn(containmentJson);
		when(root.get((SingularAttribute) null)).thenReturn(tagsPath);
		when(cb.literal(containmentJson)).thenReturn(literalExpr);
		when(cb.function("jsonb", Object.class, literalExpr)).thenReturn(jsonbCastExpr);
		when(cb.function("jsonb_contains", Boolean.class, tagsPath, jsonbCastExpr)).thenReturn(containsExpr);
		when(cb.isTrue(containsExpr)).thenReturn(isTruePredicate);
		when(cb.conjunction()).thenReturn(visibilityConjunction);
		when(cb.and(any(Predicate[].class))).thenReturn(andPredicate);

		// When:
		Specification<Lesson> spec = builder.build(filter, visibility, null, null, null, null);
		spec.toPredicate(root, query, cb);

		// Then:
		ArgumentCaptor<Predicate[]> captor = ArgumentCaptor.forClass(Predicate[].class);
		verify(cb).and(captor.capture());
		assertThat(captor.getValue()).containsExactly(isTruePredicate, visibilityConjunction);
		verify(tagsFilterSupport).toContainmentJson(tags);
	}

	@Test
	void shouldSkipTagsPredicateWhenTagsNullOrEmptyTest() {
		TagsFilterSupport tagsFilterSupport = mock(TagsFilterSupport.class);
		LessonSpecificationBuilder builder = new LessonSpecificationBuilder(tagsFilterSupport);
		LessonVisibilityFilter visibility = new LessonVisibilityFilter(true, false, null);

		// Given: null tags
		LessonListQuery nullTagsFilter = new LessonListQuery(
				null, null, null, null, null, null, null, null, null,
				LessonSortField.CREATED_AT, Sort.Direction.DESC
		);
		Root root1 = mock(Root.class);
		CriteriaQuery query1 = mock(CriteriaQuery.class);
		CriteriaBuilder cb1 = mock(CriteriaBuilder.class);
		Predicate conjunction1 = mock(Predicate.class);
		Predicate and1 = mock(Predicate.class);
		when(query1.getResultType()).thenReturn(Long.class);
		when(cb1.conjunction()).thenReturn(conjunction1);
		when(cb1.and(any(Predicate[].class))).thenReturn(and1);

		// When:
		Specification<Lesson> spec1 = builder.build(nullTagsFilter, visibility, null, null, null, null);
		spec1.toPredicate(root1, query1, cb1);

		// Then:
		ArgumentCaptor<Predicate[]> captor1 = ArgumentCaptor.forClass(Predicate[].class);
		verify(cb1).and(captor1.capture());
		assertThat(captor1.getValue()).containsExactly(conjunction1);

		// Given: empty tags
		LessonListQuery emptyTagsFilter = new LessonListQuery(
				null, List.of(), null, null, null, null, null, null, null,
				LessonSortField.CREATED_AT, Sort.Direction.DESC
		);
		Root root2 = mock(Root.class);
		CriteriaQuery query2 = mock(CriteriaQuery.class);
		CriteriaBuilder cb2 = mock(CriteriaBuilder.class);
		Predicate conjunction2 = mock(Predicate.class);
		Predicate and2 = mock(Predicate.class);
		when(query2.getResultType()).thenReturn(Long.class);
		when(cb2.conjunction()).thenReturn(conjunction2);
		when(cb2.and(any(Predicate[].class))).thenReturn(and2);

		// When:
		Specification<Lesson> spec2 = builder.build(emptyTagsFilter, visibility, null, null, null, null);
		spec2.toPredicate(root2, query2, cb2);

		// Then:
		ArgumentCaptor<Predicate[]> captor2 = ArgumentCaptor.forClass(Predicate[].class);
		verify(cb2).and(captor2.capture());
		assertThat(captor2.getValue()).containsExactly(conjunction2);
		verifyNoInteractions(tagsFilterSupport);
	}

	// ------------------------------------------------------------------
	// build() - status
	// ------------------------------------------------------------------

	@Test
	void shouldAddStatusPredicateWhenStatusIdPresentTest() {
		// Given:
		TagsFilterSupport tagsFilterSupport = mock(TagsFilterSupport.class);
		LessonSpecificationBuilder builder = new LessonSpecificationBuilder(tagsFilterSupport);
		LessonListQuery filter = new LessonListQuery(
				null, null, null, null, null, null, null, null, null,
				LessonSortField.CREATED_AT, Sort.Direction.DESC
		);
		LessonVisibilityFilter visibility = new LessonVisibilityFilter(true, false, null);

		Root root = mock(Root.class);
		CriteriaQuery query = mock(CriteriaQuery.class);
		CriteriaBuilder cb = mock(CriteriaBuilder.class);
		Path statusPath = mock(Path.class);
		Predicate statusPredicate = mock(Predicate.class);
		Predicate visibilityConjunction = mock(Predicate.class);
		Predicate andPredicate = mock(Predicate.class);

		when(query.getResultType()).thenReturn(Long.class);
		when(root.get((SingularAttribute) null)).thenReturn(statusPath);
		when(cb.equal(null, 100L)).thenReturn(statusPredicate);
		when(cb.conjunction()).thenReturn(visibilityConjunction);
		when(cb.and(any(Predicate[].class))).thenReturn(andPredicate);

		// When:
		Specification<Lesson> spec = builder.build(filter, visibility, 100L, null, null, null);
		spec.toPredicate(root, query, cb);

		// Then:
		ArgumentCaptor<Predicate[]> captor = ArgumentCaptor.forClass(Predicate[].class);
		verify(cb).and(captor.capture());
		assertThat(captor.getValue()).containsExactly(statusPredicate, visibilityConjunction);
	}

	@Test
	void shouldSkipStatusPredicateWhenStatusIdNullTest() {
		// Given:
		TagsFilterSupport tagsFilterSupport = mock(TagsFilterSupport.class);
		LessonSpecificationBuilder builder = new LessonSpecificationBuilder(tagsFilterSupport);
		LessonListQuery filter = new LessonListQuery(
				null, null, null, null, null, null, null, null, null,
				LessonSortField.CREATED_AT, Sort.Direction.DESC
		);
		LessonVisibilityFilter visibility = new LessonVisibilityFilter(true, false, null);

		Root root = mock(Root.class);
		CriteriaQuery query = mock(CriteriaQuery.class);
		CriteriaBuilder cb = mock(CriteriaBuilder.class);
		Predicate visibilityConjunction = mock(Predicate.class);
		Predicate andPredicate = mock(Predicate.class);

		when(query.getResultType()).thenReturn(Long.class);
		when(cb.conjunction()).thenReturn(visibilityConjunction);
		when(cb.and(any(Predicate[].class))).thenReturn(andPredicate);

		// When:
		Specification<Lesson> spec = builder.build(filter, visibility, null, null, null, null);
		spec.toPredicate(root, query, cb);

		// Then:
		ArgumentCaptor<Predicate[]> captor = ArgumentCaptor.forClass(Predicate[].class);
		verify(cb).and(captor.capture());
		assertThat(captor.getValue()).containsExactly(visibilityConjunction);
		verify(root, never()).get((SingularAttribute) null);
	}

	// ------------------------------------------------------------------
	// build() - publication status
	// ------------------------------------------------------------------

	@Test
	void shouldAddPublicationStatusPredicateWhenPublicationStatusIdPresentTest() {
		// Given:
		TagsFilterSupport tagsFilterSupport = mock(TagsFilterSupport.class);
		LessonSpecificationBuilder builder = new LessonSpecificationBuilder(tagsFilterSupport);
		LessonListQuery filter = new LessonListQuery(
				null, null, null, null, null, null, null, null, null,
				LessonSortField.CREATED_AT, Sort.Direction.DESC
		);
		LessonVisibilityFilter visibility = new LessonVisibilityFilter(true, false, null);

		Root root = mock(Root.class);
		CriteriaQuery query = mock(CriteriaQuery.class);
		CriteriaBuilder cb = mock(CriteriaBuilder.class);
		Path pubStatusPath = mock(Path.class);
		Predicate pubStatusPredicate = mock(Predicate.class);
		Predicate visibilityConjunction = mock(Predicate.class);
		Predicate andPredicate = mock(Predicate.class);

		when(query.getResultType()).thenReturn(Long.class);
		when(root.get((SingularAttribute) null)).thenReturn(pubStatusPath);
		when(cb.equal(null, 200L)).thenReturn(pubStatusPredicate);
		when(cb.conjunction()).thenReturn(visibilityConjunction);
		when(cb.and(any(Predicate[].class))).thenReturn(andPredicate);

		// When:
		Specification<Lesson> spec = builder.build(filter, visibility, null, 200L, null, null);
		spec.toPredicate(root, query, cb);

		// Then:
		ArgumentCaptor<Predicate[]> captor = ArgumentCaptor.forClass(Predicate[].class);
		verify(cb).and(captor.capture());
		assertThat(captor.getValue()).containsExactly(pubStatusPredicate, visibilityConjunction);
	}

	@Test
	void shouldSkipPublicationStatusPredicateWhenPublicationStatusIdNullTest() {
		// Given:
		TagsFilterSupport tagsFilterSupport = mock(TagsFilterSupport.class);
		LessonSpecificationBuilder builder = new LessonSpecificationBuilder(tagsFilterSupport);
		LessonListQuery filter = new LessonListQuery(
				null, null, null, null, null, null, null, null, null,
				LessonSortField.CREATED_AT, Sort.Direction.DESC
		);
		LessonVisibilityFilter visibility = new LessonVisibilityFilter(true, false, null);

		Root root = mock(Root.class);
		CriteriaQuery query = mock(CriteriaQuery.class);
		CriteriaBuilder cb = mock(CriteriaBuilder.class);
		Predicate visibilityConjunction = mock(Predicate.class);
		Predicate andPredicate = mock(Predicate.class);

		when(query.getResultType()).thenReturn(Long.class);
		when(cb.conjunction()).thenReturn(visibilityConjunction);
		when(cb.and(any(Predicate[].class))).thenReturn(andPredicate);

		// When:
		Specification<Lesson> spec = builder.build(filter, visibility, null, null, null, null);
		spec.toPredicate(root, query, cb);

		// Then:
		ArgumentCaptor<Predicate[]> captor = ArgumentCaptor.forClass(Predicate[].class);
		verify(cb).and(captor.capture());
		assertThat(captor.getValue()).containsExactly(visibilityConjunction);
	}

	// ------------------------------------------------------------------
	// build() - createdByUserId
	// ------------------------------------------------------------------

	@Test
	void shouldAddCreatedByUserPredicateWhenCreatedByUserIdPresentTest() {
		// Given:
		TagsFilterSupport tagsFilterSupport = mock(TagsFilterSupport.class);
		LessonSpecificationBuilder builder = new LessonSpecificationBuilder(tagsFilterSupport);
		LessonListQuery filter = new LessonListQuery(
				null, null, null, null, 300L, null, null, null, null,
				LessonSortField.CREATED_AT, Sort.Direction.DESC
		);
		LessonVisibilityFilter visibility = new LessonVisibilityFilter(true, false, null);

		Root root = mock(Root.class);
		CriteriaQuery query = mock(CriteriaQuery.class);
		CriteriaBuilder cb = mock(CriteriaBuilder.class);
		Path createdByPath = mock(Path.class);
		Predicate createdByPredicate = mock(Predicate.class);
		Predicate visibilityConjunction = mock(Predicate.class);
		Predicate andPredicate = mock(Predicate.class);

		when(query.getResultType()).thenReturn(Long.class);
		when(root.get((SingularAttribute) null)).thenReturn(createdByPath);
		when(cb.equal(null, 300L)).thenReturn(createdByPredicate);
		when(cb.conjunction()).thenReturn(visibilityConjunction);
		when(cb.and(any(Predicate[].class))).thenReturn(andPredicate);

		// When:
		Specification<Lesson> spec = builder.build(filter, visibility, null, null, null, null);
		spec.toPredicate(root, query, cb);

		// Then:
		ArgumentCaptor<Predicate[]> captor = ArgumentCaptor.forClass(Predicate[].class);
		verify(cb).and(captor.capture());
		assertThat(captor.getValue()).containsExactly(createdByPredicate, visibilityConjunction);
	}

	@Test
	void shouldSkipCreatedByUserPredicateWhenCreatedByUserIdNullTest() {
		// Given:
		TagsFilterSupport tagsFilterSupport = mock(TagsFilterSupport.class);
		LessonSpecificationBuilder builder = new LessonSpecificationBuilder(tagsFilterSupport);
		LessonListQuery filter = new LessonListQuery(
				null, null, null, null, null, null, null, null, null,
				LessonSortField.CREATED_AT, Sort.Direction.DESC
		);
		LessonVisibilityFilter visibility = new LessonVisibilityFilter(true, false, null);

		Root root = mock(Root.class);
		CriteriaQuery query = mock(CriteriaQuery.class);
		CriteriaBuilder cb = mock(CriteriaBuilder.class);
		Predicate visibilityConjunction = mock(Predicate.class);
		Predicate andPredicate = mock(Predicate.class);

		when(query.getResultType()).thenReturn(Long.class);
		when(cb.conjunction()).thenReturn(visibilityConjunction);
		when(cb.and(any(Predicate[].class))).thenReturn(andPredicate);

		// When:
		Specification<Lesson> spec = builder.build(filter, visibility, null, null, null, null);
		spec.toPredicate(root, query, cb);

		// Then:
		ArgumentCaptor<Predicate[]> captor = ArgumentCaptor.forClass(Predicate[].class);
		verify(cb).and(captor.capture());
		assertThat(captor.getValue()).containsExactly(visibilityConjunction);
	}

	// ------------------------------------------------------------------
	// build() - readyOnly
	// ------------------------------------------------------------------

	@Test
	void shouldAddReadyOnlyPredicateWhenReadyOnlyTrueTest() {
		// Given:
		TagsFilterSupport tagsFilterSupport = mock(TagsFilterSupport.class);
		LessonSpecificationBuilder builder = new LessonSpecificationBuilder(tagsFilterSupport);
		LessonListQuery filter = new LessonListQuery(
				null, null, null, null, null, null, true, null, null,
				LessonSortField.CREATED_AT, Sort.Direction.DESC
		);
		LessonVisibilityFilter visibility = new LessonVisibilityFilter(true, false, null);

		Root root = mock(Root.class);
		CriteriaQuery query = mock(CriteriaQuery.class);
		CriteriaBuilder cb = mock(CriteriaBuilder.class);
		Path statusPath = mock(Path.class);
		Predicate readyPredicate = mock(Predicate.class);
		Predicate visibilityConjunction = mock(Predicate.class);
		Predicate andPredicate = mock(Predicate.class);

		when(query.getResultType()).thenReturn(Long.class);
		when(root.get((SingularAttribute) null)).thenReturn(statusPath);
		when(cb.equal(null, 400L)).thenReturn(readyPredicate);
		when(cb.conjunction()).thenReturn(visibilityConjunction);
		when(cb.and(any(Predicate[].class))).thenReturn(andPredicate);

		// When:
		Specification<Lesson> spec = builder.build(filter, visibility, null, null, 400L, null);
		spec.toPredicate(root, query, cb);

		// Then:
		ArgumentCaptor<Predicate[]> captor = ArgumentCaptor.forClass(Predicate[].class);
		verify(cb).and(captor.capture());
		assertThat(captor.getValue()).containsExactly(readyPredicate, visibilityConjunction);
	}

	@Test
	void shouldSkipReadyOnlyPredicateWhenReadyOnlyFalseOrNullTest() {
		TagsFilterSupport tagsFilterSupport = mock(TagsFilterSupport.class);
		LessonSpecificationBuilder builder = new LessonSpecificationBuilder(tagsFilterSupport);
		LessonVisibilityFilter visibility = new LessonVisibilityFilter(true, false, null);

		// Given: readyOnly = false
		LessonListQuery falseFilter = new LessonListQuery(
				null, null, null, null, null, null, false, null, null,
				LessonSortField.CREATED_AT, Sort.Direction.DESC
		);
		Root root1 = mock(Root.class);
		CriteriaQuery query1 = mock(CriteriaQuery.class);
		CriteriaBuilder cb1 = mock(CriteriaBuilder.class);
		Predicate conjunction1 = mock(Predicate.class);
		Predicate and1 = mock(Predicate.class);
		when(query1.getResultType()).thenReturn(Long.class);
		when(cb1.conjunction()).thenReturn(conjunction1);
		when(cb1.and(any(Predicate[].class))).thenReturn(and1);

		// When:
		Specification<Lesson> spec1 = builder.build(falseFilter, visibility, null, null, 400L, null);
		spec1.toPredicate(root1, query1, cb1);

		// Then:
		ArgumentCaptor<Predicate[]> captor1 = ArgumentCaptor.forClass(Predicate[].class);
		verify(cb1).and(captor1.capture());
		assertThat(captor1.getValue()).containsExactly(conjunction1);

		// Given: readyOnly = null
		LessonListQuery nullFilter = new LessonListQuery(
				null, null, null, null, null, null, null, null, null,
				LessonSortField.CREATED_AT, Sort.Direction.DESC
		);
		Root root2 = mock(Root.class);
		CriteriaQuery query2 = mock(CriteriaQuery.class);
		CriteriaBuilder cb2 = mock(CriteriaBuilder.class);
		Predicate conjunction2 = mock(Predicate.class);
		Predicate and2 = mock(Predicate.class);
		when(query2.getResultType()).thenReturn(Long.class);
		when(cb2.conjunction()).thenReturn(conjunction2);
		when(cb2.and(any(Predicate[].class))).thenReturn(and2);

		// When:
		Specification<Lesson> spec2 = builder.build(nullFilter, visibility, null, null, 400L, null);
		spec2.toPredicate(root2, query2, cb2);

		// Then:
		ArgumentCaptor<Predicate[]> captor2 = ArgumentCaptor.forClass(Predicate[].class);
		verify(cb2).and(captor2.capture());
		assertThat(captor2.getValue()).containsExactly(conjunction2);
	}

	// ------------------------------------------------------------------
	// build() - assignedToMe
	// ------------------------------------------------------------------

	@Test
	void shouldAddAssignedToMePredicateWhenAssignedToMeTrueTest() {
		// Given:
		TagsFilterSupport tagsFilterSupport = mock(TagsFilterSupport.class);
		LessonSpecificationBuilder builder = new LessonSpecificationBuilder(tagsFilterSupport);
		LessonListQuery filter = new LessonListQuery(
				null, null, null, null, null, true, null, null, null,
				LessonSortField.CREATED_AT, Sort.Direction.DESC
		);
		LessonVisibilityFilter visibility = new LessonVisibilityFilter(true, false, 55L);

		Root root = mock(Root.class);
		CriteriaQuery query = mock(CriteriaQuery.class);
		CriteriaBuilder cb = mock(CriteriaBuilder.class);
		Subquery subquery = mock(Subquery.class);
		Root userLessonRoot = mock(Root.class);
		Predicate existsPredicate = mock(Predicate.class);
		Predicate visibilityConjunction = mock(Predicate.class);
		Predicate andPredicate = mock(Predicate.class);

		when(query.getResultType()).thenReturn(Long.class);
		when(query.subquery(Long.class)).thenReturn(subquery);
		when(subquery.from(UserLesson.class)).thenReturn(userLessonRoot);
		// userLesson.get(UserLesson_.user).get(User_.id) chains two levels deep, so the first
		// level must resolve to a non-null Path even though its own identity isn't asserted here.
		when(userLessonRoot.get((SingularAttribute) null)).thenReturn(mock(Path.class));
		when(cb.exists(subquery)).thenReturn(existsPredicate);
		when(cb.conjunction()).thenReturn(visibilityConjunction);
		when(cb.and(any(Predicate[].class))).thenReturn(andPredicate);

		// When:
		Specification<Lesson> spec = builder.build(filter, visibility, null, null, null, null);
		spec.toPredicate(root, query, cb);

		// Then:
		ArgumentCaptor<Predicate[]> captor = ArgumentCaptor.forClass(Predicate[].class);
		verify(cb).and(captor.capture());
		assertThat(captor.getValue()).containsExactly(existsPredicate, visibilityConjunction);
		verify(subquery).correlate(root);
	}

	@Test
	void shouldSkipAssignedToMePredicateWhenAssignedToMeFalseOrNullTest() {
		TagsFilterSupport tagsFilterSupport = mock(TagsFilterSupport.class);
		LessonSpecificationBuilder builder = new LessonSpecificationBuilder(tagsFilterSupport);
		LessonVisibilityFilter visibility = new LessonVisibilityFilter(true, false, null);

		// Given: assignedToMe = false
		LessonListQuery falseFilter = new LessonListQuery(
				null, null, null, null, null, false, null, null, null,
				LessonSortField.CREATED_AT, Sort.Direction.DESC
		);
		Root root1 = mock(Root.class);
		CriteriaQuery query1 = mock(CriteriaQuery.class);
		CriteriaBuilder cb1 = mock(CriteriaBuilder.class);
		Predicate conjunction1 = mock(Predicate.class);
		Predicate and1 = mock(Predicate.class);
		when(query1.getResultType()).thenReturn(Long.class);
		when(cb1.conjunction()).thenReturn(conjunction1);
		when(cb1.and(any(Predicate[].class))).thenReturn(and1);

		// When:
		Specification<Lesson> spec1 = builder.build(falseFilter, visibility, null, null, null, null);
		spec1.toPredicate(root1, query1, cb1);

		// Then:
		ArgumentCaptor<Predicate[]> captor1 = ArgumentCaptor.forClass(Predicate[].class);
		verify(cb1).and(captor1.capture());
		assertThat(captor1.getValue()).containsExactly(conjunction1);
		verify(query1, never()).subquery(Long.class);

		// Given: assignedToMe = null
		LessonListQuery nullFilter = new LessonListQuery(
				null, null, null, null, null, null, null, null, null,
				LessonSortField.CREATED_AT, Sort.Direction.DESC
		);
		Root root2 = mock(Root.class);
		CriteriaQuery query2 = mock(CriteriaQuery.class);
		CriteriaBuilder cb2 = mock(CriteriaBuilder.class);
		Predicate conjunction2 = mock(Predicate.class);
		Predicate and2 = mock(Predicate.class);
		when(query2.getResultType()).thenReturn(Long.class);
		when(cb2.conjunction()).thenReturn(conjunction2);
		when(cb2.and(any(Predicate[].class))).thenReturn(and2);

		// When:
		Specification<Lesson> spec2 = builder.build(nullFilter, visibility, null, null, null, null);
		spec2.toPredicate(root2, query2, cb2);

		// Then:
		ArgumentCaptor<Predicate[]> captor2 = ArgumentCaptor.forClass(Predicate[].class);
		verify(cb2).and(captor2.capture());
		assertThat(captor2.getValue()).containsExactly(conjunction2);
		verify(query2, never()).subquery(Long.class);
	}

	// ------------------------------------------------------------------
	// build() - activityTypeCode
	// ------------------------------------------------------------------

	@Test
	void shouldAddActivityTypePredicateWhenActivityTypeCodePresentTest() {
		// Given:
		TagsFilterSupport tagsFilterSupport = mock(TagsFilterSupport.class);
		LessonSpecificationBuilder builder = new LessonSpecificationBuilder(tagsFilterSupport);
		LessonListQuery filter = new LessonListQuery(
				null, null, null, null, null, null, null, "QUIZ", null,
				LessonSortField.CREATED_AT, Sort.Direction.DESC
		);
		LessonVisibilityFilter visibility = new LessonVisibilityFilter(true, false, null);

		Root root = mock(Root.class);
		CriteriaQuery query = mock(CriteriaQuery.class);
		CriteriaBuilder cb = mock(CriteriaBuilder.class);
		Subquery subquery = mock(Subquery.class);
		Root lessonActivityRoot = mock(Root.class);
		Join typeJoin = mock(Join.class);
		Predicate existsPredicate = mock(Predicate.class);
		Predicate visibilityConjunction = mock(Predicate.class);
		Predicate andPredicate = mock(Predicate.class);

		when(query.getResultType()).thenReturn(Long.class);
		when(query.subquery(Long.class)).thenReturn(subquery);
		when(subquery.from(LessonActivity.class)).thenReturn(lessonActivityRoot);
		when(lessonActivityRoot.join((SingularAttribute) null)).thenReturn(typeJoin);
		when(cb.exists(subquery)).thenReturn(existsPredicate);
		when(cb.conjunction()).thenReturn(visibilityConjunction);
		when(cb.and(any(Predicate[].class))).thenReturn(andPredicate);

		// When:
		Specification<Lesson> spec = builder.build(filter, visibility, null, null, null, null);
		spec.toPredicate(root, query, cb);

		// Then:
		ArgumentCaptor<Predicate[]> captor = ArgumentCaptor.forClass(Predicate[].class);
		verify(cb).and(captor.capture());
		assertThat(captor.getValue()).containsExactly(existsPredicate, visibilityConjunction);
		verify(subquery).correlate(root);
	}

	@Test
	void shouldSkipActivityTypePredicateWhenActivityTypeCodeNullTest() {
		// Given:
		TagsFilterSupport tagsFilterSupport = mock(TagsFilterSupport.class);
		LessonSpecificationBuilder builder = new LessonSpecificationBuilder(tagsFilterSupport);
		LessonListQuery filter = new LessonListQuery(
				null, null, null, null, null, null, null, null, null,
				LessonSortField.CREATED_AT, Sort.Direction.DESC
		);
		LessonVisibilityFilter visibility = new LessonVisibilityFilter(true, false, null);

		Root root = mock(Root.class);
		CriteriaQuery query = mock(CriteriaQuery.class);
		CriteriaBuilder cb = mock(CriteriaBuilder.class);
		Predicate visibilityConjunction = mock(Predicate.class);
		Predicate andPredicate = mock(Predicate.class);

		when(query.getResultType()).thenReturn(Long.class);
		when(cb.conjunction()).thenReturn(visibilityConjunction);
		when(cb.and(any(Predicate[].class))).thenReturn(andPredicate);

		// When:
		Specification<Lesson> spec = builder.build(filter, visibility, null, null, null, null);
		spec.toPredicate(root, query, cb);

		// Then:
		ArgumentCaptor<Predicate[]> captor = ArgumentCaptor.forClass(Predicate[].class);
		verify(cb).and(captor.capture());
		assertThat(captor.getValue()).containsExactly(visibilityConjunction);
		verify(query, never()).subquery(Long.class);
	}

	// ------------------------------------------------------------------
	// build() - hasActivities
	// ------------------------------------------------------------------

	@Test
	void shouldAddHasActivitiesPredicateWhenHasActivitiesPresentTest() {
		// Given:
		TagsFilterSupport tagsFilterSupport = mock(TagsFilterSupport.class);
		LessonSpecificationBuilder builder = new LessonSpecificationBuilder(tagsFilterSupport);
		LessonListQuery filter = new LessonListQuery(
				null, null, null, null, null, null, null, null, true,
				LessonSortField.CREATED_AT, Sort.Direction.DESC
		);
		LessonVisibilityFilter visibility = new LessonVisibilityFilter(true, false, null);

		Root root = mock(Root.class);
		CriteriaQuery query = mock(CriteriaQuery.class);
		CriteriaBuilder cb = mock(CriteriaBuilder.class);
		Subquery subquery = mock(Subquery.class);
		Root lessonActivityRoot = mock(Root.class);
		Predicate existsPredicate = mock(Predicate.class);
		Predicate visibilityConjunction = mock(Predicate.class);
		Predicate andPredicate = mock(Predicate.class);

		when(query.getResultType()).thenReturn(Long.class);
		when(query.subquery(Long.class)).thenReturn(subquery);
		when(subquery.from(LessonActivity.class)).thenReturn(lessonActivityRoot);
		when(cb.exists(subquery)).thenReturn(existsPredicate);
		when(cb.conjunction()).thenReturn(visibilityConjunction);
		when(cb.and(any(Predicate[].class))).thenReturn(andPredicate);

		// When:
		Specification<Lesson> spec = builder.build(filter, visibility, null, null, null, null);
		spec.toPredicate(root, query, cb);

		// Then:
		ArgumentCaptor<Predicate[]> captor = ArgumentCaptor.forClass(Predicate[].class);
		verify(cb).and(captor.capture());
		assertThat(captor.getValue()).containsExactly(existsPredicate, visibilityConjunction);
	}

	@Test
	void shouldSkipHasActivitiesPredicateWhenHasActivitiesNullTest() {
		// Given:
		TagsFilterSupport tagsFilterSupport = mock(TagsFilterSupport.class);
		LessonSpecificationBuilder builder = new LessonSpecificationBuilder(tagsFilterSupport);
		LessonListQuery filter = new LessonListQuery(
				null, null, null, null, null, null, null, null, null,
				LessonSortField.CREATED_AT, Sort.Direction.DESC
		);
		LessonVisibilityFilter visibility = new LessonVisibilityFilter(true, false, null);

		Root root = mock(Root.class);
		CriteriaQuery query = mock(CriteriaQuery.class);
		CriteriaBuilder cb = mock(CriteriaBuilder.class);
		Predicate visibilityConjunction = mock(Predicate.class);
		Predicate andPredicate = mock(Predicate.class);

		when(query.getResultType()).thenReturn(Long.class);
		when(cb.conjunction()).thenReturn(visibilityConjunction);
		when(cb.and(any(Predicate[].class))).thenReturn(andPredicate);

		// When:
		Specification<Lesson> spec = builder.build(filter, visibility, null, null, null, null);
		spec.toPredicate(root, query, cb);

		// Then:
		ArgumentCaptor<Predicate[]> captor = ArgumentCaptor.forClass(Predicate[].class);
		verify(cb).and(captor.capture());
		assertThat(captor.getValue()).containsExactly(visibilityConjunction);
		verify(query, never()).subquery(Long.class);
	}

	// ------------------------------------------------------------------
	// build() - fetch (Lesson.class result type)
	// ------------------------------------------------------------------

	@Test
	void shouldFetchAssociationsWhenResultTypeIsLessonTest() {
		// Given:
		TagsFilterSupport tagsFilterSupport = mock(TagsFilterSupport.class);
		LessonSpecificationBuilder builder = new LessonSpecificationBuilder(tagsFilterSupport);
		LessonListQuery filter = new LessonListQuery(
				null, null, null, null, null, null, null, null, null,
				LessonSortField.CREATED_AT, Sort.Direction.DESC
		);
		LessonVisibilityFilter visibility = new LessonVisibilityFilter(true, false, null);

		Root root = mock(Root.class);
		CriteriaQuery query = mock(CriteriaQuery.class);
		CriteriaBuilder cb = mock(CriteriaBuilder.class);
		Predicate visibilityConjunction = mock(Predicate.class);
		Predicate andPredicate = mock(Predicate.class);
		Path createdAtPath = mock(Path.class);
		Order descOrder = mock(Order.class);

		when(query.getResultType()).thenReturn(Lesson.class);
		when(cb.conjunction()).thenReturn(visibilityConjunction);
		when(cb.and(any(Predicate[].class))).thenReturn(andPredicate);
		when(root.get((SingularAttribute) null)).thenReturn(createdAtPath);
		when(cb.desc(createdAtPath)).thenReturn(descOrder);

		// When:
		Specification<Lesson> spec = builder.build(filter, visibility, null, null, null, null);
		spec.toPredicate(root, query, cb);

		// Then:
		verify(root, org.mockito.Mockito.times(2)).fetch((SingularAttribute) null);
		verify(root).fetch((SingularAttribute) null, JoinType.LEFT);
		verify(query).orderBy(descOrder);
	}

	@Test
	void shouldSkipFetchWhenResultTypeIsNotLessonTest() {
		// Given:
		TagsFilterSupport tagsFilterSupport = mock(TagsFilterSupport.class);
		LessonSpecificationBuilder builder = new LessonSpecificationBuilder(tagsFilterSupport);
		LessonListQuery filter = new LessonListQuery(
				null, null, null, null, null, null, null, null, null,
				LessonSortField.CREATED_AT, Sort.Direction.DESC
		);
		LessonVisibilityFilter visibility = new LessonVisibilityFilter(true, false, null);

		Root root = mock(Root.class);
		CriteriaQuery query = mock(CriteriaQuery.class);
		CriteriaBuilder cb = mock(CriteriaBuilder.class);
		Predicate visibilityConjunction = mock(Predicate.class);
		Predicate andPredicate = mock(Predicate.class);

		when(query.getResultType()).thenReturn(Long.class);
		when(cb.conjunction()).thenReturn(visibilityConjunction);
		when(cb.and(any(Predicate[].class))).thenReturn(andPredicate);

		// When:
		Specification<Lesson> spec = builder.build(filter, visibility, null, null, null, null);
		spec.toPredicate(root, query, cb);

		// Then:
		verify(root, never()).fetch((SingularAttribute) null);
		verify(root, never()).fetch((SingularAttribute) null, JoinType.LEFT);
	}

	// ------------------------------------------------------------------
	// build() - orderBy
	// ------------------------------------------------------------------

	@Test
	void shouldApplyOrderWhenResultTypeIsNotLongTest() {
		// Given:
		TagsFilterSupport tagsFilterSupport = mock(TagsFilterSupport.class);
		LessonSpecificationBuilder builder = new LessonSpecificationBuilder(tagsFilterSupport);
		LessonListQuery filter = new LessonListQuery(
				null, null, null, null, null, null, null, null, null,
				LessonSortField.TITLE, Sort.Direction.ASC
		);
		LessonVisibilityFilter visibility = new LessonVisibilityFilter(true, false, null);

		Root root = mock(Root.class);
		CriteriaQuery query = mock(CriteriaQuery.class);
		CriteriaBuilder cb = mock(CriteriaBuilder.class);
		Predicate visibilityConjunction = mock(Predicate.class);
		Predicate andPredicate = mock(Predicate.class);
		Path titlePath = mock(Path.class);
		Order ascOrder = mock(Order.class);

		// A non-Lesson, non-Long result type represents the lean summary projection, which is
		// still ordered but never fetch-joined.
		when(query.getResultType()).thenReturn(String.class);
		when(cb.conjunction()).thenReturn(visibilityConjunction);
		when(cb.and(any(Predicate[].class))).thenReturn(andPredicate);
		when(root.get((SingularAttribute) null)).thenReturn(titlePath);
		when(cb.asc(titlePath)).thenReturn(ascOrder);

		// When:
		Specification<Lesson> spec = builder.build(filter, visibility, null, null, null, null);
		spec.toPredicate(root, query, cb);

		// Then:
		verify(query).orderBy(ascOrder);
		verify(root, never()).fetch((SingularAttribute) null);
	}

	@Test
	void shouldSkipOrderWhenResultTypeIsLongTest() {
		// Given:
		TagsFilterSupport tagsFilterSupport = mock(TagsFilterSupport.class);
		LessonSpecificationBuilder builder = new LessonSpecificationBuilder(tagsFilterSupport);
		LessonListQuery filter = new LessonListQuery(
				null, null, null, null, null, null, null, null, null,
				LessonSortField.CREATED_AT, Sort.Direction.DESC
		);
		LessonVisibilityFilter visibility = new LessonVisibilityFilter(true, false, null);

		Root root = mock(Root.class);
		CriteriaQuery query = mock(CriteriaQuery.class);
		CriteriaBuilder cb = mock(CriteriaBuilder.class);
		Predicate visibilityConjunction = mock(Predicate.class);
		Predicate andPredicate = mock(Predicate.class);

		when(query.getResultType()).thenReturn(Long.class);
		when(cb.conjunction()).thenReturn(visibilityConjunction);
		when(cb.and(any(Predicate[].class))).thenReturn(andPredicate);

		// When:
		Specification<Lesson> spec = builder.build(filter, visibility, null, null, null, null);
		spec.toPredicate(root, query, cb);

		// Then: buildOrder is never invoked, so the sort expression is never looked up either
		verify(root, never()).get((SingularAttribute) null);
	}

	// ------------------------------------------------------------------
	// build() - multiple criteria combined
	// ------------------------------------------------------------------

	@Test
	void shouldCombineMultipleCriteriaWithAndTest() {
		// Given: every optional filter present at once, admin visibility, count-query result type
		// (skips fetch/orderBy so the test stays focused on predicate composition).
		TagsFilterSupport tagsFilterSupport = mock(TagsFilterSupport.class);
		LessonSpecificationBuilder builder = new LessonSpecificationBuilder(tagsFilterSupport);
		List<String> tags = List.of("design");
		LessonListQuery filter = new LessonListQuery(
				"abc", tags, "STATUS_CODE", "PUB_CODE", 300L, true, true, "QUIZ", true,
				LessonSortField.CREATED_AT, Sort.Direction.DESC
		);
		LessonVisibilityFilter visibility = new LessonVisibilityFilter(true, false, 999L);

		Root root = mock(Root.class);
		CriteriaQuery query = mock(CriteriaQuery.class);
		CriteriaBuilder cb = mock(CriteriaBuilder.class);

		Path sharedPath = mock(Path.class);
		Expression lowerExpr = mock(Expression.class);
		Predicate likePredicate = mock(Predicate.class);
		Expression searchTagsLiteral = mock(Expression.class);
		Expression searchTagsFunctionExpr = mock(Expression.class);
		Predicate searchTagsIsTrue = mock(Predicate.class);
		Predicate searchOrPredicate = mock(Predicate.class);

		String containmentJson = "[\"design\"]";
		Expression tagsFilterLiteral = mock(Expression.class);
		Expression tagsFilterJsonbCast = mock(Expression.class);
		Expression tagsFilterContainsExpr = mock(Expression.class);
		Predicate tagsFilterIsTrue = mock(Predicate.class);

		Predicate statusPredicate = mock(Predicate.class);
		Predicate pubStatusPredicate = mock(Predicate.class);
		Predicate createdByPredicate = mock(Predicate.class);
		Predicate readyPredicate = mock(Predicate.class);

		Subquery enrolledSubquery = mock(Subquery.class);
		Root userLessonRoot = mock(Root.class);
		Predicate enrolledExists = mock(Predicate.class);

		Subquery activitySubquery = mock(Subquery.class);
		Root activityLessonActivityRoot = mock(Root.class);
		Join activityTypeJoin = mock(Join.class);
		Predicate activityExists = mock(Predicate.class);

		Subquery hasActivitiesSubquery = mock(Subquery.class);
		Root hasActivitiesLessonActivityRoot = mock(Root.class);
		Predicate hasActivitiesExists = mock(Predicate.class);

		Predicate visibilityConjunction = mock(Predicate.class);
		Predicate andPredicate = mock(Predicate.class);

		when(query.getResultType()).thenReturn(Long.class);
		// Every attribute path collapses to the same shared mock (see class javadoc); values that
		// are only ever forwarded as opaque arguments (never dereferenced) are left as Mockito's
		// default null return, which is safe here.
		when(root.get((SingularAttribute) null)).thenReturn(sharedPath);

		when(cb.lower(sharedPath)).thenReturn(lowerExpr);
		when(cb.like(lowerExpr, "%abc%")).thenReturn(likePredicate);
		when(cb.literal("abc")).thenReturn(searchTagsLiteral);
		when(cb.function("jsonb_array_contains_ci", Boolean.class, sharedPath, searchTagsLiteral)).thenReturn(searchTagsFunctionExpr);
		when(cb.isTrue(searchTagsFunctionExpr)).thenReturn(searchTagsIsTrue);
		when(cb.or(likePredicate, searchTagsIsTrue)).thenReturn(searchOrPredicate);

		when(tagsFilterSupport.toContainmentJson(tags)).thenReturn(containmentJson);
		when(cb.literal(containmentJson)).thenReturn(tagsFilterLiteral);
		when(cb.function("jsonb", Object.class, tagsFilterLiteral)).thenReturn(tagsFilterJsonbCast);
		when(cb.function("jsonb_contains", Boolean.class, sharedPath, tagsFilterJsonbCast)).thenReturn(tagsFilterContainsExpr);
		when(cb.isTrue(tagsFilterContainsExpr)).thenReturn(tagsFilterIsTrue);

		when(cb.equal(null, 100L)).thenReturn(statusPredicate);
		when(cb.equal(null, 200L)).thenReturn(pubStatusPredicate);
		when(cb.equal(null, 300L)).thenReturn(createdByPredicate);
		when(cb.equal(null, 400L)).thenReturn(readyPredicate);

		when(query.subquery(Long.class)).thenReturn(enrolledSubquery, activitySubquery, hasActivitiesSubquery);
		when(enrolledSubquery.from(UserLesson.class)).thenReturn(userLessonRoot);
		// userLesson.get(UserLesson_.user).get(User_.id) chains two levels deep, so the first
		// level must resolve to a non-null Path even though its own identity isn't asserted here.
		when(userLessonRoot.get((SingularAttribute) null)).thenReturn(mock(Path.class));
		when(cb.exists(enrolledSubquery)).thenReturn(enrolledExists);

		when(activitySubquery.from(LessonActivity.class)).thenReturn(activityLessonActivityRoot);
		when(activityLessonActivityRoot.join((SingularAttribute) null)).thenReturn(activityTypeJoin);
		when(cb.exists(activitySubquery)).thenReturn(activityExists);

		when(hasActivitiesSubquery.from(LessonActivity.class)).thenReturn(hasActivitiesLessonActivityRoot);
		when(cb.exists(hasActivitiesSubquery)).thenReturn(hasActivitiesExists);

		when(cb.conjunction()).thenReturn(visibilityConjunction);
		when(cb.and(any(Predicate[].class))).thenReturn(andPredicate);

		// When:
		Specification<Lesson> spec = builder.build(filter, visibility, 100L, 200L, 400L, 500L);
		Predicate result = spec.toPredicate(root, query, cb);

		// Then: every active criterion contributes exactly one predicate, in declaration order,
		// followed by the visibility predicate.
		assertThat(result).isSameAs(andPredicate);
		ArgumentCaptor<Predicate[]> captor = ArgumentCaptor.forClass(Predicate[].class);
		verify(cb).and(captor.capture());
		assertThat(captor.getValue()).containsExactly(
				searchOrPredicate, tagsFilterIsTrue, statusPredicate, pubStatusPredicate,
				createdByPredicate, readyPredicate, enrolledExists, activityExists, hasActivitiesExists,
				visibilityConjunction
		);
	}

	// ------------------------------------------------------------------
	// buildOrder()
	// ------------------------------------------------------------------

	@Test
	void shouldBuildAscendingOrderForCreatedAtTest() {
		// Given:
		TagsFilterSupport tagsFilterSupport = mock(TagsFilterSupport.class);
		LessonSpecificationBuilder builder = new LessonSpecificationBuilder(tagsFilterSupport);
		CriteriaBuilder cb = mock(CriteriaBuilder.class);
		Root root = mock(Root.class);
		Path createdAtPath = mock(Path.class);
		Order ascOrder = mock(Order.class);
		LessonListQuery filter = new LessonListQuery(
				null, null, null, null, null, null, null, null, null,
				LessonSortField.CREATED_AT, Sort.Direction.ASC
		);

		when(root.get((SingularAttribute) null)).thenReturn(createdAtPath);
		when(cb.asc(createdAtPath)).thenReturn(ascOrder);

		// When:
		Order result = builder.buildOrder(cb, root, filter);

		// Then:
		assertThat(result).isSameAs(ascOrder);
		verify(cb).asc(createdAtPath);
	}

	@Test
	void shouldBuildDescendingOrderForUpdatedAtTest() {
		// Given:
		TagsFilterSupport tagsFilterSupport = mock(TagsFilterSupport.class);
		LessonSpecificationBuilder builder = new LessonSpecificationBuilder(tagsFilterSupport);
		CriteriaBuilder cb = mock(CriteriaBuilder.class);
		Root root = mock(Root.class);
		Path updatedAtPath = mock(Path.class);
		Order descOrder = mock(Order.class);
		LessonListQuery filter = new LessonListQuery(
				null, null, null, null, null, null, null, null, null,
				LessonSortField.UPDATED_AT, Sort.Direction.DESC
		);

		when(root.get((SingularAttribute) null)).thenReturn(updatedAtPath);
		when(cb.desc(updatedAtPath)).thenReturn(descOrder);

		// When:
		Order result = builder.buildOrder(cb, root, filter);

		// Then:
		assertThat(result).isSameAs(descOrder);
		verify(cb).desc(updatedAtPath);
	}

	@Test
	void shouldBuildOrderForTitleTest() {
		// Given:
		TagsFilterSupport tagsFilterSupport = mock(TagsFilterSupport.class);
		LessonSpecificationBuilder builder = new LessonSpecificationBuilder(tagsFilterSupport);
		CriteriaBuilder cb = mock(CriteriaBuilder.class);
		Root root = mock(Root.class);
		Path titlePath = mock(Path.class);
		Order ascOrder = mock(Order.class);
		LessonListQuery filter = new LessonListQuery(
				null, null, null, null, null, null, null, null, null,
				LessonSortField.TITLE, Sort.Direction.ASC
		);

		when(root.get((SingularAttribute) null)).thenReturn(titlePath);
		when(cb.asc(titlePath)).thenReturn(ascOrder);

		// When:
		Order result = builder.buildOrder(cb, root, filter);

		// Then:
		assertThat(result).isSameAs(ascOrder);
		verify(cb).asc(titlePath);
	}

	// ------------------------------------------------------------------
	// visibilityPredicate()
	// ------------------------------------------------------------------

	@Test
	void shouldReturnConjunctionWhenAdminTest() {
		// Given:
		TagsFilterSupport tagsFilterSupport = mock(TagsFilterSupport.class);
		LessonSpecificationBuilder builder = new LessonSpecificationBuilder(tagsFilterSupport);
		CriteriaBuilder cb = mock(CriteriaBuilder.class);
		Root root = mock(Root.class);
		Predicate conjunction = mock(Predicate.class);
		LessonVisibilityFilter visibility = new LessonVisibilityFilter(true, false, 5L);

		when(cb.conjunction()).thenReturn(conjunction);

		// When:
		Predicate result = builder.visibilityPredicate(cb, root, visibility, 900L);

		// Then:
		assertThat(result).isSameAs(conjunction);
		verify(cb).conjunction();
	}

	@Test
	void shouldReturnPublishedOnlyPredicateWhenCannotManageOwnLessonsTest() {
		// Given:
		TagsFilterSupport tagsFilterSupport = mock(TagsFilterSupport.class);
		LessonSpecificationBuilder builder = new LessonSpecificationBuilder(tagsFilterSupport);
		CriteriaBuilder cb = mock(CriteriaBuilder.class);
		Root root = mock(Root.class);
		Path pubStatusPath = mock(Path.class);
		Predicate publishedPredicate = mock(Predicate.class);
		LessonVisibilityFilter visibility = new LessonVisibilityFilter(false, false, 5L);

		when(root.get((SingularAttribute) null)).thenReturn(pubStatusPath);
		when(cb.equal(null, 900L)).thenReturn(publishedPredicate);

		// When:
		Predicate result = builder.visibilityPredicate(cb, root, visibility, 900L);

		// Then:
		assertThat(result).isSameAs(publishedPredicate);
		verify(cb).equal(null, 900L);
	}

	@Test
	void shouldReturnOwnedOrPublishedPredicateWhenCanManageOwnLessonsTest() {
		// Given:
		TagsFilterSupport tagsFilterSupport = mock(TagsFilterSupport.class);
		LessonSpecificationBuilder builder = new LessonSpecificationBuilder(tagsFilterSupport);
		CriteriaBuilder cb = mock(CriteriaBuilder.class);
		Root root = mock(Root.class);
		Path sharedPath = mock(Path.class);
		Predicate publishedPredicate = mock(Predicate.class);
		Predicate ownedPredicate = mock(Predicate.class);
		Predicate orPredicate = mock(Predicate.class);
		LessonVisibilityFilter visibility = new LessonVisibilityFilter(false, true, 7L);

		when(root.get((SingularAttribute) null)).thenReturn(sharedPath);
		when(cb.equal(null, 900L)).thenReturn(publishedPredicate);
		when(cb.equal(null, 7L)).thenReturn(ownedPredicate);
		when(cb.or(ownedPredicate, publishedPredicate)).thenReturn(orPredicate);

		// When:
		Predicate result = builder.visibilityPredicate(cb, root, visibility, 900L);

		// Then:
		assertThat(result).isSameAs(orPredicate);
		verify(cb).or(ownedPredicate, publishedPredicate);
	}

	// ------------------------------------------------------------------
	// enrolledByViewer()
	// ------------------------------------------------------------------

	@Test
	void shouldBuildEnrolledByViewerExistsSubqueryTest() {
		// Given:
		TagsFilterSupport tagsFilterSupport = mock(TagsFilterSupport.class);
		LessonSpecificationBuilder builder = new LessonSpecificationBuilder(tagsFilterSupport);

		CriteriaQuery query = mock(CriteriaQuery.class);
		CriteriaBuilder cb = mock(CriteriaBuilder.class);
		Root root = mock(Root.class);
		Subquery subquery = mock(Subquery.class);
		Root correlatedLesson = mock(Root.class);
		Root userLessonRoot = mock(Root.class);
		Expression literalOne = mock(Expression.class);
		Path lessonPath = mock(Path.class);
		Path userPath = mock(Path.class);
		Path userIdPath = mock(Path.class);
		Predicate lessonEqualPredicate = mock(Predicate.class);
		Predicate userIdEqualPredicate = mock(Predicate.class);
		Predicate existsPredicate = mock(Predicate.class);
		Long viewerId = 42L;

		when(query.subquery(Long.class)).thenReturn(subquery);
		when(subquery.correlate(root)).thenReturn(correlatedLesson);
		when(subquery.from(UserLesson.class)).thenReturn(userLessonRoot);
		when(cb.literal(1L)).thenReturn(literalOne);
		when(userLessonRoot.get((SingularAttribute) null)).thenReturn(lessonPath, userPath);
		when(userPath.get((SingularAttribute) null)).thenReturn(userIdPath);
		when(cb.equal(lessonPath, correlatedLesson)).thenReturn(lessonEqualPredicate);
		when(cb.equal(userIdPath, viewerId)).thenReturn(userIdEqualPredicate);
		when(cb.exists(subquery)).thenReturn(existsPredicate);

		// When:
		Predicate result = builder.enrolledByViewer(query, cb, root, viewerId);

		// Then:
		assertThat(result).isSameAs(existsPredicate);
		verify(subquery).select(literalOne);
		verify(subquery).where(lessonEqualPredicate, userIdEqualPredicate);
		verify(cb).exists(subquery);
	}

	// ------------------------------------------------------------------
	// hasActivityOfType()
	// ------------------------------------------------------------------

	@Test
	void shouldBuildHasActivityOfTypeExistsSubqueryTest() {
		// Given:
		TagsFilterSupport tagsFilterSupport = mock(TagsFilterSupport.class);
		LessonSpecificationBuilder builder = new LessonSpecificationBuilder(tagsFilterSupport);

		CriteriaQuery query = mock(CriteriaQuery.class);
		CriteriaBuilder cb = mock(CriteriaBuilder.class);
		Root root = mock(Root.class);
		Subquery subquery = mock(Subquery.class);
		Root correlatedLesson = mock(Root.class);
		Root lessonActivityRoot = mock(Root.class);
		Join typeJoin = mock(Join.class);
		Expression literalOne = mock(Expression.class);
		Path lessonPath = mock(Path.class);
		Path codePath = mock(Path.class);
		Predicate lessonEqualPredicate = mock(Predicate.class);
		Predicate codeEqualPredicate = mock(Predicate.class);
		Predicate existsPredicate = mock(Predicate.class);
		String activityTypeCode = "QUIZ";

		when(query.subquery(Long.class)).thenReturn(subquery);
		when(subquery.correlate(root)).thenReturn(correlatedLesson);
		when(subquery.from(LessonActivity.class)).thenReturn(lessonActivityRoot);
		when(lessonActivityRoot.join((SingularAttribute) null)).thenReturn(typeJoin);
		when(cb.literal(1L)).thenReturn(literalOne);
		when(lessonActivityRoot.get((SingularAttribute) null)).thenReturn(lessonPath);
		when(typeJoin.get((SingularAttribute) null)).thenReturn(codePath);
		when(cb.equal(lessonPath, correlatedLesson)).thenReturn(lessonEqualPredicate);
		when(cb.equal(codePath, activityTypeCode)).thenReturn(codeEqualPredicate);
		when(cb.exists(subquery)).thenReturn(existsPredicate);

		// When:
		Predicate result = builder.hasActivityOfType(query, cb, root, activityTypeCode);

		// Then:
		assertThat(result).isSameAs(existsPredicate);
		verify(subquery).select(literalOne);
		verify(subquery).where(lessonEqualPredicate, codeEqualPredicate);
		verify(cb).exists(subquery);
	}

	// ------------------------------------------------------------------
	// hasAnyActivity()
	// ------------------------------------------------------------------

	@Test
	void shouldBuildHasAnyActivityExistsPredicateWhenExpectedTrueTest() {
		// Given:
		TagsFilterSupport tagsFilterSupport = mock(TagsFilterSupport.class);
		LessonSpecificationBuilder builder = new LessonSpecificationBuilder(tagsFilterSupport);

		CriteriaQuery query = mock(CriteriaQuery.class);
		CriteriaBuilder cb = mock(CriteriaBuilder.class);
		Root root = mock(Root.class);
		Subquery subquery = mock(Subquery.class);
		Root correlatedLesson = mock(Root.class);
		Root lessonActivityRoot = mock(Root.class);
		Expression literalOne = mock(Expression.class);
		Predicate existsPredicate = mock(Predicate.class);

		when(query.subquery(Long.class)).thenReturn(subquery);
		when(subquery.correlate(root)).thenReturn(correlatedLesson);
		when(subquery.from(LessonActivity.class)).thenReturn(lessonActivityRoot);
		when(cb.literal(1L)).thenReturn(literalOne);
		when(cb.exists(subquery)).thenReturn(existsPredicate);

		// When:
		Predicate result = builder.hasAnyActivity(query, cb, root, true);

		// Then:
		assertThat(result).isSameAs(existsPredicate);
		verify(cb, never()).not(existsPredicate);
	}

	@Test
	void shouldBuildHasAnyActivityNotExistsPredicateWhenExpectedFalseTest() {
		// Given:
		TagsFilterSupport tagsFilterSupport = mock(TagsFilterSupport.class);
		LessonSpecificationBuilder builder = new LessonSpecificationBuilder(tagsFilterSupport);

		CriteriaQuery query = mock(CriteriaQuery.class);
		CriteriaBuilder cb = mock(CriteriaBuilder.class);
		Root root = mock(Root.class);
		Subquery subquery = mock(Subquery.class);
		Root correlatedLesson = mock(Root.class);
		Root lessonActivityRoot = mock(Root.class);
		Expression literalOne = mock(Expression.class);
		Predicate existsPredicate = mock(Predicate.class);
		Predicate notExistsPredicate = mock(Predicate.class);

		when(query.subquery(Long.class)).thenReturn(subquery);
		when(subquery.correlate(root)).thenReturn(correlatedLesson);
		when(subquery.from(LessonActivity.class)).thenReturn(lessonActivityRoot);
		when(cb.literal(1L)).thenReturn(literalOne);
		when(cb.exists(subquery)).thenReturn(existsPredicate);
		when(cb.not(existsPredicate)).thenReturn(notExistsPredicate);

		// When:
		Predicate result = builder.hasAnyActivity(query, cb, root, false);

		// Then:
		assertThat(result).isSameAs(notExistsPredicate);
		verify(cb).not(existsPredicate);
	}

	// ------------------------------------------------------------------
	// jsonbContains()
	// ------------------------------------------------------------------

	@Test
	void shouldBuildJsonbContainsFunctionExpressionTest() {
		// Given:
		TagsFilterSupport tagsFilterSupport = mock(TagsFilterSupport.class);
		LessonSpecificationBuilder builder = new LessonSpecificationBuilder(tagsFilterSupport);
		CriteriaBuilder cb = mock(CriteriaBuilder.class);
		Expression jsonColumn = mock(Expression.class);
		Expression literalExpr = mock(Expression.class);
		Expression jsonbCastExpr = mock(Expression.class);
		Expression containsExpr = mock(Expression.class);
		String containmentJson = "[\"design\"]";

		when(cb.literal(containmentJson)).thenReturn(literalExpr);
		when(cb.function("jsonb", Object.class, literalExpr)).thenReturn(jsonbCastExpr);
		when(cb.function("jsonb_contains", Boolean.class, jsonColumn, jsonbCastExpr)).thenReturn(containsExpr);

		// When:
		Expression<Boolean> result = builder.jsonbContains(cb, jsonColumn, containmentJson);

		// Then:
		assertThat(result).isSameAs(containsExpr);
		verify(cb).function("jsonb", Object.class, literalExpr);
		verify(cb).function("jsonb_contains", Boolean.class, jsonColumn, jsonbCastExpr);
	}

	// ------------------------------------------------------------------
	// tagsContain()
	// ------------------------------------------------------------------

	@Test
	void shouldBuildTagsContainFunctionExpressionTest() {
		// Given:
		TagsFilterSupport tagsFilterSupport = mock(TagsFilterSupport.class);
		LessonSpecificationBuilder builder = new LessonSpecificationBuilder(tagsFilterSupport);
		CriteriaBuilder cb = mock(CriteriaBuilder.class);
		Expression tagsColumn = mock(Expression.class);
		Expression literalExpr = mock(Expression.class);
		Expression functionExpr = mock(Expression.class);
		String searchText = "design";

		when(cb.literal(searchText)).thenReturn(literalExpr);
		when(cb.function("jsonb_array_contains_ci", Boolean.class, tagsColumn, literalExpr)).thenReturn(functionExpr);

		// When:
		Expression<Boolean> result = builder.tagsContain(cb, tagsColumn, searchText);

		// Then:
		assertThat(result).isSameAs(functionExpr);
		verify(cb).function("jsonb_array_contains_ci", Boolean.class, tagsColumn, literalExpr);
	}
}
