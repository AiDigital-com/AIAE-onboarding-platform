package com.aidigital.aionboarding.mappers.common;

import com.aidigital.aionboarding.api.v1.model.PermissionDefinitionMetaV1;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PermissionDefinitionRegistryTest {

	@Test
	void shouldHave20EntriesWithAllRequiredFieldsNonNull() {
		// Execution
		List<PermissionDefinitionMetaV1> defs = new PermissionDefinitionRegistry().all();

		// Verification
		assertThat(defs).hasSize(20);
		defs.forEach(def -> {
			assertThat(def.getCode()).isNotNull();
			assertThat(def.getLabel()).isNotBlank();
			assertThat(def.getGroup()).isNotBlank();
		});
	}
}
