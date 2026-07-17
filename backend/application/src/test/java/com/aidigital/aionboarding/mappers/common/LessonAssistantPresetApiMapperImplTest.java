package com.aidigital.aionboarding.mappers.common;

import com.aidigital.aionboarding.api.v1.model.LessonAssistantPresetV1;
import com.aidigital.aionboarding.service.lesson.enums.LessonAssistantPreset;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LessonAssistantPresetApiMapperImplTest {

	private final LessonAssistantPresetApiMapperImpl lessonAssistantPresetApiMapperImpl =
			new LessonAssistantPresetApiMapperImpl();

	@Test
	void shouldMapAssistantPresetLessonAssistantPresetV1Test() {
		// Given:
		LessonAssistantPresetV1 preset = Instancio.create(LessonAssistantPresetV1.class);

		// When:
		LessonAssistantPreset actualResult = lessonAssistantPresetApiMapperImpl.mapAssistantPreset(preset);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldFromAssistantPresetStringTest() {
		// Given:
		String preset = "value";

		// When:
		LessonAssistantPresetV1 actualResult = lessonAssistantPresetApiMapperImpl.fromAssistantPreset(preset);

		// Then:
		assertThat(actualResult).isNotNull();
	}

}