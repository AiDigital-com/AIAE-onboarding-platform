package com.aidigital.aionboarding.service.roadmap.models;

/**
 * Security context resolved once per request and pushed into the roadmap list query. Roadmaps
 * are private by default: a viewer sees a roadmap only when they hold an existing enrollment
 * (a {@code UserRoadmap} row, granted directly or fanned out from a group assignment), when
 * they may manage roadmaps and authored it, or when they are a team lead managing a roadmap
 * authored by one of their team members. An admin sees every roadmap regardless of the other
 * fields.
 *
 * @param admin              whether the viewer is an admin
 * @param canManageRoadmaps  whether the viewer holds the roadmaps-manage permission
 * @param teamLead           whether the viewer is a team lead, extending visibility to roadmaps
 *                           authored by their own team's members
 * @param viewerUserId       the viewer's internal user id
 */
public record RoadmapVisibilityFilter(boolean admin, boolean canManageRoadmaps, boolean teamLead, Long viewerUserId) {
}
