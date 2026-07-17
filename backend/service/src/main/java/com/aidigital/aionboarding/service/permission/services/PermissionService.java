package com.aidigital.aionboarding.service.permission.services;

import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.permission.models.PermissionSnapshotRecord;
import com.aidigital.aionboarding.service.user.models.UserRecord;

import java.util.List;
import java.util.Map;

/**
 * Resolves role defaults, user overrides, and resource-management authorization checks.
 */
public interface PermissionService {

	/**
	 * Returns the default permission map for a role code.
	 *
	 * @param roleCode role dictionary code (for example {@code admin}, {@code teamlead}, {@code member})
	 * @return map of every known permission key to its default allowed state for the role
	 */
	Map<String, Boolean> getRoleDefaults(String roleCode);

	/**
	 * Resolves the effective permission map for a user, applying stored overrides on top of role defaults.
	 *
	 * @param user authenticated user; when {@code null}, member defaults are returned
	 * @return effective permission key to allowed-state map
	 */
	Map<String, Boolean> getUserPermissionMap(AppUser user);

	/**
	 * Checks whether a user has a specific permission.
	 *
	 * @param user          authenticated user
	 * @param permissionKey permission key from {@link com.aidigital.aionboarding.service.permission.PermissionKeys}
	 * @return {@code true} when the permission is granted, otherwise {@code false}
	 */
	boolean userHasPermission(AppUser user, String permissionKey);

	/**
	 * Asserts that a user has a specific permission.
	 *
	 * @param user          authenticated user
	 * @param permissionKey permission key to require
	 * @throws com.aidigital.aionboarding.service.common.error.AppException with reason {@code C004}
	 *                                                                      when the permission is not granted
	 */
	void requirePermission(AppUser user, String permissionKey);

	/**
	 * Builds effective and override permission snapshots for a batch of users.
	 *
	 * @param users users to snapshot
	 * @return map of user id to permission snapshot including role code, effective permissions,
	 * and explicit overrides
	 */
	Map<Long, PermissionSnapshotRecord> snapshotForUsers(List<UserRecord> users);

	/**
	 * Replaces permission overrides for a target user, persisting only values that differ from role defaults.
	 *
	 * @param actor        authenticated user performing the change
	 * @param targetUserId internal user id receiving overrides
	 * @param overrides    permission key to allowed-state map
	 * @throws com.aidigital.aionboarding.service.common.error.AppException when the target user is missing,
	 *                                                                      the actor cannot delegate to the target
	 *                                                                      (admin overrides on admins, team leads
	 *                                                                      outside their team,
	 *                                                                      or non-managers), or access is denied with
	 *                                                                      reason {@code C004}
	 */
	void setOverrides(AppUser actor, Long targetUserId, Map<String, Boolean> overrides);

	/**
	 * Clears every stored permission override for a user, so the caller's own role defaults
	 * take effect again.
	 *
	 * @param userId internal user id whose overrides are removed
	 */
	void resetOverrides(Long userId);

	/**
	 * Checks whether one user is the team lead of another.
	 *
	 * @param leadUserId   team lead internal user id
	 * @param memberUserId team member internal user id
	 * @return {@code true} when the lead-member relationship exists
	 */
	boolean isTeamLeadForMember(Long leadUserId, Long memberUserId);

	/**
	 * Checks whether a user may manage a lesson created by another user.
	 *
	 * @param user            authenticated user
	 * @param createdByUserId lesson author internal user id
	 * @return {@code true} when the user is an admin or the lesson author
	 */
	boolean canManageExistingLesson(AppUser user, Long createdByUserId);

	/**
	 * Checks whether a user may manage a roadmap authored by another user.
	 *
	 * @param user         authenticated user
	 * @param authorUserId roadmap author internal user id
	 * @return {@code true} when the user is an admin, the author, or a team lead of the author
	 */
	boolean canManageRoadmap(AppUser user, Long authorUserId);

	/**
	 * Checks whether a user may manage a team owned by a lead.
	 *
	 * @param user       authenticated user
	 * @param leadUserId team lead internal user id
	 * @return {@code true} when the user is an admin or the team lead
	 */
	boolean canManageTeam(AppUser user, Long leadUserId);
}
