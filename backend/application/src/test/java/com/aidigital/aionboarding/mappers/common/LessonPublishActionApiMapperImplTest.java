package com.aidigital.aionboarding.mappers.common;

import com.aidigital.aionboarding.api.v1.model.LessonPublishActionV1;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LessonPublishActionApiMapperImplTest {

	private final LessonPublishActionApiMapperImpl lessonPublishActionApiMapperImpl =
			new LessonPublishActionApiMapperImpl();

	@Test
	void shouldFromLessonPublishActionV1Test() {
		// Given:
		LessonPublishActionV1 action = Instancio.create(LessonPublishActionV1.class);

		// When:
		String actualResult = lessonPublishActionApiMapperImpl.from(action);

		// Then:
		assertThat(actualResult).isNotNull();
	}

}