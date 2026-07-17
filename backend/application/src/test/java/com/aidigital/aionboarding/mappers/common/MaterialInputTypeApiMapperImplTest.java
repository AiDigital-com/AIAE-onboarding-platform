package com.aidigital.aionboarding.mappers.common;

import com.aidigital.aionboarding.api.v1.model.MaterialInputTypeV1;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MaterialInputTypeApiMapperImplTest {

	private final MaterialInputTypeApiMapperImpl materialInputTypeApiMapperImpl = new MaterialInputTypeApiMapperImpl();

	@Test
	void shouldMapMaterialInputTypeStringTest() {
		// Given:
		String type = "value";

		// When:
		MaterialInputTypeV1 actualResult = materialInputTypeApiMapperImpl.mapMaterialInputType(type);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldFromMaterialInputTypeMaterialInputTypeV1Test() {
		// Given:
		MaterialInputTypeV1 type = Instancio.create(MaterialInputTypeV1.class);

		// When:
		String actualResult = materialInputTypeApiMapperImpl.fromMaterialInputType(type);

		// Then:
		assertThat(actualResult).isNotNull();
	}

}