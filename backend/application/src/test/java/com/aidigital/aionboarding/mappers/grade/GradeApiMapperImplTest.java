package com.aidigital.aionboarding.mappers.grade;

import com.aidigital.aionboarding.api.v1.model.GradeResponseV1;
import com.aidigital.aionboarding.api.v1.model.GradeV1;
import com.aidigital.aionboarding.service.grade.models.GradeRecord;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GradeApiMapperImplTest {

	private final GradeApiMapperImpl gradeApiMapperImpl = new GradeApiMapperImpl();

	@Test
	void shouldToGradeV1GradeRecordTest() {
		// Given:
		GradeRecord grade = Instancio.create(GradeRecord.class);

		// When:
		GradeV1 actualResult = gradeApiMapperImpl.toGradeV1(grade);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToGradeV1GradeRecordWithNullTest() {
		// Given:
		GradeRecord grade = null;

		// When:
		GradeV1 actualResult = gradeApiMapperImpl.toGradeV1(grade);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToGradeResponseV1GradeRecordTest() {
		// Given:
		GradeRecord grade = Instancio.create(GradeRecord.class);

		// When:
		GradeResponseV1 actualResult = gradeApiMapperImpl.toGradeResponseV1(grade);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToGradeResponseV1GradeRecordWithNullTest() {
		// Given:
		GradeRecord grade = null;

		// When:
		GradeResponseV1 actualResult = gradeApiMapperImpl.toGradeResponseV1(grade);

		// Then:
		assertThat(actualResult).isNull();
	}

}