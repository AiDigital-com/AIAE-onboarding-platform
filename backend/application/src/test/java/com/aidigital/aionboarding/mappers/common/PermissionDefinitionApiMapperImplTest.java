package com.aidigital.aionboarding.mappers.common;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PermissionDefinitionApiMapperImplTest {

	private final PermissionDefinitionApiMapperImpl permissionDefinitionApiMapperImpl =
			new PermissionDefinitionApiMapperImpl();

	@Test
	void shouldAllDefinitionsPermissionDefinitionRegistryTest() {
		// Given:
		PermissionDefinitionRegistry permissionDefinitionRegistry = mock(PermissionDefinitionRegistry.class);
		when(permissionDefinitionRegistry.all()).thenReturn(List.of());

		// When:
		List actualResult = permissionDefinitionApiMapperImpl.allDefinitions(permissionDefinitionRegistry);

		// Then:
		assertThat(actualResult).isEmpty();
	}

}