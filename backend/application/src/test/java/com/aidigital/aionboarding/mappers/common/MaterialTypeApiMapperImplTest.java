package com.aidigital.aionboarding.mappers.common;

import com.aidigital.aionboarding.api.v1.model.MaterialTypeV1;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MaterialTypeApiMapperImplTest {

	private final MaterialTypeApiMapperImpl materialTypeApiMapperImpl = new MaterialTypeApiMapperImpl();

	@Test
	void shouldMapMaterialTypeStringTest() {
		// Given:
		String type = "value";

		// When:
		MaterialTypeV1 actualResult = materialTypeApiMapperImpl.mapMaterialType(type);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldFromMaterialTypeMaterialTypeV1Test() {
		// Given:
		MaterialTypeV1 type = Instancio.create(MaterialTypeV1.class);

		// When:
		String actualResult = materialTypeApiMapperImpl.fromMaterialType(type);

		// Then:
		assertThat(actualResult).isNotNull();
	}

}