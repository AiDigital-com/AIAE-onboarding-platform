package com.aidigital.aionboarding.service.material.support;

import com.aidigital.aionboarding.domain.lesson.entities.LessonMaterial;
import com.aidigital.aionboarding.domain.material.entities.Material;
import com.aidigital.aionboarding.domain.material.entities.MaterialFile;
import com.aidigital.aionboarding.domain.material.entities.MaterialLink;
import com.aidigital.aionboarding.domain.material.entities.MaterialYoutubeUrl;
import com.aidigital.aionboarding.domain.material.entities.Material_;
import com.aidigital.aionboarding.service.common.mapping.TagsFilterSupport;
import com.aidigital.aionboarding.service.material.models.MaterialListQuery;
import com.aidigital.aionboarding.service.material.models.MaterialSortField;
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
class MaterialSpecificationBuilderTest {

	@Mock
	private TagsFilterSupport tagsFilterSupport;

	@Test
	void shouldBuildPredicateWithAllFiltersForRowQueryTest() {
		// Given:
		MaterialSpecificationBuilder builder = new MaterialSpecificationBuilder(tagsFilterSupport);
		MaterialListQuery filter = new MaterialListQuery(
				"Search",
				List.of("tag1", "tag2"),
				5L,
				true,
				false,
				true,
				MaterialSortField.USAGE_COUNT,
				Sort.Direction.DESC
		);
		when(tagsFilterSupport.toContainmentJson(filter.tags())).thenReturn("[\"tag1\",\"tag2\"]");

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
		when(cb.literal("[\"tag1\",\"tag2\"]")).thenReturn(containmentLiteral);
		when(cb.function("jsonb", Object.class, containmentLiteral)).thenReturn(jsonbCastExpr);
		when(cb.function("jsonb_contains", Boolean.class, sharedPath, jsonbCastExpr)).thenReturn(jsonbContainsExpr);
		when(cb.isTrue(jsonbContainsExpr)).thenReturn(jsonbContainsPredicate);

		// createdByUserId: root.get(createdByUser).get(id) - the first hop resolves to sharedPath;
		// the second hop is invoked on that mock without further stubbing and returns Mockito's
		// default null, which is the literal argument cb.equal(...) receives here.
		Predicate createdByUserPredicate = mock(Predicate.class);
		when(cb.equal(null, 5L)).thenReturn(createdByUserPredicate);

		Expression literalOne = mock(Expression.class);
		when(cb.literal(1L)).thenReturn(literalOne);

		Subquery attachmentSubquery = mock(Subquery.class);
		Subquery youtubeSubquery = mock(Subquery.class);
		Subquery linkSubquery = mock(Subquery.class);
		Subquery usageSubquery = mock(Subquery.class);
		when(query.subquery(Long.class)).thenReturn(attachmentSubquery, youtubeSubquery, linkSubquery, usageSubquery);

		Root correlatedForAttachment = mock(Root.class);
		Root materialFileRoot = mock(Root.class);
		Predicate attachmentEqualPredicate = mock(Predicate.class);
		Predicate attachmentExists = mock(Predicate.class);
		when(attachmentSubquery.correlate(root)).thenReturn(correlatedForAttachment);
		when(attachmentSubquery.from(MaterialFile.class)).thenReturn(materialFileRoot);
		when(attachmentSubquery.select(literalOne)).thenReturn(attachmentSubquery);
		when(cb.equal(null, correlatedForAttachment)).thenReturn(attachmentEqualPredicate);
		when(attachmentSubquery.where(attachmentEqualPredicate)).thenReturn(attachmentSubquery);
		when(cb.exists(attachmentSubquery)).thenReturn(attachmentExists);

		Root correlatedForYoutube = mock(Root.class);
		Root youtubeRoot = mock(Root.class);
		Predicate youtubeEqualPredicate = mock(Predicate.class);
		Predicate youtubeExists = mock(Predicate.class);
		Predicate youtubeNotExists = mock(Predicate.class);
		when(youtubeSubquery.correlate(root)).thenReturn(correlatedForYoutube);
		when(youtubeSubquery.from(MaterialYoutubeUrl.class)).thenReturn(youtubeRoot);
		when(youtubeSubquery.select(literalOne)).thenReturn(youtubeSubquery);
		when(cb.equal(null, correlatedForYoutube)).thenReturn(youtubeEqualPredicate);
		when(youtubeSubquery.where(youtubeEqualPredicate)).thenReturn(youtubeSubquery);
		when(cb.exists(youtubeSubquery)).thenReturn(youtubeExists);
		when(cb.not(youtubeExists)).thenReturn(youtubeNotExists);

		Root correlatedForLink = mock(Root.class);
		Root linkRoot = mock(Root.class);
		Predicate linkEqualPredicate = mock(Predicate.class);
		Predicate linkExists = mock(Predicate.class);
		when(linkSubquery.correlate(root)).thenReturn(correlatedForLink);
		when(linkSubquery.from(MaterialLink.class)).thenReturn(linkRoot);
		when(linkSubquery.select(literalOne)).thenReturn(linkSubquery);
		when(cb.equal(null, correlatedForLink)).thenReturn(linkEqualPredicate);
		when(linkSubquery.where(linkEqualPredicate)).thenReturn(linkSubquery);
		when(cb.exists(linkSubquery)).thenReturn(linkExists);

		Root correlatedForUsage = mock(Root.class);
		Root lessonMaterialRoot = mock(Root.class);
		Expression countExpr = mock(Expression.class);
		Predicate usageEqualPredicate = mock(Predicate.class);
		Order usageDescOrder = mock(Order.class);
		when(usageSubquery.correlate(root)).thenReturn(correlatedForUsage);
		when(usageSubquery.from(LessonMaterial.class)).thenReturn(lessonMaterialRoot);
		when(cb.count(lessonMaterialRoot)).thenReturn(countExpr);
		when(usageSubquery.select(countExpr)).thenReturn(usageSubquery);
		when(cb.equal(null, correlatedForUsage)).thenReturn(usageEqualPredicate);
		when(usageSubquery.where(usageEqualPredicate)).thenReturn(usageSubquery);
		when(cb.desc(usageSubquery)).thenReturn(usageDescOrder);

		when(query.getResultType()).thenReturn(Material.class);

		Predicate finalAndPredicate = mock(Predicate.class);
		when(cb.and(searchOrPredicate, jsonbContainsPredicate, createdByUserPredicate, attachmentExists,
				youtubeNotExists, linkExists)).thenReturn(finalAndPredicate);

		// When:
		Specification<Material> spec = builder.build(filter);
		Predicate result = spec.toPredicate(root, query, cb);

		// Then:
		assertThat(result).isSameAs(finalAndPredicate);
		verify(query).orderBy(usageDescOrder);
	}

