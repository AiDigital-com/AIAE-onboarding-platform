package com.aidigital.aionboarding.mappers.grade;

import com.aidigital.aionboarding.service.grade.models.GradeRecord;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises {@link GradeApiMapper} default methods directly against the generated
 * implementation, since MapStruct default methods are otherwise mocked out (returning
 * canned values) everywhere else in the test suite.
 */
class GradeApiMapperTest {

	private final GradeApiMapper mapper = new GradeApiMapperImpl();

	@Nested
	class ToGradesListResponseV1 {

		@Test
		void shouldReturnNullForANullPageTest() {
			// When:
			var result = mapper.toGradesListResponseV1((Page<GradeRecord>) null);

			// Then:
			assertThat(result).isNull();
		}

		@Test
		void shouldBuildAnEmptyResponseForAnEmptyPageTest() {
			// When:
			var result = mapper.toGradesListResponseV1(new PageImpl<>(List.<GradeRecord>of()));

			// Then:
			assertThat(result.getGrades()).isEmpty();
		}

		@Test
		void shouldWrapAPlainListIntoAPageTest() {
			// When:
			var result = mapper.toGradesListResponseV1(List.<GradeRecord>of());

			// Then:
			assertThat(result.getGrades()).isEmpty();
		}
	}
}
