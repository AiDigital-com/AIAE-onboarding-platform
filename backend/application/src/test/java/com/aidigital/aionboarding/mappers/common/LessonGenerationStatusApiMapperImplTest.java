package com.aidigital.aionboarding.mappers.common;

import com.aidigital.aionboarding.api.v1.model.LessonGenerationStatusV1;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LessonGenerationStatusApiMapperImplTest {

	private final LessonGenerationStatusApiMapperImpl lessonGenerationStatusApiMapperImpl =
			new LessonGenerationStatusApiMapperImpl();

	@Test
	void shouldMapLessonGenerationStatusStringTest() {
		// Given:
		String status = "value";

		// When:
		LessonGenerationStatusV1 actualResult = lessonGenerationStatusApiMapperImpl.mapLessonGenerationStatus(status);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldFromLessonGenerationStatusLessonGenerationStatusV1Test() {
		// Given:
		LessonGenerationStatusV1 status = Instancio.create(LessonGenerationStatusV1.class);

		// When:
		String actualResult = lessonGenerationStatusApiMapperImpl.fromLessonGenerationStatus(status);

		// Then:
		assertThat(actualResult).isNotNull();
	}

}