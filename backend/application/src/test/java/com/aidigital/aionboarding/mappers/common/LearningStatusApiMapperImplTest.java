package com.aidigital.aionboarding.mappers.common;

import com.aidigital.aionboarding.api.v1.model.LearningStatusV1;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LearningStatusApiMapperImplTest {

	private final LearningStatusApiMapperImpl learningStatusApiMapperImpl = new LearningStatusApiMapperImpl();

	@Test
	void shouldMapLearningStatusStringTest() {
		// Given:
		String state = "value";

		// When:
		LearningStatusV1 actualResult = learningStatusApiMapperImpl.mapLearningStatus(state);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldFromLearningStatusLearningStatusV1Test() {
		// Given:
		LearningStatusV1 state = Instancio.create(LearningStatusV1.class);

		// When:
		String actualResult = learningStatusApiMapperImpl.fromLearningStatus(state);

		// Then:
		assertThat(actualResult).isNotNull();
	}

}