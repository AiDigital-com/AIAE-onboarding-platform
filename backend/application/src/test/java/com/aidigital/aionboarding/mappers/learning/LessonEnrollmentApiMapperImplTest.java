package com.aidigital.aionboarding.mappers.learning;

import com.aidigital.aionboarding.api.v1.model.LessonEnrollmentV1;
import com.aidigital.aionboarding.service.learning.models.LessonEnrollmentRecord;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LessonEnrollmentApiMapperImplTest {

	private final LessonEnrollmentApiMapperImpl lessonEnrollmentApiMapperImpl = new LessonEnrollmentApiMapperImpl();

	@Test
	void shouldToLessonEnrollmentV1LessonEnrollmentRecordTest() {
		// Given:
		LessonEnrollmentRecord enrollment = Instancio.create(LessonEnrollmentRecord.class);

		// When:
		LessonEnrollmentV1 actualResult = lessonEnrollmentApiMapperImpl.toLessonEnrollmentV1(enrollment);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToLessonEnrollmentV1LessonEnrollmentRecordWithNullTest() {
		// Given:
		LessonEnrollmentRecord enrollment = null;

		// When:
		LessonEnrollmentV1 actualResult = lessonEnrollmentApiMapperImpl.toLessonEnrollmentV1(enrollment);

		// Then:
		assertThat(actualResult).isNull();
	}

}