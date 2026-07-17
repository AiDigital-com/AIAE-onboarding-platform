package com.aidigital.aionboarding.mappers.material;

import com.aidigital.aionboarding.api.v1.model.MaterialSortFieldV1;
import com.aidigital.aionboarding.service.material.models.MaterialSortField;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MaterialSortFieldApiMapperImplTest {

	private final MaterialSortFieldApiMapperImpl materialSortFieldApiMapperImpl = new MaterialSortFieldApiMapperImpl();

	@Test
	void shouldToMaterialSortFieldMaterialSortFieldV1Test() {
		// Given:
		MaterialSortFieldV1 sort = Instancio.create(MaterialSortFieldV1.class);

		// When:
		MaterialSortField actualResult = materialSortFieldApiMapperImpl.toMaterialSortField(sort);

		// Then:
		assertThat(actualResult).isNotNull();
	}

}