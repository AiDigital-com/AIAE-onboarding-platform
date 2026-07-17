package com.aidigital.aionboarding.mappers.common;

import com.aidigital.aionboarding.api.v1.model.LessonContentFormatV1;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LessonContentFormatApiMapperImplTest {

	private final LessonContentFormatApiMapperImpl lessonContentFormatApiMapperImpl =
			new LessonContentFormatApiMapperImpl();

	@Test
	void shouldMapLessonContentFormatStringTest() {
		// Given:
		String contentFormat = "value";

		// When:
		LessonContentFormatV1 actualResult = lessonContentFormatApiMapperImpl.mapLessonContentFormat(contentFormat);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldFromLessonContentFormatLessonContentFormatV1Test() {
		// Given:
		LessonContentFormatV1 contentFormat = Instancio.create(LessonContentFormatV1.class);

		// When:
		String actualResult = lessonContentFormatApiMapperImpl.fromLessonContentFormat(contentFormat);

		// Then:
		assertThat(actualResult).isNotNull();
	}

}