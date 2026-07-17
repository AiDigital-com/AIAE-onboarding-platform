package com.aidigital.aionboarding.service.roadmap.models;

import org.springframework.data.domain.Sort;

import java.util.List;

/**
 * Typed filter and sort parameters for a paged roadmap list query.
 *
 * @param searchText free-text search matched against title, description, creator name, and tags; {@code null} to skip
 * @param tags restricts results to roadmaps tagged with every given value; {@code null} or empty to skip
 * @param createdByUserId restricts results to roadmaps authored by this internal user id; {@code null} to skip
 * @param assignedToMe when {@code true}, restricts results to roadmaps the viewer is enrolled in
 * @param sortField whitelisted field to sort by
 * @param direction sort direction
 */
public record RoadmapListQuery(
    String searchText,
    List<String> tags,
    Long createdByUserId,
    Boolean assignedToMe,
    RoadmapSortField sortField,
    Sort.Direction direction
) {
}
