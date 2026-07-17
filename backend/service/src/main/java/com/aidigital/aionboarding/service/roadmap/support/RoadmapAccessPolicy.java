package com.aidigital.aionboarding.service.roadmap.support;

import com.aidigital.aionboarding.domain.roadmap.entities.Roadmap;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import com.aidigital.aionboarding.service.roadmap.services.entity.RoadmapEntityService;
import com.aidigital.aionboarding.service.team.services.entity.TeamEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Authorization gate for roadmap management operations: ownership/admin checks that must run
 * before any roadmap mutation, and manageable-roadmap-ID computation for listing views.
 */
@Component
@RequiredArgsConstructor
public class RoadmapAccessPolicy {

	private final RoadmapEntityService roadmapEntityService;
	private final PermissionService permissionService;
	private final TeamEntityService teamEntityService;

	/**
	 * Loads the roadmap and verifies the viewer may manage it, throwing before any mutation
	 * proceeds.
	 *
	 * @param viewer the acting user
	 * @param id     the roadmap primary key
	 * @return the loaded {@link Roadmap}, guaranteed manageable by {@code viewer}
	 * @throws AppException {@link ErrorReason#C001} if the roadmap does not exist, or
	 *                      {@link ErrorReason#C004} if the viewer is neither admin nor able to
	 *                      manage the roadmap's author
	 */
	public Roadmap requireManageable(AppUser viewer, Long id) {
		permissionService.requirePermission(viewer, PermissionKeys.ROADMAPS_MANAGE);
		Roadmap roadmap = roadmapEntityService.getReference(id);
		Long authorId = roadmap.getAuthorUser() == null ? null : roadmap.getAuthorUser().getId();
		if (!permissionService.canManageRoadmap(viewer, authorId)) {
			throw new AppException(ErrorReason.C004);
		}
		return roadmap;
	}

	/**
	 * Computes the set of roadmap IDs the viewer may manage, using the {@code null} sentinel to
	 * mean "can manage everything" for admins rather than an explicit (and needlessly large) set.
	 *
	 * @param viewer   the acting user
	 * @param roadmaps the roadmaps being rendered, used to evaluate per-roadmap ownership for
	 *                 non-admin viewers
	 * @return {@code null} if the viewer is an admin (manages everything); otherwise the explicit
	 * set of manageable roadmap IDs
	 */
	public Set<Long> getManageableRoadmapIds(AppUser viewer, List<Roadmap> roadmaps) {
		if (viewer.isAdmin()) {
			return null;
		}
		Set<Long> ids = new HashSet<>();
		Set<Long> candidateTeamMemberIds = new HashSet<>();
		for (Roadmap roadmap : roadmaps) {
			Long authorId = roadmap.getAuthorUser() == null ? null : roadmap.getAuthorUser().getId();
			if (Objects.equals(viewer.internalId(), authorId)) {
				ids.add(roadmap.getId());
			} else if (viewer.isTeamLead() && authorId != null) {
				candidateTeamMemberIds.add(authorId);
			}
		}
		Set<Long> teamMemberIds = viewer.isTeamLead()
				? teamEntityService.findMemberUserIdsByLeadUserIdAndMemberUserIds(viewer.internalId(),
				candidateTeamMemberIds)
				: Set.of();
		for (Roadmap roadmap : roadmaps) {
			Long authorId = roadmap.getAuthorUser() == null ? null : roadmap.getAuthorUser().getId();
			if (teamMemberIds.contains(authorId)) {
				ids.add(roadmap.getId());
			}
		}
		return ids;
	}
}
