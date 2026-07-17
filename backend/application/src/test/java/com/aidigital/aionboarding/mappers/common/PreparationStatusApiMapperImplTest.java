package com.aidigital.aionboarding.mappers.common;

import com.aidigital.aionboarding.api.v1.model.PreparationStatusV1;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PreparationStatusApiMapperImplTest {

	private final PreparationStatusApiMapperImpl preparationStatusApiMapperImpl = new PreparationStatusApiMapperImpl();

	@Test
	void shouldMapPreparationStatusStringTest() {
		// Given:
		String status = "value";

		// When:
		PreparationStatusV1 actualResult = preparationStatusApiMapperImpl.mapPreparationStatus(status);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldFromPreparationStatusPreparationStatusV1Test() {
		// Given:
		PreparationStatusV1 status = Instancio.create(PreparationStatusV1.class);

		// When:
		String actualResult = preparationStatusApiMapperImpl.fromPreparationStatus(status);

		// Then:
		assertThat(actualResult).isNotNull();
	}

}