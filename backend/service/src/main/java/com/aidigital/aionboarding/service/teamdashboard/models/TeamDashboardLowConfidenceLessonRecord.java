package com.aidigital.aionboarding.service.teamdashboard.models;

import java.util.List;

public record TeamDashboardLowConfidenceLessonRecord(
		String id,
		String lesson,
		int attempts,
		int learners,
		int avgScore,
		int attemptsExcludingLead,
		int learnersExcludingLead,
		int avgScoreExcludingLead,
		List<TeamDashboardLowConfidenceAttemptItemRecord> attemptItems
) {

}
