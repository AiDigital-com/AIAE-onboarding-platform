package com.aidigital.aionboarding.service.learning.models;

import java.util.List;

public record RoadmapTeamAssignmentResultRecord(
		boolean ok,
		RoadmapTeamAssignmentRecord assignment,
		List<RoadmapAssignmentEnrollmentRecord> enrollments
) {

}
