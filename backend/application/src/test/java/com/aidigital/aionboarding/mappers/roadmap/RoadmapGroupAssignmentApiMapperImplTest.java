package com.aidigital.aionboarding.mappers.roadmap;

import com.aidigital.aionboarding.api.v1.model.GradeV1;
import com.aidigital.aionboarding.api.v1.model.RoadmapAssignmentEnrollmentV1;
import com.aidigital.aionboarding.api.v1.model.RoadmapGroupAssignmentPreviewResponseV1;
import com.aidigital.aionboarding.api.v1.model.RoadmapGroupAssignmentPreviewV1;
import com.aidigital.aionboarding.api.v1.model.RoadmapGroupAssignmentResponseV1;
import com.aidigital.aionboarding.api.v1.model.RoadmapGroupAssignmentV1;
import com.aidigital.aionboarding.api.v1.model.UserSummaryV1;
import com.aidigital.aionboarding.mappers.grade.GradeApiMapper;
import com.aidigital.aionboarding.mappers.learning.LearningApiMapper;
import com.aidigital.aionboarding.mappers.user.UserApiMapper;
import com.aidigital.aionboarding.service.grade.models.GradeRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapAssignmentEnrollmentRecord;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapGroupAssignmentPreviewRecord;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapGroupAssignmentRecord;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapGroupAssignmentResultRecord;
import com.aidigital.aionboarding.service.user.models.UserRecord;
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
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoadmapGroupAssignmentApiMapperImplTest {

	@InjectMocks
	private RoadmapGroupAssignmentApiMapperImpl roadmapGroupAssignmentApiMapperImpl;

	@Mock
	private UserApiMapper userApiMapper;

	@Mock
	private GradeApiMapper gradeApiMapper;

	@Mock
	private LearningApiMapper learningApiMapper;

	@BeforeEach
	void setUp() {
		when(userApiMapper.toUserSummaryV1(any(UserRecord.class))).thenReturn(Instancio.create(UserSummaryV1.class));
		when(gradeApiMapper.toGradeV1(any(GradeRecord.class))).thenReturn(Instancio.create(GradeV1.class));
		when(learningApiMapper.toRoadmapAssignmentEnrollmentV1(any(RoadmapAssignmentEnrollmentRecord.class))).thenReturn(Instancio.create(RoadmapAssignmentEnrollmentV1.class));
	}

	@Test
	void shouldToRoadmapGroupAssignmentV1RoadmapGroupAssignmentRecordTest() {
		// Given:
		RoadmapGroupAssignmentRecord assignment = Instancio.create(RoadmapGroupAssignmentRecord.class);

		// When:
		RoadmapGroupAssignmentV1 actualResult =
				roadmapGroupAssignmentApiMapperImpl.toRoadmapGroupAssignmentV1(assignment);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToRoadmapGroupAssignmentV1RoadmapGroupAssignmentRecordWithNullTest() {
		// Given:
		RoadmapGroupAssignmentRecord assignment = null;

		// When:
		RoadmapGroupAssignmentV1 actualResult =
				roadmapGroupAssignmentApiMapperImpl.toRoadmapGroupAssignmentV1(assignment);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToRoadmapGroupAssignmentResponseV1RoadmapGroupAssignmentResultRecordTest() {
		// Given:
		RoadmapGroupAssignmentResultRecord result = Instancio.create(RoadmapGroupAssignmentResultRecord.class);

		// When:
		RoadmapGroupAssignmentResponseV1 actualResult =
				roadmapGroupAssignmentApiMapperImpl.toRoadmapGroupAssignmentResponseV1(result);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToRoadmapGroupAssignmentResponseV1RoadmapGroupAssignmentResultRecordWithNullTest() {
		// Given:
		RoadmapGroupAssignmentResultRecord result = null;

		// When:
		RoadmapGroupAssignmentResponseV1 actualResult =
				roadmapGroupAssignmentApiMapperImpl.toRoadmapGroupAssignmentResponseV1(result);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToRoadmapGroupAssignmentPreviewV1RoadmapGroupAssignmentPreviewRecordTest() {
		// Given:
		RoadmapGroupAssignmentPreviewRecord preview = Instancio.create(RoadmapGroupAssignmentPreviewRecord.class);

		// When:
		RoadmapGroupAssignmentPreviewV1 actualResult =
				roadmapGroupAssignmentApiMapperImpl.toRoadmapGroupAssignmentPreviewV1(preview);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToRoadmapGroupAssignmentPreviewV1RoadmapGroupAssignmentPreviewRecordWithNullTest() {
		// Given:
		RoadmapGroupAssignmentPreviewRecord preview = null;

		// When:
		RoadmapGroupAssignmentPreviewV1 actualResult =
				roadmapGroupAssignmentApiMapperImpl.toRoadmapGroupAssignmentPreviewV1(preview);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToRoadmapGroupAssignmentPreviewResponseV1RoadmapGroupAssignmentPreviewRecordTest() {
		// Given:
		RoadmapGroupAssignmentPreviewRecord preview = Instancio.create(RoadmapGroupAssignmentPreviewRecord.class);

		// When:
		RoadmapGroupAssignmentPreviewResponseV1 actualResult =
				roadmapGroupAssignmentApiMapperImpl.toRoadmapGroupAssignmentPreviewResponseV1(preview);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToRoadmapGroupAssignmentPreviewResponseV1RoadmapGroupAssignmentPreviewRecordWithNullTest() {
		// Given:
		RoadmapGroupAssignmentPreviewRecord preview = null;

		// When:
		RoadmapGroupAssignmentPreviewResponseV1 actualResult =
				roadmapGroupAssignmentApiMapperImpl.toRoadmapGroupAssignmentPreviewResponseV1(preview);

		// Then:
		assertThat(actualResult).isNull();
	}

}