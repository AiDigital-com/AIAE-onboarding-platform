package com.aidigital.aionboarding.domain.learning.repositories;

/**
 * One of the first few learners enrolled in a roadmap, for the avatar stack on roadmap cards.
 * Interface-based because it is produced by a native query.
 */
public interface RoadmapAssigneePreviewProjection {

	/**
	 * @return roadmap primary key
	 */
	Long getRoadmapId();

	/**
	 * @return enrolled user primary key
	 */
	Long getUserId();

	/**
	 * @return enrolled user's display name
	 */
	String getName();

	/**
	 * @return enrolled user's email
	 */
	String getEmail();

	/**
	 * @return enrolled user's avatar background color, when set
	 */
	String getAvatarColor();

	/**
	 * @return enrolled user's avatar storage key, when set
	 */
	String getAvatarStorageKey();
}
