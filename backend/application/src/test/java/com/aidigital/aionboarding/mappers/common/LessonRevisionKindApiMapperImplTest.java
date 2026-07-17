package com.aidigital.aionboarding.mappers.common;

import com.aidigital.aionboarding.api.v1.model.LessonRevisionKindV1;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LessonRevisionKindApiMapperImplTest {

	private final LessonRevisionKindApiMapperImpl lessonRevisionKindApiMapperImpl =
			new LessonRevisionKindApiMapperImpl();

	@Test
	void shouldMapLessonRevisionKindStringTest() {
		// Given:
		String changeScope = "value";

		// When:
		LessonRevisionKindV1 actualResult = lessonRevisionKindApiMapperImpl.mapLessonRevisionKind(changeScope);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldFromLessonRevisionKindLessonRevisionKindV1Test() {
		// Given:
		LessonRevisionKindV1 changeScope = Instancio.create(LessonRevisionKindV1.class);

		// When:
		String actualResult = lessonRevisionKindApiMapperImpl.fromLessonRevisionKind(changeScope);

		// Then:
		assertThat(actualResult).isNotNull();
	}

}