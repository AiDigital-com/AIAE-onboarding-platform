package com.aidigital.aionboarding.service.roadmap.models;

/**
 * A dry-run preview of what a grade filter would match within a group, without creating an
 * assignment or enrolling anyone.
 *
 * @param groupMembersCount        total current members in the group
 * @param membersMatchedCount      members who would be enrolled with the given grade filter
 * @param membersWithoutGradeCount members with no grade, who would be skipped when a filter is set
 */
public record RoadmapGroupAssignmentPreviewRecord(
    long groupMembersCount,
    long membersMatchedCount,
    long membersWithoutGradeCount
) {
}
