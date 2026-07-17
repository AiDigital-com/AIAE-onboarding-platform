package com.aidigital.aionboarding.mappers.common;

import com.aidigital.aionboarding.api.v1.model.LessonActivityTypeV1;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LessonActivityTypeApiMapperImplTest {

	private final LessonActivityTypeApiMapperImpl lessonActivityTypeApiMapperImpl =
			new LessonActivityTypeApiMapperImpl();

	@Test
	void shouldMapLessonActivityTypeStringTest() {
		// Given:
		String type = "value";

		// When:
		LessonActivityTypeV1 actualResult = lessonActivityTypeApiMapperImpl.mapLessonActivityType(type);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldFromLessonActivityTypeLessonActivityTypeV1Test() {
		// Given:
		LessonActivityTypeV1 type = Instancio.create(LessonActivityTypeV1.class);

		// When:
		String actualResult = lessonActivityTypeApiMapperImpl.fromLessonActivityType(type);

		// Then:
		assertThat(actualResult).isNotNull();
	}

}