package com.aidigital.aionboarding.service.roadmap.models;

import com.aidigital.aionboarding.service.grade.models.GradeRecord;
import com.aidigital.aionboarding.service.user.models.UserRecord;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A roadmap's standing assignment to a group, with its resolved grade filter and current match
 * counts.
 *
 * @param id                       assignment primary key
 * @param roadmapId                assigned roadmap
 * @param roadmapTitle             assigned roadmap's display title
 * @param groupId                  assigned group
 * @param groupName                assigned group's display name
 * @param groupLeads               the group's current leads
 * @param gradeFilters             grades this assignment is narrowed to; empty means every member
 * @param membersMatchedCount      current group members who match the filter (and were/will be enrolled)
 * @param membersWithoutGradeCount current group members with no grade, skipped when a filter is set
 * @param assignedByUserId         who created the assignment, when known
 * @param assignedByUserName       display name of who created the assignment, when known
 * @param createdAt                when the assignment was created
 */
public record RoadmapGroupAssignmentRecord(
    Long id,
    Long roadmapId,
    String roadmapTitle,
    Long groupId,
    String groupName,
    List<UserRecord> groupLeads,
    List<GradeRecord> gradeFilters,
    long membersMatchedCount,
    long membersWithoutGradeCount,
    Long assignedByUserId,
    String assignedByUserName,
    LocalDateTime createdAt
) {
}
