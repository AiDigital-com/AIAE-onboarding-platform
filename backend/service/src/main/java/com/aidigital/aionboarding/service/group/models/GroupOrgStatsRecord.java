package com.aidigital.aionboarding.service.group.models;

/**
 * Aggregate team/member/lead counts, scoped like {@code listGroups}.
 *
 * @param totalGroups  number of groups visible to the caller
 * @param totalMembers number of distinct users who are a member of at least one visible group
 * @param totalLeads   number of distinct users who lead at least one visible group
 */
public record GroupOrgStatsRecord(int totalGroups, int totalMembers, int totalLeads) {

}
