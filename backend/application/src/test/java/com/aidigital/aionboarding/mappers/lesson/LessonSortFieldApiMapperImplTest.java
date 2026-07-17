package com.aidigital.aionboarding.mappers.lesson;

import com.aidigital.aionboarding.api.v1.model.LessonSortFieldV1;
import com.aidigital.aionboarding.service.lesson.models.LessonSortField;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LessonSortFieldApiMapperImplTest {

	private final LessonSortFieldApiMapperImpl lessonSortFieldApiMapperImpl = new LessonSortFieldApiMapperImpl();

	@Test
	void shouldToLessonSortFieldLessonSortFieldV1Test() {
		// Given:
		LessonSortFieldV1 sort = Instancio.create(LessonSortFieldV1.class);

		// When:
		LessonSortField actualResult = lessonSortFieldApiMapperImpl.toLessonSortField(sort);

		// Then:
		assertThat(actualResult).isNotNull();
	}

}