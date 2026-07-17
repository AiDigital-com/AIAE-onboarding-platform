package com.aidigital.aionboarding.mappers.common;

import com.aidigital.aionboarding.api.v1.model.ProgressStatusV1;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProgressStatusApiMapperImplTest {

	private final ProgressStatusApiMapperImpl progressStatusApiMapperImpl = new ProgressStatusApiMapperImpl();

	@Test
	void shouldMapProgressStatusStringTest() {
		// Given:
		String status = "value";

		// When:
		ProgressStatusV1 actualResult = progressStatusApiMapperImpl.mapProgressStatus(status);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldFromProgressStatusProgressStatusV1Test() {
		// Given:
		ProgressStatusV1 status = Instancio.create(ProgressStatusV1.class);

		// When:
		String actualResult = progressStatusApiMapperImpl.fromProgressStatus(status);

		// Then:
		assertThat(actualResult).isNotNull();
	}

}