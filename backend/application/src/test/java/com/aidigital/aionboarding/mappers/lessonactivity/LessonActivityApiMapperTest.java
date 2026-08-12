package com.aidigital.aionboarding.mappers.lessonactivity;

import com.aidigital.aionboarding.api.v1.model.GenerateActivityRequestV1;
import com.aidigital.aionboarding.api.v1.model.LessonActivityTypeV1;
import com.aidigital.aionboarding.api.v1.model.SubmitActivityProgressRequestV1;
import com.aidigital.aionboarding.service.lessonactivity.models.LessonActivityRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.SubmitActivityProgressInput;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises {@link LessonActivityApiMapper} default methods directly against the generated
 * implementation, since MapStruct default methods are otherwise mocked out (returning
 * canned values) everywhere else in the test suite.
 */
class LessonActivityApiMapperTest {

	private final LessonActivityApiMapper mapper = new LessonActivityApiMapperImpl();

	@Nested
	class ToLessonActivitiesResponseV1 {

		@Test
		void shouldReturnNullForANullPageTest() {
			// When:
			var result = mapper.toLessonActivitiesResponseV1((Page<LessonActivityRecord>) null);

			// Then:
			assertThat(result).isNull();
		}

		@Test
		void shouldBuildAnEmptyResponseForAnEmptyPageTest() {
			// Given:
			Page<LessonActivityRecord> page = new PageImpl<>(List.of());

			// When:
			var result = mapper.toLessonActivitiesResponseV1(page);

			// Then:
			assertThat(result.getActivities()).isEmpty();
		}

		@Test
		void shouldWrapANullListInAnEmptyPageTest() {
			// When:
			var result = mapper.toLessonActivitiesResponseV1((List<LessonActivityRecord>) null);

			// Then:
			assertThat(result.getActivities()).isEmpty();
		}

		@Test
		void shouldWrapAPlainListInAPageTest() {
			// When:
			var result = mapper.toLessonActivitiesResponseV1(List.<LessonActivityRecord>of());

			// Then:
			assertThat(result.getActivities()).isEmpty();
		}
	}

	@Nested
	class ActivityType {

		@Test
		void shouldReturnNullWhenTypeIsUnsetTest() {
			// Given:
			GenerateActivityRequestV1 request = new GenerateActivityRequestV1();

			// When / Then:
			assertThat(mapper.activityType(request)).isNull();
		}

		@Test
		void shouldReturnTheWireValueWhenTypeIsSetTest() {
			// Given:
			GenerateActivityRequestV1 request = new GenerateActivityRequestV1().type(LessonActivityTypeV1.QUIZ);

			// When / Then:
			assertThat(mapper.activityType(request)).isEqualTo(LessonActivityTypeV1.QUIZ.getValue());
		}
	}

	@Nested
	class ToSubmitActivityProgressInput {

		@Test
		void shouldConvertEveryFieldTest() {
			// Given:
			SubmitActivityProgressRequestV1 request = new SubmitActivityProgressRequestV1()
					.type(LessonActivityTypeV1.QUIZ)
					.answers(List.of(List.of("a")))
					.reviewedCards(3);

			// When:
			SubmitActivityProgressInput input = mapper.toSubmitActivityProgressInput(request);

			// Then:
			assertThat(input.type()).isEqualTo(LessonActivityTypeV1.QUIZ.getValue());
			assertThat(input.answers()).containsExactly(List.of("a"));
			assertThat(input.reviewedCards()).isEqualTo(3);
		}

		@Test
		void shouldReturnNullTypeWhenUnsetTest() {
			// Given:
			SubmitActivityProgressRequestV1 request = new SubmitActivityProgressRequestV1();

			// When:
			SubmitActivityProgressInput input = mapper.toSubmitActivityProgressInput(request);

			// Then:
			assertThat(input.type()).isNull();
		}
	}
}
