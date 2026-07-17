package com.aidigital.aionboarding.service.teamdashboard.models;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record TeamDashboardRecord(
		String teamName,
		LocalDateTime refreshedAt,
		List<TeamDashboardMemberRecord> members,
		List<TeamDashboardRoadmapStatRecord> roadmaps,
		List<TeamDashboardWeeklyActivityRecord> weekly,
		List<TeamDashboardLowConfidenceLessonRecord> lowConfidenceLessons,
		List<TeamDashboardRecentActivityRecord> recentActivity,
		Map<String, List<TeamDashboardIndividualRoadmapRecord>> individualRoadmapsByMemberId,
		TeamDashboardKpisRecord kpis
) {

}
