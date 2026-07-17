package com.aidigital.aionboarding.service.roadmap.services;

import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.roadmap.models.CreateRoadmapInput;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapListQuery;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapRecord;
import com.aidigital.aionboarding.service.roadmap.models.UpdateRoadmapInput;
import org.springframework.data.domain.Page;

/**
 * Manages roadmap listing, creation, updates, and deletion.
 */
public interface RoadmapService {

	/**
	 * Returns a bounded, sorted page of roadmaps matching the given filter.
	 *
	 * @param viewer authenticated user requesting the list
	 * @param query  typed filter and sort parameters
	 * @param page   zero-based page index
	 * @param size   maximum number of roadmaps per page
	 * @return a page of roadmap records
	 */
	Page<RoadmapRecord> getAllRoadmaps(AppUser viewer, RoadmapListQuery query, int page, int size);

	/**
	 * Counts roadmaps matching the given filter, without fetching or paginating any rows.
	 *
	 * @param viewer authenticated user requesting the count
	 * @param query  typed filter and sort parameters
	 * @return the number of roadmaps matching the filter
	 */
	long countRoadmaps(AppUser viewer, RoadmapListQuery query);

	/**
	 * Creates a new roadmap authored by the caller.
	 *
	 * @param viewer authenticated user with {@code roadmaps.create}
	 * @param input  title and optional description
	 * @return created roadmap record
	 * @throws com.aidigital.aionboarding.service.common.error.AppException when the caller lacks permission
	 *                                                                      or the title is blank
	 */
	RoadmapRecord createRoadmap(AppUser viewer, CreateRoadmapInput input);

	/**
	 * Updates an existing roadmap when the caller is allowed to manage it.
	 *
	 * @param viewer authenticated user with {@code roadmaps.manage} who can manage the roadmap author
	 * @param id     roadmap identifier
	 * @param input  optional title and description updates
	 * @return updated roadmap record
	 * @throws com.aidigital.aionboarding.service.common.error.AppException when the roadmap is missing,
	 *                                                                      the caller lacks permission, or management
	 *                                                                      of the author is not allowed
	 */
	RoadmapRecord updateRoadmap(AppUser viewer, Long id, UpdateRoadmapInput input);

	/**
	 * Deletes a roadmap when the caller is allowed to manage it.
	 *
	 * @param viewer authenticated user with {@code roadmaps.manage} who can manage the roadmap author
	 * @param id     roadmap identifier
	 * @throws com.aidigital.aionboarding.service.common.error.AppException when the roadmap is missing,
	 *                                                                      the caller lacks permission, or management
	 *                                                                      of the author is not allowed
	 */
	void deleteRoadmap(AppUser viewer, Long id);
}
