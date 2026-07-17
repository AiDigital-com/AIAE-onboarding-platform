package com.aidigital.aionboarding.service.teamdashboard.services.impl;

import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.teamdashboard.models.TeamDashboardIndividualRoadmapRecord;
import com.aidigital.aionboarding.service.teamdashboard.models.TeamDashboardLowConfidenceLessonRecord;
import com.aidigital.aionboarding.service.teamdashboard.models.TeamDashboardMemberRecord;
import com.aidigital.aionboarding.service.teamdashboard.models.TeamDashboardPeriod;
import com.aidigital.aionboarding.service.teamdashboard.models.TeamDashboardRecentActivityRecord;
import com.aidigital.aionboarding.service.teamdashboard.models.TeamDashboardRecord;
import com.aidigital.aionboarding.service.teamdashboard.models.TeamDashboardRoadmapStatRecord;
import com.aidigital.aionboarding.service.teamdashboard.models.TeamDashboardScope;
import com.aidigital.aionboarding.service.teamdashboard.models.TeamDashboardWeeklyActivityRecord;
import com.aidigital.aionboarding.service.teamdashboard.services.TeamDashboardQueryService;
import com.aidigital.aionboarding.service.teamdashboard.services.TeamDashboardService;
import com.aidigital.aionboarding.service.teamdashboard.services.entity.TeamDashboardQueryEntityService;
import com.aidigital.aionboarding.service.teamdashboard.support.TeamDashboardRecordAssembler;
import com.aidigital.aionboarding.service.teamdashboard.support.TeamDashboardScopeResolver;
import com.aidigital.aionboarding.service.teamdashboard.util.TeamDashboardSupport;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TeamDashboardServiceImpl implements TeamDashboardService {

	private final TeamDashboardQueryEntityService teamDashboardQueryEntityService;
	private final TeamDashboardScopeResolver teamDashboardScopeResolver;
	private final TeamDashboardQueryService teamDashboardQueryService;
	private final TeamDashboardRecordAssembler teamDashboardMapper;
	private final CurrentTime currentTime;

	@Override
	@Transactional(readOnly = true)
	public TeamDashboardRecord getTeamDashboardData(AppUser viewer, TeamDashboardPeriod period) {
		TeamDashboardScope scope = teamDashboardScopeResolver.resolveVisibleTeamScope(viewer);
		List<Long> memberIds = scope.members().stream().map(UserRecord::id).filter(Objects::nonNull).toList();

		List<TeamDashboardMemberRecord> members = getMemberStats(memberIds, period, scope.leadId());
		List<TeamDashboardRoadmapStatRecord> roadmaps = getRoadmapStats(memberIds);
		List<TeamDashboardWeeklyActivityRecord> weekly = getWeeklyActivity(memberIds);
		List<TeamDashboardLowConfidenceLessonRecord> lowConfidenceLessons =
				getLowConfidenceLessons(memberIds, scope.leadId());
		List<TeamDashboardRecentActivityRecord> recentActivity = getRecentActivity(memberIds);

		Map<String, List<TeamDashboardIndividualRoadmapRecord>> individualRoadmapsByMemberId =
				teamDashboardQueryService.getIndividualRoadmapDetailsByMemberIds(memberIds);

		return new TeamDashboardRecord(
				scope.label(),
				currentTime.utcDateTime(),
				members,
				roadmaps,
				weekly,
				lowConfidenceLessons,
				recentActivity,
				individualRoadmapsByMemberId,
				teamDashboardMapper.toKpisRecord(members)
		);
	}

	List<TeamDashboardMemberRecord> getMemberStats(List<Long> memberIds, TeamDashboardPeriod period, Long leadId) {
		if (memberIds.isEmpty()) {
			return List.of();
		}

		List<TeamDashboardMemberRecord> result = new ArrayList<>();
		var rows = teamDashboardQueryEntityService.findMemberStats(memberIds);
		for (int index = 0; index < rows.size(); index++) {
			result.add(teamDashboardMapper.toMemberRecord(rows.get(index), index, period, leadId));
		}
		return result;
	}

	List<TeamDashboardRoadmapStatRecord> getRoadmapStats(List<Long> memberIds) {
		if (memberIds.isEmpty()) {
			return List.of();
		}
		List<TeamDashboardRoadmapStatRecord> result = new ArrayList<>();
		var rows = teamDashboardQueryEntityService.findRoadmapStats(memberIds);
		for (int index = 0; index < rows.size(); index++) {
			result.add(teamDashboardMapper.toRoadmapStatRecord(rows.get(index), index));
		}
		return result;
	}

	List<TeamDashboardWeeklyActivityRecord> getWeeklyActivity(List<Long> memberIds) {
		if (memberIds.isEmpty()) {
			return List.of();
		}
		return teamDashboardQueryEntityService.findWeeklyActivity(memberIds).stream()
				.map(teamDashboardMapper::toWeeklyActivityRecord)
				.toList();
	}

	List<TeamDashboardLowConfidenceLessonRecord> getLowConfidenceLessons(List<Long> memberIds, Long leadId) {
		if (memberIds.isEmpty()) {
			return List.of();
		}
		return teamDashboardQueryEntityService.findLowConfidenceLessons(memberIds, leadId).stream()
				.map(teamDashboardMapper::toLowConfidenceLessonRecord)
				.toList();
	}

	List<TeamDashboardRecentActivityRecord> getRecentActivity(List<Long> memberIds) {
		if (memberIds.isEmpty()) {
			return List.of();
		}
		Map<Long, String> fallbackColorByUserId = new HashMap<>();
		var rows = teamDashboardQueryEntityService.findRecentActivity(memberIds);
		List<TeamDashboardRecentActivityRecord> result = new ArrayList<>();
		for (int index = 0; index < rows.size(); index++) {
			var row = rows.get(index);
			fallbackColorByUserId.computeIfAbsent(row.getUserId(), userId ->
					TeamDashboardSupport.TEAM_COLORS.get(fallbackColorByUserId.size() % TeamDashboardSupport.TEAM_COLORS.size()));
			result.add(teamDashboardMapper.toRecentActivityRecord(row, index, fallbackColorByUserId));
		}
		return result;
	}
}
