package com.aidigital.aionboarding.mappers.common;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises {@link PageInfoApiMapper} default methods directly against the generated
 * implementation, since MapStruct default methods are otherwise mocked out (returning
 * canned values) everywhere else in the test suite.
 */
class PageInfoApiMapperTest {

	private final PageInfoApiMapper mapper = new PageInfoApiMapperImpl();

	@Nested
	class ToPageInfoV1 {

		@Test
		void shouldReturnNullForANullPageTest() {
			// When:
			var result = mapper.toPageInfoV1(null);

			// Then:
			assertThat(result).isNull();
		}

		@Test
		void shouldExposeHasNextWhenMorePagesRemainTest() {
			// Given: 3 items, page size 1, on the first page — a second page exists
			var page = new PageImpl<>(List.of("a"), PageRequest.of(0, 1), 3);

			// When:
			var result = mapper.toPageInfoV1(page);

			// Then:
			assertThat(result.getPage()).isZero();
			assertThat(result.getSize()).isEqualTo(1);
			assertThat(result.getTotalElements()).isEqualTo(3L);
			assertThat(result.getTotalPages()).isEqualTo(3);
			assertThat(result.getHasNext()).isTrue();
			assertThat(result.getHasPrevious()).isFalse();
		}

		@Test
		void shouldExposeHasPreviousOnALaterPageTest() {
			// Given: 3 items, page size 1, on the last page — no next page, but a previous one
			var page = new PageImpl<>(List.of("c"), PageRequest.of(2, 1), 3);

			// When:
			var result = mapper.toPageInfoV1(page);

			// Then:
			assertThat(result.getHasNext()).isFalse();
			assertThat(result.getHasPrevious()).isTrue();
		}
	}

	@Nested
	class ToCountResponseV1 {

		@Test
		void shouldReturnNullForANullCountTest() {
			// When:
			var result = mapper.toCountResponseV1(null);

			// Then:
			assertThat(result).isNull();
		}

		@Test
		void shouldWrapTheCountTest() {
			// When:
			var result = mapper.toCountResponseV1(42L);

			// Then:
			assertThat(result.getTotalElements()).isEqualTo(42L);
		}
	}
}
