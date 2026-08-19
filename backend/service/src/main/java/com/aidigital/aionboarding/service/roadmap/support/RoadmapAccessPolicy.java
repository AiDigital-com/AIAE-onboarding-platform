package com.aidigital.aionboarding.service.roadmap.support;

import com.aidigital.aionboarding.domain.learning.entities.UserRoadmap;
import com.aidigital.aionboarding.domain.roadmap.entities.Roadmap;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.learning.services.entity.LearningEnrollmentEntityService;
import com.aidigital.aionboarding.service.learning.support.LearningEnrollmentSupport;
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
 * before any roadmap mutation, self-enrollment eligibility, and manageable-roadmap-ID computation
 * for listing views.
 * <p>
 * {@link #getManageableRoadmapIds} decides which of a page's already-visible rows the viewer may
 * <em>manage</em> (i.e. edit/delete). It is a distinct concern from list <em>visibility</em>,
 * which {@code RoadmapSpecificationBuilder.visibilityPredicate} enforces in SQL before those rows
 * are even fetched. The two rules must stay semantically aligned for a manage-holder — the same
 * mirrored-rule hazard already documented between {@code LessonSpecificationBuilder} and
 * {@code LessonMutationSupport}: if they drift, a roadmap a manager may edit could be missing
 * from their own list, or a roadmap absent from the manageable set could still leak into the list.
 * {@link #requireSelfEnrollable} is a third consumer of the same underlying manage-holder rule
 * (via {@link #isManageable}) and must stay aligned with the other two for the same reason.
 */
@Component
@RequiredArgsConstructor
public class RoadmapAccessPolicy {

	private final RoadmapEntityService roadmapEntityService;
	private final PermissionService permissionService;
	private final TeamEntityService teamEntityService;
	private final LearningEnrollmentEntityService learningEnrollmentEntityService;
	private final LearningEnrollmentSupport learningEnrollmentSupport;

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
	 * Verifies that the actor may self-enroll in a roadmap and returns it. Mirrors
	 * {@code RoadmapSpecificationBuilder.visibilityPredicate} exactly: self-enroll is allowed only
	 * when the roadmap is already visible to the actor — either they already hold an enrollment
	 * (making a repeat self-enroll an idempotent no-op) or they may manage the roadmap (see
	 * {@link #isManageable}). Throws {@link ErrorReason#C001}, never {@link ErrorReason#C004}, so
	 * a self-enroll attempt against a roadmap the actor cannot see never discloses that the
	 * roadmap exists.
	 *
	 * @param actor     the user attempting to self-enroll
	 * @param roadmapId the roadmap primary key
	 * @return the loaded {@link Roadmap}
	 * @throws AppException {@link ErrorReason#C001} if the roadmap does not exist, or the actor
	 *                       holds no enrollment and may not manage it
	 */
	public Roadmap requireSelfEnrollable(AppUser actor, Long roadmapId) {
		Roadmap roadmap = roadmapEntityService.getReference(roadmapId);
		if (isAlreadyEnrolled(actor, roadmapId) || isManageable(actor, roadmap)) {
			return roadmap;
		}
		throw new AppException(ErrorReason.C001, roadmapId);
	}

	/**
	 * Checks whether the actor already holds a roadmap enrollment, so a repeat self-enroll request
	 * is recognized as an idempotent no-op rather than re-evaluated as a fresh authorization
	 * decision.
	 *
	 * @param actor     the acting user
	 * @param roadmapId the roadmap primary key
	 * @return {@code true} when a {@code UserRoadmap} row already exists for this actor and roadmap
	 */
	boolean isAlreadyEnrolled(AppUser actor, Long roadmapId) {
		UserRoadmap.UserRoadmapId id = learningEnrollmentSupport.userRoadmapId(actor.internalId(), roadmapId);
		return learningEnrollmentEntityService.findUserRoadmapById(id).isPresent();
	}

	/**
	 * Checks whether the actor may manage the given roadmap, mirroring the manage-holder branch of
	 * {@code RoadmapSpecificationBuilder.visibilityPredicate}: an admin always qualifies regardless
	 * of the {@code ROADMAPS_MANAGE} permission override (matching the SQL predicate's
	 * admin-short-circuit); otherwise the actor must hold {@code ROADMAPS_MANAGE} and be the
	 * roadmap's author or a team lead over the author (delegated to
	 * {@link PermissionService#canManageRoadmap}, the single source of truth for that
	 * relationship).
	 *
	 * @param actor   the acting user
	 * @param roadmap the roadmap being checked
	 * @return {@code true} when the actor may manage the roadmap
	 */
	boolean isManageable(AppUser actor, Roadmap roadmap) {
		if (actor.isAdmin()) {
			return true;
		}
		if (!permissionService.userHasPermission(actor, PermissionKeys.ROADMAPS_MANAGE)) {
			return false;
		}
		Long authorId = roadmap.getAuthorUser() == null ? null : roadmap.getAuthorUser().getId();
		return permissionService.canManageRoadmap(actor, authorId);
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
