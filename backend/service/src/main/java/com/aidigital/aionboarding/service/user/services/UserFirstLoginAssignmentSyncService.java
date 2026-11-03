package com.aidigital.aionboarding.service.user.services;

/**
 * Hands a newly created user over to the standing roadmap rules that apply to every new hire,
 * keeping the user aggregate unaware of roadmap internals.
 */
public interface UserFirstLoginAssignmentSyncService {

	/**
	 * Applies every "auto-assign to new users" roadmap to a user created at their first login.
	 *
	 * @param userId the user that was just created
	 */
	void onUserCreated(Long userId);
}
