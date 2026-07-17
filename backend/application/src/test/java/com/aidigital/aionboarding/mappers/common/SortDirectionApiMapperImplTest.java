package com.aidigital.aionboarding.mappers.common;

import com.aidigital.aionboarding.api.v1.model.SortDirectionV1;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

class SortDirectionApiMapperImplTest {

	private final SortDirectionApiMapperImpl sortDirectionApiMapperImpl = new SortDirectionApiMapperImpl();

	@Test
	void shouldMapAscToAscTest() {
		// When:
		Sort.Direction actualResult = sortDirectionApiMapperImpl.toSortDirection(SortDirectionV1.ASC);

		// Then:
		assertThat(actualResult).isEqualTo(Sort.Direction.ASC);
	}

	@Test
	void shouldMapDescToDescTest() {
		// When:
		Sort.Direction actualResult = sortDirectionApiMapperImpl.toSortDirection(SortDirectionV1.DESC);

		// Then:
		assertThat(actualResult).isEqualTo(Sort.Direction.DESC);
	}

	@Test
	void shouldDefaultToDescForNullTest() {
		// When:
		Sort.Direction actualResult = sortDirectionApiMapperImpl.toSortDirection(null);

		// Then:
		assertThat(actualResult).isEqualTo(Sort.Direction.DESC);
	}
}