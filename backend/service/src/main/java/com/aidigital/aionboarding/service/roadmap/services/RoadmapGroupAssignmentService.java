package com.aidigital.aionboarding.service.roadmap.services;

import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapGroupAssignmentPreviewRecord;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapGroupAssignmentRecord;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapGroupAssignmentResultRecord;

import java.util.List;

/**
 * Manages standing roadmap-to-group assignments, including their optional grade filter, and
 * fans enrollment out to currently matching group members. Group-membership and grade-change
 * driven enrollment sync live in {@link RoadmapGroupAssignmentSyncService}.
 */
public interface RoadmapGroupAssignmentService {

	/**
	 * Creates or updates the standing assignment of a roadmap to a group, replacing any existing
	 * grade filter, and enrolls every currently matching group member.
	 *
	 * @param actor     authenticated caller
	 * @param roadmapId roadmap to assign
	 * @param groupId   target group
	 * @param gradeIds  grade ids to narrow the assignment to; {@code null} or empty means every member
	 * @return the standing assignment and the enrollments it produced
	 * @throws com.aidigital.aionboarding.service.common.error.AppException when the caller lacks
	 *                                                                      {@code learning.assign}, cannot manage the
	 *                                                                      group, or a grade id does not exist
	 */
	RoadmapGroupAssignmentResultRecord assignRoadmapToGroup(AppUser actor, Long roadmapId, Long groupId,
															List<Long> gradeIds);

	/**
	 * Removes a roadmap's standing assignment to a group. Existing member enrollments and
	 * progress are left untouched.
	 *
	 * @param actor     authenticated caller
	 * @param roadmapId roadmap primary key
	 * @param groupId   group primary key
	 * @throws com.aidigital.aionboarding.service.common.error.AppException when the caller lacks
	 *                                                                      {@code learning.assign} or cannot manage
	 *                                                                      the group
	 */
	void unassignRoadmapFromGroup(AppUser actor, Long roadmapId, Long groupId);

	/**
	 * Lists the standing group assignments for a roadmap, restricted to groups the viewer can
	 * manage.
	 *
	 * @param viewer    authenticated caller
	 * @param roadmapId roadmap primary key
	 * @return matching assignments
	 * @throws com.aidigital.aionboarding.service.common.error.AppException when the caller lacks {@code learning
	 * .assign}
	 */
	List<RoadmapGroupAssignmentRecord> listAssignments(AppUser viewer, Long roadmapId);

	/**
	 * Lists the standing roadmap assignments for one group.
	 *
	 * @param viewer  authenticated caller
	 * @param groupId group primary key
	 * @return matching assignments
	 * @throws com.aidigital.aionboarding.service.common.error.AppException when the caller lacks
	 *                                                                      {@code learning.assign} or cannot manage
	 *                                                                      the group
	 */
	List<RoadmapGroupAssignmentRecord> listAssignmentsForGroup(AppUser viewer, Long groupId);

	/**
	 * Previews how many current group members a grade filter would match, without creating an
	 * assignment or enrolling anyone.
	 *
	 * @param viewer   authenticated caller
	 * @param groupId  group primary key
	 * @param gradeIds candidate grade ids; {@code null} or empty means every member
	 * @return the match/skip counts
	 * @throws com.aidigital.aionboarding.service.common.error.AppException when the caller cannot manage the group
	 */
	RoadmapGroupAssignmentPreviewRecord previewAssignment(AppUser viewer, Long groupId, List<Long> gradeIds);
}
