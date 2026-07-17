package com.aidigital.aionboarding.service.teamdashboard.services;

import com.aidigital.aionboarding.service.teamdashboard.models.TeamDashboardIndividualRoadmapRecord;

import java.util.List;
import java.util.Map;

/**
 * Loads per-member roadmap and standalone lesson progress details for team dashboard views.
 */
public interface TeamDashboardQueryService {

    /**
     * Builds individual roadmap groups and standalone lesson progress for each given team member
     * in one batched round trip per underlying query.
     *
     * @param memberIds team member internal user ids
     * @return roadmap groups with lesson progress keyed by member id (as a string); returns an
     *         empty map when {@code memberIds} is {@code null} or empty
     */
    Map<String, List<TeamDashboardIndividualRoadmapRecord>> getIndividualRoadmapDetailsByMemberIds(List<Long> memberIds);
}
