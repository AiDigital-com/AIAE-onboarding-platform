package com.aidigital.aionboarding.mappers.common;

import com.aidigital.aionboarding.api.v1.model.CountResponseV1;
import com.aidigital.aionboarding.api.v1.model.PageInfoV1;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PageInfoApiMapperImplTest {

	private final PageInfoApiMapperImpl pageInfoApiMapperImpl = new PageInfoApiMapperImpl();

	@Test
	void shouldToPageInfoV1PageTest() {
		// Given:
		Page<?> page = mock(Page.class);
		when(page.getNumber()).thenReturn(0);
		when(page.getSize()).thenReturn(10);
		when(page.getTotalElements()).thenReturn(100L);
		when(page.getTotalPages()).thenReturn(10);
		when(page.hasNext()).thenReturn(true);
		when(page.hasPrevious()).thenReturn(false);

		// When:
		PageInfoV1 actualResult = pageInfoApiMapperImpl.toPageInfoV1(page);

		// Then:
		assertThat(actualResult).isNotNull();
		assertThat(actualResult.getPage()).isZero();
	}


	@Test
	void shouldToCountResponseV1longTest() {
		// Given:
		long totalElements = 5L;

		// When:
		CountResponseV1 actualResult = pageInfoApiMapperImpl.toCountResponseV1(totalElements);

		// Then:
		assertThat(actualResult).isNotNull();
		assertThat(actualResult.getTotalElements()).isEqualTo(5L);
	}

}