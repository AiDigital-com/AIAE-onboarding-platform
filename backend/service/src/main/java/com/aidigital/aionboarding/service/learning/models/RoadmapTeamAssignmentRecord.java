package com.aidigital.aionboarding.service.learning.models;

import java.time.LocalDateTime;

public record RoadmapTeamAssignmentRecord(
		Long id,
		Long roadmapId,
		Long leadUserId,
		Long assignedByUserId,
		LocalDateTime createdAt
) {

}
