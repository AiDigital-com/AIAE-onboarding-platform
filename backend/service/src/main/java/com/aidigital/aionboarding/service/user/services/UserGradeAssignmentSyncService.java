package com.aidigital.aionboarding.service.user.services;

/**
 * Keeps roadmap enrollment in sync when a user's grade changes, by re-evaluating the standing
 * group roadmap assignments of every group the user belongs to.
 */
public interface UserGradeAssignmentSyncService {

	/**
	 * Enrolls the user into every standing group assignment whose grade filter their new grade now
	 * matches (or which has no filter at all). Never removes an enrollment when the new grade stops
	 * matching a filter — existing progress is kept.
	 *
	 * @param userId     user whose grade changed
	 * @param newGradeId the user's new grade id, or {@code null} when the grade was cleared
	 */
	void onGradeChanged(Long userId, Long newGradeId);
}
