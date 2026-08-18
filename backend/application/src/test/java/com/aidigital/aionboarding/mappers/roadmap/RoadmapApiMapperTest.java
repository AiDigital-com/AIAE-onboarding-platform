package com.aidigital.aionboarding.mappers.roadmap;

import com.aidigital.aionboarding.api.v1.model.RoadmapSortFieldV1;
import com.aidigital.aionboarding.api.v1.model.SearchRoadmapsV1;
import com.aidigital.aionboarding.api.v1.model.SortDirectionV1;
import com.aidigital.aionboarding.api.v1.model.UpdateRoadmapRequestV1;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapListQuery;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapRecord;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapSortField;
import com.aidigital.aionboarding.service.roadmap.models.UpdateRoadmapInput;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises {@link RoadmapApiMapper} default methods directly against the generated
 * implementation, since MapStruct default methods are otherwise mocked out (returning
 * canned values) everywhere else in the test suite.
 */
class RoadmapApiMapperTest {

	private final RoadmapApiMapper mapper = new RoadmapApiMapperImpl();

	@Nested
	class PageAndSize {

		@Test
		void shouldDefaultWhenRequestIsNullTest() {
			// When / Then:
			assertThat(mapper.page(null)).isZero();
			assertThat(mapper.size(null)).isEqualTo(20);
		}

		@Test
		void shouldUseRequestedValuesWhenPresentTest() {
			// Given:
			SearchRoadmapsV1 request = new SearchRoadmapsV1().page(1).size(40);

			// When / Then:
			assertThat(mapper.page(request)).isEqualTo(1);
			assertThat(mapper.size(request)).isEqualTo(40);
		}
	}

	@Nested
	class RoadmapSortFieldAndDirection {

		@Test
		void shouldMapEverySortFieldTest() {
			// When / Then:
			assertThat(mapper.roadmapSortField(new SearchRoadmapsV1().sort(RoadmapSortFieldV1.CREATED_AT)))
					.isEqualTo(RoadmapSortField.CREATED_AT);
			assertThat(mapper.roadmapSortField(new SearchRoadmapsV1().sort(RoadmapSortFieldV1.UPDATED_AT)))
					.isEqualTo(RoadmapSortField.UPDATED_AT);
			assertThat(mapper.roadmapSortField(new SearchRoadmapsV1().sort(RoadmapSortFieldV1.TITLE)))
					.isEqualTo(RoadmapSortField.TITLE);
			assertThat(mapper.roadmapSortField(null)).isEqualTo(RoadmapSortField.CREATED_AT);
			assertThat(mapper.roadmapSortField(new SearchRoadmapsV1())).isEqualTo(RoadmapSortField.CREATED_AT);
		}

		@Test
		void shouldMapSortDirectionTest() {
			// When / Then:
			assertThat(mapper.sortDirection(new SearchRoadmapsV1().direction(SortDirectionV1.ASC)))
					.isEqualTo(Sort.Direction.ASC);
			assertThat(mapper.sortDirection(new SearchRoadmapsV1().direction(SortDirectionV1.DESC)))
					.isEqualTo(Sort.Direction.DESC);
			assertThat(mapper.sortDirection(null)).isEqualTo(Sort.Direction.DESC);
		}
	}

	@Nested
	class ToRoadmapListQuery {

		@Test
		void shouldReturnNullFieldsForANullRequestTest() {
			// When:
			RoadmapListQuery query = mapper.toRoadmapListQuery(null);

			// Then:
			assertThat(query.searchText()).isNull();
			assertThat(query.sortField()).isEqualTo(RoadmapSortField.CREATED_AT);
		}

		@Test
		void shouldCarryEveryFieldFromAPopulatedRequestTest() {
			// Given:
			SearchRoadmapsV1 request = new SearchRoadmapsV1()
					.query("path")
					.createdByUserId(3L)
					.assignedToMe(true)
					.sort(RoadmapSortFieldV1.TITLE)
					.direction(SortDirectionV1.ASC);

			// When:
			RoadmapListQuery query = mapper.toRoadmapListQuery(request);

			// Then:
			assertThat(query.searchText()).isEqualTo("path");
			assertThat(query.createdByUserId()).isEqualTo(3L);
			assertThat(query.assignedToMe()).isTrue();
			assertThat(query.sortField()).isEqualTo(RoadmapSortField.TITLE);
			assertThat(query.direction()).isEqualTo(Sort.Direction.ASC);
		}
	}

	@Nested
	class ToRoadmapsListResponseV1 {

		@Test
		void shouldReturnNullForANullPageTest() {
			// When:
			var result = mapper.toRoadmapsListResponseV1(null);

			// Then:
			assertThat(result).isNull();
		}

		@Test
		void shouldBuildAnEmptyResponseForAnEmptyPageTest() {
			// Given:
			Page<RoadmapRecord> page = new PageImpl<>(List.of());

			// When:
			var result = mapper.toRoadmapsListResponseV1(page);

			// Then:
			assertThat(result.getRoadmaps()).isEmpty();
			assertThat(result.getPage().getTotalElements()).isZero();
		}
	}

	@Nested
	class ToUpdateRoadmapInput {

		@Test
		void shouldWrapEveryPresentFieldTest() {
			// Given:
			UpdateRoadmapRequestV1 request = new UpdateRoadmapRequestV1()
					.title("New title")
					.description("New description")
					.lessonIds(List.of(1L, 2L))
					.tags(List.of("a", "b"));

			// When:
			UpdateRoadmapInput input = mapper.toUpdateRoadmapInput(request);

			// Then:
			assertThat(input.title()).contains("New title");
			assertThat(input.description()).contains("New description");
			assertThat(input.lessonIds()).isPresent();
			assertThat(input.tags()).isPresent();
		}

		@Test
		void shouldReturnEmptyOptionalsForAllAbsentFieldsTest() {
			// Given:
			UpdateRoadmapRequestV1 request = new UpdateRoadmapRequestV1();

			// When:
			UpdateRoadmapInput input = mapper.toUpdateRoadmapInput(request);

			// Then:
			assertThat(input.title()).isEmpty();
			assertThat(input.description()).isEmpty();
		}
	}
}
