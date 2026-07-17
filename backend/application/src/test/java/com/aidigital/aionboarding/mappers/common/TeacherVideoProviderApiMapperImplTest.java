package com.aidigital.aionboarding.mappers.common;

import com.aidigital.aionboarding.api.v1.model.TeacherVideoProviderV1;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TeacherVideoProviderApiMapperImplTest {

	private final TeacherVideoProviderApiMapperImpl teacherVideoProviderApiMapperImpl =
			new TeacherVideoProviderApiMapperImpl();

	@Test
	void shouldMapTeacherVideoProviderStringTest() {
		// Given:
		String provider = "value";

		// When:
		TeacherVideoProviderV1 actualResult = teacherVideoProviderApiMapperImpl.mapTeacherVideoProvider(provider);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldFromTeacherVideoProviderTeacherVideoProviderV1Test() {
		// Given:
		TeacherVideoProviderV1 provider = Instancio.create(TeacherVideoProviderV1.class);

		// When:
		String actualResult = teacherVideoProviderApiMapperImpl.fromTeacherVideoProvider(provider);

		// Then:
		assertThat(actualResult).isNotNull();
	}

}