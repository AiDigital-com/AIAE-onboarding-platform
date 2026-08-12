package com.aidigital.aionboarding.mappers.material;

import com.aidigital.aionboarding.api.v1.model.MaterialSortFieldV1;
import com.aidigital.aionboarding.api.v1.model.SearchMaterialsV1;
import com.aidigital.aionboarding.api.v1.model.SortDirectionV1;
import com.aidigital.aionboarding.service.material.models.MaterialListQuery;
import com.aidigital.aionboarding.service.material.models.MaterialSearchSummaryRecord;
import com.aidigital.aionboarding.service.material.models.MaterialSortField;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises {@link MaterialApiMapper} default methods directly against the generated
 * implementation, since MapStruct default methods are otherwise mocked out (returning
 * canned values) everywhere else in the test suite.
 */
class MaterialApiMapperTest {

	private final MaterialApiMapper mapper = new MaterialApiMapperImpl();

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
			SearchMaterialsV1 request = new SearchMaterialsV1().page(2).size(30);

			// When / Then:
			assertThat(mapper.page(request)).isEqualTo(2);
			assertThat(mapper.size(request)).isEqualTo(30);
		}
	}

	@Nested
	class MaterialSortFieldAndDirection {

		@Test
		void shouldMapEverySortFieldTest() {
			// When / Then:
			assertThat(mapper.materialSortField(new SearchMaterialsV1().sort(MaterialSortFieldV1.CREATED_AT)))
					.isEqualTo(MaterialSortField.CREATED_AT);
			assertThat(mapper.materialSortField(new SearchMaterialsV1().sort(MaterialSortFieldV1.UPDATED_AT)))
					.isEqualTo(MaterialSortField.UPDATED_AT);
			assertThat(mapper.materialSortField(new SearchMaterialsV1().sort(MaterialSortFieldV1.TITLE)))
					.isEqualTo(MaterialSortField.TITLE);
			assertThat(mapper.materialSortField(new SearchMaterialsV1().sort(MaterialSortFieldV1.USAGE_COUNT)))
					.isEqualTo(MaterialSortField.USAGE_COUNT);
			assertThat(mapper.materialSortField(null)).isEqualTo(MaterialSortField.CREATED_AT);
			assertThat(mapper.materialSortField(new SearchMaterialsV1())).isEqualTo(MaterialSortField.CREATED_AT);
		}

		@Test
		void shouldMapSortDirectionTest() {
			// When / Then:
			assertThat(mapper.sortDirection(new SearchMaterialsV1().direction(SortDirectionV1.ASC)))
					.isEqualTo(Sort.Direction.ASC);
			assertThat(mapper.sortDirection(new SearchMaterialsV1().direction(SortDirectionV1.DESC)))
					.isEqualTo(Sort.Direction.DESC);
			assertThat(mapper.sortDirection(null)).isEqualTo(Sort.Direction.DESC);
		}
	}

	@Nested
	class ToMaterialListQuery {

		@Test
		void shouldReturnNullFieldsForANullRequestTest() {
			// When:
			MaterialListQuery query = mapper.toMaterialListQuery(null);

			// Then:
			assertThat(query.searchText()).isNull();
			assertThat(query.sortField()).isEqualTo(MaterialSortField.CREATED_AT);
		}

		@Test
		void shouldCarryEveryFieldFromAPopulatedRequestTest() {
			// Given:
			SearchMaterialsV1 request = new SearchMaterialsV1()
					.query("notes")
					.createdByUserId(9L)
					.hasAttachments(true)
					.hasYoutube(true)
					.hasLinks(true)
					.sort(MaterialSortFieldV1.TITLE)
					.direction(SortDirectionV1.ASC);

			// When:
			MaterialListQuery query = mapper.toMaterialListQuery(request);

			// Then:
			assertThat(query.searchText()).isEqualTo("notes");
			assertThat(query.createdByUserId()).isEqualTo(9L);
			assertThat(query.hasAttachments()).isTrue();
			assertThat(query.hasYoutube()).isTrue();
			assertThat(query.hasLinks()).isTrue();
			assertThat(query.sortField()).isEqualTo(MaterialSortField.TITLE);
			assertThat(query.direction()).isEqualTo(Sort.Direction.ASC);
		}
	}

	@Nested
	class ToMaterialsListResponseV1 {

		@Test
		void shouldReturnNullForANullPageTest() {
			// When:
			var result = mapper.toMaterialsListResponseV1(null);

			// Then:
			assertThat(result).isNull();
		}

		@Test
		void shouldBuildAnEmptyResponseForAnEmptyPageTest() {
			// Given:
			Page<MaterialSearchSummaryRecord> page = new PageImpl<>(List.of());

			// When:
			var result = mapper.toMaterialsListResponseV1(page);

			// Then:
			assertThat(result.getMaterials()).isEmpty();
			assertThat(result.getPage().getTotalElements()).isZero();
		}
	}
}
