package com.aidigital.aionboarding.mappers.teamdashboard;

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
import com.aidigital.aionboarding.config.ApplicationMapperConfig;
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
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
    config = ApplicationMapperConfig.class,
    uses = {
        ProgressStatusApiMapper.class,
        ActivityCompletionModeApiMapper.class,
        LearningStatusApiMapper.class
    }
)
public interface TeamDashboardApiMapper {

    @Mapping(target = "status", source = "status")
    TeamDashboardMemberV1 toTeamDashboardMemberV1(TeamDashboardMemberRecord member);

    TeamDashboardMemberRoadmapV1 toTeamDashboardMemberRoadmapV1(TeamDashboardMemberRoadmapRecord roadmap);

    TeamDashboardPeriodCountsV1 toTeamDashboardPeriodCountsV1(TeamDashboardPeriodCountsRecord counts);

    TeamDashboardQuizByPeriodV1 toTeamDashboardQuizByPeriodV1(TeamDashboardQuizByPeriodRecord quiz);

    TeamDashboardRoadmapStatV1 toTeamDashboardRoadmapStatV1(TeamDashboardRoadmapStatRecord roadmap);

    TeamDashboardWeeklyActivityV1 toTeamDashboardWeeklyActivityV1(TeamDashboardWeeklyActivityRecord weekly);

    @Mapping(target = "attemptItems", source = "attemptItems")
    TeamDashboardLowConfidenceLessonV1 toTeamDashboardLowConfidenceLessonV1(
        TeamDashboardLowConfidenceLessonRecord lesson
    );

    TeamDashboardLowConfidenceAttemptItemV1 toTeamDashboardLowConfidenceAttemptItemV1(
        TeamDashboardLowConfidenceAttemptItemRecord item
    );

    @Mapping(target = "action", source = "action")
    TeamDashboardRecentActivityV1 toTeamDashboardRecentActivityV1(TeamDashboardRecentActivityRecord activity);

    @Mapping(target = "state", source = "state")
    TeamDashboardIndividualLessonV1 toTeamDashboardIndividualLessonV1(TeamDashboardIndividualLessonRecord lesson);

    @Mapping(target = "lessons", source = "lessons")
    TeamDashboardIndividualRoadmapV1 toTeamDashboardIndividualRoadmapV1(TeamDashboardIndividualRoadmapRecord roadmap);

    TeamDashboardKpisV1 toTeamDashboardKpisV1(TeamDashboardKpisRecord kpis);

    TeamDashboardV1 toTeamDashboardV1(TeamDashboardRecord dashboard);

    List<TeamDashboardIndividualRoadmapV1> mapIndividualRoadmapList(
        List<TeamDashboardIndividualRoadmapRecord> source
    );
}
