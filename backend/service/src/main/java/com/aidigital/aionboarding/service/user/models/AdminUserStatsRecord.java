package com.aidigital.aionboarding.service.user.models;

/**
 * Workspace-wide user/admin/team-lead counts, independent of the current search/filter/page.
 *
 * @param totalUsers              total number of users in the workspace
 * @param totalAdmins             number of users with the admin role
 * @param totalTeamLeads          number of users with the team lead role
 * @param totalPermissionsEnabled total number of enabled effective permission flags across every team lead
 */
public record AdminUserStatsRecord(
		int totalUsers, int totalAdmins, int totalTeamLeads, int totalPermissionsEnabled
) {

}
