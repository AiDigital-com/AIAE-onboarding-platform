package com.aidigital.aionboarding.mappers.common;

import com.aidigital.aionboarding.api.v1.model.MaterialAssetKindV1;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MaterialAssetKindApiMapperImplTest {

	private final MaterialAssetKindApiMapperImpl materialAssetKindApiMapperImpl = new MaterialAssetKindApiMapperImpl();

	@Test
	void shouldMapMaterialAssetKindStringTest() {
		// Given:
		String kind = "value";

		// When:
		MaterialAssetKindV1 actualResult = materialAssetKindApiMapperImpl.mapMaterialAssetKind(kind);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldFromMaterialAssetKindMaterialAssetKindV1Test() {
		// Given:
		MaterialAssetKindV1 kind = Instancio.create(MaterialAssetKindV1.class);

		// When:
		String actualResult = materialAssetKindApiMapperImpl.fromMaterialAssetKind(kind);

		// Then:
		assertThat(actualResult).isNotNull();
	}

}