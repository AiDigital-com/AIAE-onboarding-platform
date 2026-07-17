package com.aidigital.aionboarding.mappers.teamdashboard;

import com.aidigital.aionboarding.api.v1.model.ActivityCompletionModeV1;
import com.aidigital.aionboarding.api.v1.model.LearningStatusV1;
import com.aidigital.aionboarding.api.v1.model.ProgressStatusV1;
import com.aidigital.aionboarding.api.v1.model.TeamDashboardIndividualLessonV1;
import com.aidigital.aionboarding.api.v1.model.TeamDashboardIndividualRoadmapV1;
import com.aidigital.aionboarding.api.v1.model.TeamDashboardKpisV1;
import com.aidigital.aionboarding.api.v1.model.TeamDashboardLowConfidenceAttemptItemV1;
import com.aidigital.aionboarding.api.v1.model.TeamDashboardLowConfidenceLessonV1;
import com.aidigital.aionboarding.api.v1.model.TeamDashboardMemberRoadmapV1;
import com.aidigital.aionboarding.api.v1.model.TeamDashboardMemberV1;
import com.aidigital.aionboarding.api.v1.model.TeamDashboardPeriodCountsV1;
import com.aidigital.aionboarding.api.v1.model.TeamDashboardQuizByPeriodV1;
import com.aidigital.aionboarding.api.v1.model.TeamDashboardRecentActivityV1;
import com.aidigital.aionboarding.api.v1.model.TeamDashboardRoadmapStatV1;
import com.aidigital.aionboarding.api.v1.model.TeamDashboardV1;
import com.aidigital.aionboarding.api.v1.model.TeamDashboardWeeklyActivityV1;
import com.aidigital.aionboarding.mappers.common.ActivityCompletionModeApiMapper;
import com.aidigital.aionboarding.mappers.common.LearningStatusApiMapper;
import com.aidigital.aionboarding.mappers.common.ProgressStatusApiMapper;
import com.aidigital.aionboarding.service.teamdashboard.models.TeamDashboardIndividualLessonRecord;
import com.aidigital.aionboarding.service.teamdashboard.models.TeamDashboardIndividualRoadmapRecord;
import com.aidigital.aionboarding.service.teamdashboard.models.TeamDashboardKpisRecord;
import com.aidigital.aionboarding.service.teamdashboard.models.TeamDashboardLowConfidenceAttemptItemRecord;
import com.aidigital.aionboarding.service.teamdashboard.models.TeamDashboardLowConfidenceLessonRecord;
import com.aidigital.aionboarding.service.teamdashboard.models.TeamDashboardMemberRecord;
import com.aidigital.aionboarding.service.teamdashboard.models.TeamDashboardMemberRoadmapRecord;
import com.aidigital.aionboarding.service.teamdashboard.models.TeamDashboardPeriodCountsRecord;
import com.aidigital.aionboarding.service.teamdashboard.models.TeamDashboardQuizByPeriodRecord;
import com.aidigital.aionboarding.service.teamdashboard.models.TeamDashboardRecentActivityRecord;
import com.aidigital.aionboarding.service.teamdashboard.models.TeamDashboardRecord;
import com.aidigital.aionboarding.service.teamdashboard.models.TeamDashboardRoadmapStatRecord;
import com.aidigital.aionboarding.service.teamdashboard.models.TeamDashboardWeeklyActivityRecord;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TeamDashboardApiMapperImplTest {

	@InjectMocks
	private TeamDashboardApiMapperImpl teamDashboardApiMapperImpl;

	@Mock
	private ProgressStatusApiMapper progressStatusApiMapper;

	@Mock
	private ActivityCompletionModeApiMapper activityCompletionModeApiMapper;

	@Mock
	private LearningStatusApiMapper learningStatusApiMapper;

	@BeforeEach
	void setUp() {
		when(progressStatusApiMapper.mapProgressStatus(anyString())).thenReturn(Instancio.create(ProgressStatusV1.class));
		when(activityCompletionModeApiMapper.mapActivityCompletionMode(anyString())).thenReturn(Instancio.create(ActivityCompletionModeV1.class));
		when(learningStatusApiMapper.mapLearningStatus(anyString())).thenReturn(Instancio.create(LearningStatusV1.class));
	}

	@Test
	void shouldToTeamDashboardMemberV1TeamDashboardMemberRecordTest() {
		// Given:
		TeamDashboardMemberRecord member = Instancio.create(TeamDashboardMemberRecord.class);

		// When:
		TeamDashboardMemberV1 actualResult = teamDashboardApiMapperImpl.toTeamDashboardMemberV1(member);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToTeamDashboardMemberV1TeamDashboardMemberRecordWithNullTest() {
		// Given:
		TeamDashboardMemberRecord member = null;

		// When:
		TeamDashboardMemberV1 actualResult = teamDashboardApiMapperImpl.toTeamDashboardMemberV1(member);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToTeamDashboardMemberRoadmapV1TeamDashboardMemberRoadmapRecordTest() {
		// Given:
		TeamDashboardMemberRoadmapRecord roadmap = Instancio.create(TeamDashboardMemberRoadmapRecord.class);

		// When:
		TeamDashboardMemberRoadmapV1 actualResult = teamDashboardApiMapperImpl.toTeamDashboardMemberRoadmapV1(roadmap);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToTeamDashboardMemberRoadmapV1TeamDashboardMemberRoadmapRecordWithNullTest() {
		// Given:
		TeamDashboardMemberRoadmapRecord roadmap = null;

		// When:
		TeamDashboardMemberRoadmapV1 actualResult = teamDashboardApiMapperImpl.toTeamDashboardMemberRoadmapV1(roadmap);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToTeamDashboardPeriodCountsV1TeamDashboardPeriodCountsRecordTest() {
		// Given:
		TeamDashboardPeriodCountsRecord counts = Instancio.create(TeamDashboardPeriodCountsRecord.class);

		// When:
		TeamDashboardPeriodCountsV1 actualResult = teamDashboardApiMapperImpl.toTeamDashboardPeriodCountsV1(counts);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToTeamDashboardPeriodCountsV1TeamDashboardPeriodCountsRecordWithNullTest() {
		// Given:
		TeamDashboardPeriodCountsRecord counts = null;

		// When:
		TeamDashboardPeriodCountsV1 actualResult = teamDashboardApiMapperImpl.toTeamDashboardPeriodCountsV1(counts);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToTeamDashboardQuizByPeriodV1TeamDashboardQuizByPeriodRecordTest() {
		// Given:
		TeamDashboardQuizByPeriodRecord quiz = Instancio.create(TeamDashboardQuizByPeriodRecord.class);

		// When:
		TeamDashboardQuizByPeriodV1 actualResult = teamDashboardApiMapperImpl.toTeamDashboardQuizByPeriodV1(quiz);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToTeamDashboardQuizByPeriodV1TeamDashboardQuizByPeriodRecordWithNullTest() {
		// Given:
		TeamDashboardQuizByPeriodRecord quiz = null;

		// When:
		TeamDashboardQuizByPeriodV1 actualResult = teamDashboardApiMapperImpl.toTeamDashboardQuizByPeriodV1(quiz);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToTeamDashboardRoadmapStatV1TeamDashboardRoadmapStatRecordTest() {
		// Given:
		TeamDashboardRoadmapStatRecord roadmap = Instancio.create(TeamDashboardRoadmapStatRecord.class);

		// When:
		TeamDashboardRoadmapStatV1 actualResult = teamDashboardApiMapperImpl.toTeamDashboardRoadmapStatV1(roadmap);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToTeamDashboardRoadmapStatV1TeamDashboardRoadmapStatRecordWithNullTest() {
		// Given:
		TeamDashboardRoadmapStatRecord roadmap = null;

		// When:
		TeamDashboardRoadmapStatV1 actualResult = teamDashboardApiMapperImpl.toTeamDashboardRoadmapStatV1(roadmap);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToTeamDashboardWeeklyActivityV1TeamDashboardWeeklyActivityRecordTest() {
		// Given:
		TeamDashboardWeeklyActivityRecord weekly = Instancio.create(TeamDashboardWeeklyActivityRecord.class);

		// When:
		TeamDashboardWeeklyActivityV1 actualResult =
				teamDashboardApiMapperImpl.toTeamDashboardWeeklyActivityV1(weekly);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToTeamDashboardWeeklyActivityV1TeamDashboardWeeklyActivityRecordWithNullTest() {
		// Given:
		TeamDashboardWeeklyActivityRecord weekly = null;

		// When:
		TeamDashboardWeeklyActivityV1 actualResult =
				teamDashboardApiMapperImpl.toTeamDashboardWeeklyActivityV1(weekly);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToTeamDashboardLowConfidenceLessonV1TeamDashboardLowConfidenceLessonRecordTest() {
		// Given:
		TeamDashboardLowConfidenceLessonRecord lesson = Instancio.create(TeamDashboardLowConfidenceLessonRecord.class);

		// When:
		TeamDashboardLowConfidenceLessonV1 actualResult =
				teamDashboardApiMapperImpl.toTeamDashboardLowConfidenceLessonV1(lesson);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToTeamDashboardLowConfidenceLessonV1TeamDashboardLowConfidenceLessonRecordWithNullTest() {
		// Given:
		TeamDashboardLowConfidenceLessonRecord lesson = null;

		// When:
		TeamDashboardLowConfidenceLessonV1 actualResult =
				teamDashboardApiMapperImpl.toTeamDashboardLowConfidenceLessonV1(lesson);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToTeamDashboardLowConfidenceAttemptItemV1TeamDashboardLowConfidenceAttemptItemRecordTest() {
		// Given:
		TeamDashboardLowConfidenceAttemptItemRecord item =
				Instancio.create(TeamDashboardLowConfidenceAttemptItemRecord.class);

		// When:
		TeamDashboardLowConfidenceAttemptItemV1 actualResult =
				teamDashboardApiMapperImpl.toTeamDashboardLowConfidenceAttemptItemV1(item);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToTeamDashboardLowConfidenceAttemptItemV1TeamDashboardLowConfidenceAttemptItemRecordWithNullTest() {
		// Given:
		TeamDashboardLowConfidenceAttemptItemRecord item = null;

		// When:
		TeamDashboardLowConfidenceAttemptItemV1 actualResult =
				teamDashboardApiMapperImpl.toTeamDashboardLowConfidenceAttemptItemV1(item);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToTeamDashboardRecentActivityV1TeamDashboardRecentActivityRecordTest() {
		// Given:
		TeamDashboardRecentActivityRecord activity = Instancio.create(TeamDashboardRecentActivityRecord.class);

		// When:
		TeamDashboardRecentActivityV1 actualResult =
				teamDashboardApiMapperImpl.toTeamDashboardRecentActivityV1(activity);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToTeamDashboardRecentActivityV1TeamDashboardRecentActivityRecordWithNullTest() {
		// Given:
		TeamDashboardRecentActivityRecord activity = null;

		// When:
		TeamDashboardRecentActivityV1 actualResult =
				teamDashboardApiMapperImpl.toTeamDashboardRecentActivityV1(activity);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToTeamDashboardIndividualLessonV1TeamDashboardIndividualLessonRecordTest() {
		// Given:
		TeamDashboardIndividualLessonRecord lesson = Instancio.create(TeamDashboardIndividualLessonRecord.class);

		// When:
		TeamDashboardIndividualLessonV1 actualResult =
				teamDashboardApiMapperImpl.toTeamDashboardIndividualLessonV1(lesson);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToTeamDashboardIndividualLessonV1TeamDashboardIndividualLessonRecordWithNullTest() {
		// Given:
		TeamDashboardIndividualLessonRecord lesson = null;

		// When:
		TeamDashboardIndividualLessonV1 actualResult =
				teamDashboardApiMapperImpl.toTeamDashboardIndividualLessonV1(lesson);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToTeamDashboardIndividualRoadmapV1TeamDashboardIndividualRoadmapRecordTest() {
		// Given:
		TeamDashboardIndividualRoadmapRecord roadmap = Instancio.create(TeamDashboardIndividualRoadmapRecord.class);

		// When:
		TeamDashboardIndividualRoadmapV1 actualResult =
				teamDashboardApiMapperImpl.toTeamDashboardIndividualRoadmapV1(roadmap);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToTeamDashboardIndividualRoadmapV1TeamDashboardIndividualRoadmapRecordWithNullTest() {
		// Given:
		TeamDashboardIndividualRoadmapRecord roadmap = null;

		// When:
		TeamDashboardIndividualRoadmapV1 actualResult =
				teamDashboardApiMapperImpl.toTeamDashboardIndividualRoadmapV1(roadmap);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToTeamDashboardKpisV1TeamDashboardKpisRecordTest() {
		// Given:
		TeamDashboardKpisRecord kpis = Instancio.create(TeamDashboardKpisRecord.class);

		// When:
		TeamDashboardKpisV1 actualResult = teamDashboardApiMapperImpl.toTeamDashboardKpisV1(kpis);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToTeamDashboardKpisV1TeamDashboardKpisRecordWithNullTest() {
		// Given:
		TeamDashboardKpisRecord kpis = null;

		// When:
		TeamDashboardKpisV1 actualResult = teamDashboardApiMapperImpl.toTeamDashboardKpisV1(kpis);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToTeamDashboardV1TeamDashboardRecordTest() {
		// Given:
		TeamDashboardRecord dashboard = Instancio.create(TeamDashboardRecord.class);

		// When:
		TeamDashboardV1 actualResult = teamDashboardApiMapperImpl.toTeamDashboardV1(dashboard);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToTeamDashboardV1TeamDashboardRecordWithNullTest() {
		// Given:
		TeamDashboardRecord dashboard = null;

		// When:
		TeamDashboardV1 actualResult = teamDashboardApiMapperImpl.toTeamDashboardV1(dashboard);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldMapIndividualRoadmapListListTest() {
		// Given:
		List<TeamDashboardIndividualRoadmapRecord> source =
				Instancio.ofList(TeamDashboardIndividualRoadmapRecord.class).create();

		// When:
		List actualResult = teamDashboardApiMapperImpl.mapIndividualRoadmapList(source);

		// Then:
		assertThat(actualResult).isNotNull();
	}

}