package com.aidigital.aionboarding.mappers.common;

import com.aidigital.aionboarding.api.v1.model.LessonVisibilityV1;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LessonVisibilityApiMapperImplTest {

	private final LessonVisibilityApiMapperImpl lessonVisibilityApiMapperImpl = new LessonVisibilityApiMapperImpl();

	@Test
	void shouldMapLessonVisibilityStringTest() {
		// Given:
		String publicationStatus = "value";

		// When:
		LessonVisibilityV1 actualResult = lessonVisibilityApiMapperImpl.mapLessonVisibility(publicationStatus);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldFromLessonVisibilityLessonVisibilityV1Test() {
		// Given:
		LessonVisibilityV1 publicationStatus = Instancio.create(LessonVisibilityV1.class);

		// When:
		String actualResult = lessonVisibilityApiMapperImpl.fromLessonVisibility(publicationStatus);

		// Then:
		assertThat(actualResult).isNotNull();
	}

}