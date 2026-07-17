package com.aidigital.aionboarding.domain.teamdashboard.repositories;

import java.time.LocalDateTime;

public interface StandaloneLessonProjection {

	Long getMemberId();

	Long getLessonId();

	String getLessonTitle();

	LocalDateTime getCompletedAt();

	LocalDateTime getEnrolledAt();

	Integer getAvgScore();

	LocalDateTime getLastQuizAt();
}
