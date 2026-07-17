package com.aidigital.aionboarding.mappers.common;

import com.aidigital.aionboarding.api.v1.model.QuizQuestionTypeV1;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QuizQuestionTypeApiMapperImplTest {

	private final QuizQuestionTypeApiMapperImpl quizQuestionTypeApiMapperImpl = new QuizQuestionTypeApiMapperImpl();

	@Test
	void shouldMapQuizQuestionTypeStringTest() {
		// Given:
		String type = "value";

		// When:
		QuizQuestionTypeV1 actualResult = quizQuestionTypeApiMapperImpl.mapQuizQuestionType(type);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldFromQuizQuestionTypeQuizQuestionTypeV1Test() {
		// Given:
		QuizQuestionTypeV1 type = Instancio.create(QuizQuestionTypeV1.class);

		// When:
		String actualResult = quizQuestionTypeApiMapperImpl.fromQuizQuestionType(type);

		// Then:
		assertThat(actualResult).isNotNull();
	}

}