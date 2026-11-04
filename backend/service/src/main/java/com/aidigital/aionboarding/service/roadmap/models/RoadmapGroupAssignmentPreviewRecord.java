package com.aidigital.aionboarding.service.roadmap.models;

import java.util.List;

/**
 * A dry-run preview of what a grade filter would match within a group, without creating an
 * assignment or enrolling anyone.
 *
 * @param groupMembersCount        total current members in the group
 * @param membersMatchedCount      members who would be enrolled with the given grade filter
 * @param membersWithoutGradeCount members with no grade, who would be skipped when a filter is set
 * @param matchedUserIds           user ids of the members who would be enrolled, so a client can
 *                                 de-duplicate them against explicitly picked people and other teams
 */
public record RoadmapGroupAssignmentPreviewRecord(
    long groupMembersCount,
    long membersMatchedCount,
    long membersWithoutGradeCount,
    List<Long> matchedUserIds
) {
}
