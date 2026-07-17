package com.aidigital.aionboarding.domain.learning.repositories;

/**
 * {@link CompletedRoadmapProjection} implementation used as a {@code CriteriaBuilder.construct}
 * target, since Spring Data's automatic interface-projection proxying only applies to
 * Spring-Data-executed query methods, not to results built by hand via the Criteria API.
 */
public record CompletedRoadmapRow(Long id, String title) implements CompletedRoadmapProjection {

	@Override
	public Long getId() {
		return id;
	}

	@Override
	public String getTitle() {
		return title;
	}
}
