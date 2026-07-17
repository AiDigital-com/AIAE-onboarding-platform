package com.aidigital.aionboarding.domain.teamdashboard.repositories;

public interface RoadmapStatsProjection {

	Long getId();

	String getTitle();

	Integer getLearners();

	Integer getLessonCount();

	Integer getAvgProgress();
}
