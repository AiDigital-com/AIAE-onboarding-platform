package com.aidigital.aionboarding.mappers.learning;

import com.aidigital.aionboarding.api.v1.model.AssignmentEnrollmentV1;
import com.aidigital.aionboarding.api.v1.model.AssignmentResponseV1;
import com.aidigital.aionboarding.api.v1.model.CompletedRoadmapSummaryV1;
import com.aidigital.aionboarding.api.v1.model.EnrollmentResponseV1;
import com.aidigital.aionboarding.api.v1.model.LearningAssigneeV1;
import com.aidigital.aionboarding.api.v1.model.LessonActivityCountsV1;
import com.aidigital.aionboarding.api.v1.model.LessonEnrollmentV1;
import com.aidigital.aionboarding.api.v1.model.LessonGenerationStatusV1;
import com.aidigital.aionboarding.api.v1.model.LessonVisibilityV1;
import com.aidigital.aionboarding.api.v1.model.MyLessonSummaryV1;
import com.aidigital.aionboarding.api.v1.model.RoadmapAssignmentEnrollmentV1;
import com.aidigital.aionboarding.api.v1.model.RoadmapEnrollmentV1;
import com.aidigital.aionboarding.api.v1.model.RoadmapTeamAssignmentResponseV1;
import com.aidigital.aionboarding.api.v1.model.RoadmapTeamAssignmentV1;
import com.aidigital.aionboarding.mappers.common.LessonGenerationStatusApiMapper;
import com.aidigital.aionboarding.mappers.common.LessonVisibilityApiMapper;
import com.aidigital.aionboarding.service.learning.models.CompletedRoadmapRecord;
import com.aidigital.aionboarding.service.learning.models.LearningAssigneeRecord;
import com.aidigital.aionboarding.service.learning.models.LessonAssignmentEnrollmentRecord;
import com.aidigital.aionboarding.service.learning.models.LessonAssignmentResultRecord;
import com.aidigital.aionboarding.service.learning.models.LessonEnrollmentRecord;
import com.aidigital.aionboarding.service.learning.models.LessonEnrollmentResultRecord;
import com.aidigital.aionboarding.service.learning.models.MyLessonSummaryRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapAssignmentEnrollmentRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapAssignmentResultRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapEnrollmentRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapEnrollmentResultRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapTeamAssignmentRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapTeamAssignmentResultRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.LessonActivityCountsRecord;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LearningApiMapperImplTest {

	@InjectMocks
	private LearningApiMapperImpl learningApiMapperImpl;

	@Mock
	private LessonGenerationStatusApiMapper lessonGenerationStatusApiMapper;

	@Mock
	private LessonVisibilityApiMapper lessonVisibilityApiMapper;

	@BeforeEach
	void setUp() {
		when(lessonGenerationStatusApiMapper.mapLessonGenerationStatus(anyString())).thenReturn(Instancio.create(LessonGenerationStatusV1.class));
		when(lessonVisibilityApiMapper.mapLessonVisibility(anyString())).thenReturn(Instancio.create(LessonVisibilityV1.class));
	}

	@Test
	void shouldToLessonEnrollmentV1LessonEnrollmentRecordTest() {
		// Given:
		LessonEnrollmentRecord enrollment = Instancio.create(LessonEnrollmentRecord.class);

		// When:
		LessonEnrollmentV1 actualResult = learningApiMapperImpl.toLessonEnrollmentV1(enrollment);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToLessonEnrollmentV1LessonEnrollmentRecordWithNullTest() {
		// Given:
		LessonEnrollmentRecord enrollment = null;

		// When:
		LessonEnrollmentV1 actualResult = learningApiMapperImpl.toLessonEnrollmentV1(enrollment);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToAssignmentEnrollmentV1LessonAssignmentEnrollmentRecordTest() {
		// Given:
		LessonAssignmentEnrollmentRecord enrollment = Instancio.create(LessonAssignmentEnrollmentRecord.class);

		// When:
		AssignmentEnrollmentV1 actualResult = learningApiMapperImpl.toAssignmentEnrollmentV1(enrollment);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToAssignmentEnrollmentV1LessonAssignmentEnrollmentRecordWithNullTest() {
		// Given:
		LessonAssignmentEnrollmentRecord enrollment = null;

		// When:
		AssignmentEnrollmentV1 actualResult = learningApiMapperImpl.toAssignmentEnrollmentV1(enrollment);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToRoadmapAssignmentEnrollmentV1RoadmapAssignmentEnrollmentRecordTest() {
		// Given:
		RoadmapAssignmentEnrollmentRecord enrollment = Instancio.create(RoadmapAssignmentEnrollmentRecord.class);

		// When:
		RoadmapAssignmentEnrollmentV1 actualResult = learningApiMapperImpl.toRoadmapAssignmentEnrollmentV1(enrollment);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToRoadmapAssignmentEnrollmentV1RoadmapAssignmentEnrollmentRecordWithNullTest() {
		// Given:
		RoadmapAssignmentEnrollmentRecord enrollment = null;

		// When:
		RoadmapAssignmentEnrollmentV1 actualResult = learningApiMapperImpl.toRoadmapAssignmentEnrollmentV1(enrollment);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToRoadmapEnrollmentV1RoadmapEnrollmentRecordTest() {
		// Given:
		RoadmapEnrollmentRecord enrollment = Instancio.create(RoadmapEnrollmentRecord.class);

		// When:
		RoadmapEnrollmentV1 actualResult = learningApiMapperImpl.toRoadmapEnrollmentV1(enrollment);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToRoadmapEnrollmentV1RoadmapEnrollmentRecordWithNullTest() {
		// Given:
		RoadmapEnrollmentRecord enrollment = null;

		// When:
		RoadmapEnrollmentV1 actualResult = learningApiMapperImpl.toRoadmapEnrollmentV1(enrollment);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToCompletedRoadmapSummaryV1CompletedRoadmapRecordTest() {
		// Given:
		CompletedRoadmapRecord roadmap = Instancio.create(CompletedRoadmapRecord.class);

		// When:
		CompletedRoadmapSummaryV1 actualResult = learningApiMapperImpl.toCompletedRoadmapSummaryV1(roadmap);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToCompletedRoadmapSummaryV1CompletedRoadmapRecordWithNullTest() {
		// Given:
		CompletedRoadmapRecord roadmap = null;

		// When:
		CompletedRoadmapSummaryV1 actualResult = learningApiMapperImpl.toCompletedRoadmapSummaryV1(roadmap);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToLearningAssigneeV1LearningAssigneeRecordTest() {
		// Given:
		LearningAssigneeRecord assignee = Instancio.create(LearningAssigneeRecord.class);

		// When:
		LearningAssigneeV1 actualResult = learningApiMapperImpl.toLearningAssigneeV1(assignee);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToLearningAssigneeV1LearningAssigneeRecordWithNullTest() {
		// Given:
		LearningAssigneeRecord assignee = null;

		// When:
		LearningAssigneeV1 actualResult = learningApiMapperImpl.toLearningAssigneeV1(assignee);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToLessonAssignmentResponseV1LessonAssignmentResultRecordTest() {
		// Given:
		LessonAssignmentResultRecord result = Instancio.create(LessonAssignmentResultRecord.class);

		// When:
		AssignmentResponseV1 actualResult = learningApiMapperImpl.toLessonAssignmentResponseV1(result);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToLessonAssignmentResponseV1LessonAssignmentResultRecordWithNullTest() {
		// Given:
		LessonAssignmentResultRecord result = null;

		// When:
		AssignmentResponseV1 actualResult = learningApiMapperImpl.toLessonAssignmentResponseV1(result);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToRoadmapAssignmentResponseV1RoadmapAssignmentResultRecordTest() {
		// Given:
		RoadmapAssignmentResultRecord result = Instancio.create(RoadmapAssignmentResultRecord.class);

		// When:
		AssignmentResponseV1 actualResult = learningApiMapperImpl.toRoadmapAssignmentResponseV1(result);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToRoadmapAssignmentResponseV1RoadmapAssignmentResultRecordWithNullTest() {
		// Given:
		RoadmapAssignmentResultRecord result = null;

		// When:
		AssignmentResponseV1 actualResult = learningApiMapperImpl.toRoadmapAssignmentResponseV1(result);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToLessonEnrollmentResponseV1LessonEnrollmentResultRecordTest() {
		// Given:
		LessonEnrollmentResultRecord result = Instancio.create(LessonEnrollmentResultRecord.class);

		// When:
		EnrollmentResponseV1 actualResult = learningApiMapperImpl.toLessonEnrollmentResponseV1(result);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToLessonEnrollmentResponseV1LessonEnrollmentResultRecordWithNullTest() {
		// Given:
		LessonEnrollmentResultRecord result = null;

		// When:
		EnrollmentResponseV1 actualResult = learningApiMapperImpl.toLessonEnrollmentResponseV1(result);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToRoadmapEnrollmentResponseV1RoadmapEnrollmentResultRecordTest() {
		// Given:
		RoadmapEnrollmentResultRecord result = Instancio.create(RoadmapEnrollmentResultRecord.class);

		// When:
		EnrollmentResponseV1 actualResult = learningApiMapperImpl.toRoadmapEnrollmentResponseV1(result);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToRoadmapEnrollmentResponseV1RoadmapEnrollmentResultRecordWithNullTest() {
		// Given:
		RoadmapEnrollmentResultRecord result = null;

		// When:
		EnrollmentResponseV1 actualResult = learningApiMapperImpl.toRoadmapEnrollmentResponseV1(result);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToActivityCountsV1LessonActivityCountsRecordTest() {
		// Given:
		LessonActivityCountsRecord counts = Instancio.create(LessonActivityCountsRecord.class);

		// When:
		LessonActivityCountsV1 actualResult = learningApiMapperImpl.toActivityCountsV1(counts);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToActivityCountsV1LessonActivityCountsRecordWithNullTest() {
		// Given:
		LessonActivityCountsRecord counts = null;

		// When:
		LessonActivityCountsV1 actualResult = learningApiMapperImpl.toActivityCountsV1(counts);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToMyLessonSummaryV1MyLessonSummaryRecordTest() {
		// Given:
		MyLessonSummaryRecord record = Instancio.create(MyLessonSummaryRecord.class);

		// When:
		MyLessonSummaryV1 actualResult = learningApiMapperImpl.toMyLessonSummaryV1(record);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToMyLessonSummaryV1MyLessonSummaryRecordWithNullTest() {
		// Given:
		MyLessonSummaryRecord record = null;

		// When:
		MyLessonSummaryV1 actualResult = learningApiMapperImpl.toMyLessonSummaryV1(record);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToRoadmapTeamAssignmentV1RoadmapTeamAssignmentRecordTest() {
		// Given:
		RoadmapTeamAssignmentRecord assignment = Instancio.create(RoadmapTeamAssignmentRecord.class);

		// When:
		RoadmapTeamAssignmentV1 actualResult = learningApiMapperImpl.toRoadmapTeamAssignmentV1(assignment);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToRoadmapTeamAssignmentV1RoadmapTeamAssignmentRecordWithNullTest() {
		// Given:
		RoadmapTeamAssignmentRecord assignment = null;

		// When:
		RoadmapTeamAssignmentV1 actualResult = learningApiMapperImpl.toRoadmapTeamAssignmentV1(assignment);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToRoadmapTeamAssignmentResponseV1RoadmapTeamAssignmentResultRecordTest() {
		// Given:
		RoadmapTeamAssignmentResultRecord result = Instancio.create(RoadmapTeamAssignmentResultRecord.class);

		// When:
		RoadmapTeamAssignmentResponseV1 actualResult = learningApiMapperImpl.toRoadmapTeamAssignmentResponseV1(result);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToRoadmapTeamAssignmentResponseV1RoadmapTeamAssignmentResultRecordWithNullTest() {
		// Given:
		RoadmapTeamAssignmentResultRecord result = null;

		// When:
		RoadmapTeamAssignmentResponseV1 actualResult = learningApiMapperImpl.toRoadmapTeamAssignmentResponseV1(result);

		// Then:
		assertThat(actualResult).isNull();
	}

}