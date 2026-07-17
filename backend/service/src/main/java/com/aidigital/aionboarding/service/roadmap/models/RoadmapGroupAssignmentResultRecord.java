package com.aidigital.aionboarding.service.roadmap.models;

import com.aidigital.aionboarding.service.learning.models.RoadmapAssignmentEnrollmentRecord;

import java.util.List;

/**
 * Result of assigning a roadmap to a group: the standing assignment plus the enrollments it
 * produced for currently matching members.
 *
 * @param ok          always {@code true} on success; present for response-shape consistency with
 *                    the existing individual-assignment endpoints
 * @param assignment  the created or updated standing assignment
 * @param enrollments enrollment rows created or refreshed for currently matching group members
 */
public record RoadmapGroupAssignmentResultRecord(
		boolean ok,
		RoadmapGroupAssignmentRecord assignment,
		List<RoadmapAssignmentEnrollmentRecord> enrollments
) {

}
