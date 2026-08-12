package com.aidigital.aionboarding.mappers.learning;

import com.aidigital.aionboarding.service.learning.models.LearningAssigneeRecord;
import com.aidigital.aionboarding.service.learning.models.MyLessonSummaryRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapTeamAssignmentRecord;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises {@link LearningApiMapper} default methods directly against the generated
 * implementation, since MapStruct default methods are otherwise mocked out (returning
 * canned values) everywhere else in the test suite.
 */
class LearningApiMapperTest {

	private final LearningApiMapper mapper = new LearningApiMapperImpl();

	@Nested
	class ToLearningAssigneesResponseV1 {

		@Test
		void shouldReturnNullForANullPageTest() {
			// When:
			var result = mapper.toLearningAssigneesResponseV1((Page<LearningAssigneeRecord>) null);

			// Then:
			assertThat(result).isNull();
		}

		@Test
		void shouldBuildAnEmptyResponseForAnEmptyPageTest() {
			// When:
			var result = mapper.toLearningAssigneesResponseV1(new PageImpl<>(List.<LearningAssigneeRecord>of()));

			// Then:
			assertThat(result.getAssignees()).isEmpty();
		}

		@Test
		void shouldWrapANullListInAnEmptyPageTest() {
			// When:
			var result = mapper.toLearningAssigneesResponseV1((List<LearningAssigneeRecord>) null);

			// Then:
			assertThat(result.getAssignees()).isEmpty();
		}
	}

	@Nested
	class ToMyLessonsResponseV1 {

		@Test
		void shouldReturnNullForANullPageTest() {
			// When:
			var result = mapper.toMyLessonsResponseV1(null);

			// Then:
			assertThat(result).isNull();
		}

		@Test
		void shouldBuildAnEmptyResponseForAnEmptyPageTest() {
			// Given:
			Page<MyLessonSummaryRecord> page = new PageImpl<>(List.of());

			// When:
			var result = mapper.toMyLessonsResponseV1(page);

			// Then:
			assertThat(result.getLessons()).isEmpty();
		}
	}

	@Nested
	class ToRoadmapTeamAssignmentsListResponseV1 {

		@Test
		void shouldReturnNullForANullPageTest() {
			// When:
			var result = mapper.toRoadmapTeamAssignmentsListResponseV1((Page<RoadmapTeamAssignmentRecord>) null);

			// Then:
			assertThat(result).isNull();
		}

		@Test
		void shouldBuildAnEmptyResponseForAnEmptyPageTest() {
			// When:
			var result = mapper.toRoadmapTeamAssignmentsListResponseV1(
					new PageImpl<>(List.<RoadmapTeamAssignmentRecord>of()));

			// Then:
			assertThat(result.getAssignments()).isEmpty();
		}

		@Test
		void shouldWrapANullListInAnEmptyPageTest() {
			// When:
			var result = mapper.toRoadmapTeamAssignmentsListResponseV1((List<RoadmapTeamAssignmentRecord>) null);

			// Then:
			assertThat(result.getAssignments()).isEmpty();
		}
	}
}
