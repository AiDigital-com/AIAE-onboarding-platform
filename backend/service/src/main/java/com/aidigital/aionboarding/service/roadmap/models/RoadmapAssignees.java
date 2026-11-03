package com.aidigital.aionboarding.service.roadmap.models;

import java.util.List;
import java.util.Map;

/**
 * Pre-fetched assignee data for a page of roadmaps, keyed by roadmap id.
 *
 * @param countByRoadmapId   enrolled learner count per roadmap; absent means zero
 * @param previewByRoadmapId the first few enrolled learners per roadmap; absent means none
 */
public record RoadmapAssignees(
    Map<Long, Long> countByRoadmapId,
    Map<Long, List<RoadmapAssigneePreviewRecord>> previewByRoadmapId
) { }
