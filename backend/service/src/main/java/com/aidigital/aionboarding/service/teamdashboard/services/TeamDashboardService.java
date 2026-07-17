package com.aidigital.aionboarding.service.teamdashboard.services;

import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.teamdashboard.models.TeamDashboardPeriod;
import com.aidigital.aionboarding.service.teamdashboard.models.TeamDashboardRecord;

/**
 * Aggregates team progress, activity, and roadmap statistics for dashboard views.
 */
public interface TeamDashboardService {

    /**
     * Loads dashboard data visible to the viewer for the given reporting period.
     *
     * @param viewer authenticated admin or team lead; scope is resolved from role and team membership
     * @param period reporting window for member statistics
     * @return aggregated dashboard record including members, roadmaps, weekly activity, low-confidence
     *     lessons, recent activity, per-member roadmap details, and KPIs
     */
    TeamDashboardRecord getTeamDashboardData(AppUser viewer, TeamDashboardPeriod period);
}
