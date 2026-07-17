package com.aidigital.aionboarding.mappers.common;

import com.aidigital.aionboarding.api.v1.model.ActivityCompletionModeV1;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ActivityCompletionModeApiMapperImplTest {

	private final ActivityCompletionModeApiMapperImpl activityCompletionModeApiMapperImpl =
			new ActivityCompletionModeApiMapperImpl();

	@Test
	void shouldMapActivityCompletionModeStringTest() {
		// Given:
		String action = "value";

		// When:
		ActivityCompletionModeV1 actualResult = activityCompletionModeApiMapperImpl.mapActivityCompletionMode(action);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldFromActivityCompletionModeActivityCompletionModeV1Test() {
		// Given:
		ActivityCompletionModeV1 action = Instancio.create(ActivityCompletionModeV1.class);

		// When:
		String actualResult = activityCompletionModeApiMapperImpl.fromActivityCompletionMode(action);

		// Then:
		assertThat(actualResult).isNotNull();
	}

}