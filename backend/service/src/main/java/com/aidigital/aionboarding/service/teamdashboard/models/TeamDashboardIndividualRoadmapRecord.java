package com.aidigital.aionboarding.service.teamdashboard.models;

import java.time.LocalDateTime;
import java.util.List;

public record TeamDashboardIndividualRoadmapRecord(
		String id,
		String title,
		LocalDateTime enrolledAt,
		List<TeamDashboardIndividualLessonRecord> lessons,
		int lessonCount,
		int completedCount,
		int progress
) {

}
