package com.aidigital.aionboarding.support;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;

class PaginationSupportTest {

	private final PaginationSupport support = new PaginationSupport();

	@Nested
	class Unclamped {

		@Test
		void shouldDefaultPageAndSizeWhenNullTest() {
			// When / Then:
			assertThat(support.page(null)).isZero();
			assertThat(support.size(null)).isEqualTo(20);
		}

		@Test
		void shouldNotClampAnOutOfRangeSizeTest() {
			// When / Then: unclamped endpoints trust the caller's size, unlike the clamped ones
			assertThat(support.size(500)).isEqualTo(500);
			assertThat(support.page(-5)).isEqualTo(-5);
		}
	}

	@Nested
	class Clamped {

		@Test
		void shouldClampNullAndOutOfRangeInputsTest() {
			// When / Then: negative/null page normalizes to 0, size clamps into [1, 100]
			assertThat(support.normalizedPageIndex(null)).isZero();
			assertThat(support.normalizedPageSize(null)).isEqualTo(20);
			assertThat(support.normalizedPageIndex(-5)).isZero();
			assertThat(support.normalizedPageSize(500)).isEqualTo(100);
			assertThat(support.normalizedPageSize(0)).isEqualTo(1);
		}
	}

	@Nested
	class PageableBuilders {

		@Test
		void unsortedPageableShouldClampNullAndOutOfRangeInputsTest() {
			// When / Then:
			assertThat(support.unsortedPageable(null, null).getPageNumber()).isEqualTo(0);
			assertThat(support.unsortedPageable(null, null).getPageSize()).isEqualTo(20);
			assertThat(support.unsortedPageable(-5, 500).getPageNumber()).isEqualTo(0);
			assertThat(support.unsortedPageable(-5, 500).getPageSize()).isEqualTo(100);
			assertThat(support.unsortedPageable(3, 0).getPageSize()).isEqualTo(1);
			assertThat(support.unsortedPageable(3, 10).getSort().isUnsorted()).isTrue();
		}

		@Test
		void sortedByNameEmailShouldSortByNameThenEmailCaseInsensitivelyTest() {
			// When:
			Pageable pageable = support.sortedByNameEmail(2, 15);

			// Then:
			assertThat(pageable.getPageNumber()).isEqualTo(2);
			assertThat(pageable.getPageSize()).isEqualTo(15);
			assertThat(pageable.getSort().getOrderFor("name")).isNotNull();
			assertThat(pageable.getSort().getOrderFor("email")).isNotNull();
		}
	}
}
