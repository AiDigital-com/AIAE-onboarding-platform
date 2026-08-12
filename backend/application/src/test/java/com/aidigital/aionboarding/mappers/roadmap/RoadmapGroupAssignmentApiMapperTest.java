package com.aidigital.aionboarding.mappers.roadmap;

import com.aidigital.aionboarding.service.roadmap.models.RoadmapGroupAssignmentRecord;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises {@link RoadmapGroupAssignmentApiMapper} default methods directly against the
 * generated implementation, since MapStruct default methods are otherwise mocked out (returning
 * canned values) everywhere else in the test suite.
 */
class RoadmapGroupAssignmentApiMapperTest {

	private final RoadmapGroupAssignmentApiMapper mapper = new RoadmapGroupAssignmentApiMapperImpl();

	@Nested
	class ToRoadmapGroupAssignmentsListResponseV1 {

		@Test
		void shouldReturnNullForANullPageTest() {
			// When:
			var result = mapper.toRoadmapGroupAssignmentsListResponseV1((Page<RoadmapGroupAssignmentRecord>) null);

			// Then:
			assertThat(result).isNull();
		}

		@Test
		void shouldBuildAnEmptyResponseForAnEmptyPageTest() {
			// When:
			var result = mapper.toRoadmapGroupAssignmentsListResponseV1(
					new PageImpl<>(List.<RoadmapGroupAssignmentRecord>of()));

			// Then:
			assertThat(result.getAssignments()).isEmpty();
		}

		@Test
		void shouldWrapAPlainListIntoAPageTest() {
			// When:
			var result = mapper.toRoadmapGroupAssignmentsListResponseV1(List.<RoadmapGroupAssignmentRecord>of());

			// Then:
			assertThat(result.getAssignments()).isEmpty();
		}
	}
}
