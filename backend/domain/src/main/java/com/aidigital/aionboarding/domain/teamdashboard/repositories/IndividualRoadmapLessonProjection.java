package com.aidigital.aionboarding.domain.teamdashboard.repositories;

import java.time.LocalDateTime;

public interface IndividualRoadmapLessonProjection {

	Long getMemberId();

	Long getRoadmapId();

	String getRoadmapTitle();

	LocalDateTime getRoadmapEnrolledAt();

	Integer getSortOrder();

	Long getLessonId();

	String getLessonTitle();

	LocalDateTime getCompletedAt();

	LocalDateTime getEnrolledAt();

	Integer getAvgScore();

	LocalDateTime getLastQuizAt();
}