	@Test
	void shouldSkipSortForCountQueryTest() {
		// Given:
		MaterialSpecificationBuilder builder = new MaterialSpecificationBuilder(tagsFilterSupport);
		MaterialListQuery filter = new MaterialListQuery(
				null, null, null, null, null, null, MaterialSortField.CREATED_AT, Sort.Direction.ASC
		);

		CriteriaBuilder cb = mock(CriteriaBuilder.class);
		CriteriaQuery<Long> query = mock(CriteriaQuery.class);
		Root<Material> root = mock(Root.class);
		when(query.getResultType()).thenReturn(Long.class);

		Predicate finalPredicate = mock(Predicate.class);
		when(cb.and()).thenReturn(finalPredicate);

		// When:
		Specification<Material> spec = builder.build(filter);
		Predicate result = spec.toPredicate(root, query, cb);

		// Then:
		assertThat(result).isSameAs(finalPredicate);
	}

	@Test
	void shouldBuildOrderByCreatedAtAscendingTest() {
		// Given:
		MaterialSpecificationBuilder builder = new MaterialSpecificationBuilder(tagsFilterSupport);
		MaterialListQuery filter = new MaterialListQuery(
				null, null, null, null, null, null, MaterialSortField.CREATED_AT, Sort.Direction.ASC
		);

		CriteriaBuilder cb = mock(CriteriaBuilder.class);
		CriteriaQuery<Material> query = mock(CriteriaQuery.class);
		Root<Material> root = mock(Root.class);
		Path<LocalDateTime> createdAtPath = mock(Path.class);
		when(root.get(Material_.createdAt)).thenReturn(createdAtPath);
		Order createdAtOrder = mock(Order.class);
		when(cb.asc(createdAtPath)).thenReturn(createdAtOrder);
		when(query.getResultType()).thenReturn(Material.class);
		Predicate finalPredicate = mock(Predicate.class);
		when(cb.and()).thenReturn(finalPredicate);

		// When:
		Specification<Material> spec = builder.build(filter);
		spec.toPredicate(root, query, cb);

		// Then:
		verify(query).orderBy(createdAtOrder);
	}

	@Test
	void shouldBuildOrderByUpdatedAtDescendingTest() {
		// Given:
		MaterialSpecificationBuilder builder = new MaterialSpecificationBuilder(tagsFilterSupport);
		MaterialListQuery filter = new MaterialListQuery(
				null, null, null, null, null, null, MaterialSortField.UPDATED_AT, Sort.Direction.DESC
		);

		CriteriaBuilder cb = mock(CriteriaBuilder.class);
		CriteriaQuery<Material> query = mock(CriteriaQuery.class);
		Root<Material> root = mock(Root.class);
		Path<LocalDateTime> updatedAtPath = mock(Path.class);
		when(root.get(Material_.updatedAt)).thenReturn(updatedAtPath);
		Order updatedAtOrder = mock(Order.class);
		when(cb.desc(updatedAtPath)).thenReturn(updatedAtOrder);
		when(query.getResultType()).thenReturn(Material.class);
		Predicate finalPredicate = mock(Predicate.class);
		when(cb.and()).thenReturn(finalPredicate);

		// When:
		Specification<Material> spec = builder.build(filter);
		spec.toPredicate(root, query, cb);

		// Then:
		verify(query).orderBy(updatedAtOrder);
	}

	@Test
	void shouldBuildOrderByTitleAscendingTest() {
		// Given:
		MaterialSpecificationBuilder builder = new MaterialSpecificationBuilder(tagsFilterSupport);
		MaterialListQuery filter = new MaterialListQuery(
				null, null, null, null, null, null, MaterialSortField.TITLE, Sort.Direction.ASC
		);

		CriteriaBuilder cb = mock(CriteriaBuilder.class);
		CriteriaQuery<Material> query = mock(CriteriaQuery.class);
		Root<Material> root = mock(Root.class);
		Path<String> titlePath = mock(Path.class);
		when(root.get(Material_.title)).thenReturn(titlePath);
		Order titleOrder = mock(Order.class);
		when(cb.asc(titlePath)).thenReturn(titleOrder);
		when(query.getResultType()).thenReturn(Material.class);
		Predicate finalPredicate = mock(Predicate.class);
		when(cb.and()).thenReturn(finalPredicate);

		// When:
		Specification<Material> spec = builder.build(filter);
		spec.toPredicate(root, query, cb);

		// Then:
		verify(query).orderBy(titleOrder);
	}
}
