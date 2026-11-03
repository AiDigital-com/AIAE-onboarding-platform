package com.aidigital.aionboarding.service.roadmap.services;

/**
 * Applies the roadmap "automatically assign to new users" setting: enrolls a user who has just
 * been created at their first login into every roadmap that has the setting on.
 */
public interface RoadmapAutoAssignmentService {

	/**
	 * Enrolls a brand-new user into every roadmap flagged for auto-assignment. Runs once, at
	 * user creation; a later manual revoke is never undone, and a roadmap the user already holds
	 * (for example through a group) is left as is.
	 *
	 * @param userId the user that was just created
	 */
	void enrollNewUser(Long userId);
}
