package com.aidigital.aionboarding.domain.teamdashboard.repositories;

import java.time.LocalDateTime;

public interface MemberStatsProjection {

	Long getId();

	String getName();

	String getEmail();

	String getRole();

	String getPosition();

	String getAvatarStorageKey();

	String getAvatarColor();

	Integer getRoadmapCount();

	Object getRoadmapIds();

	Object getRoadmapTitles();

	Integer getRoadmapLessonCount();

	Integer getRoadmapCompletedCount();

	Integer getCompletedWeek();

	Integer getCompletedMonth();

	Integer getCompletedQuarter();

	Integer getOpenAssignments();

	Integer getAvgQuizScoreWeek();

	Integer getAvgQuizScoreMonth();

	Integer getAvgQuizScoreQuarter();

	LocalDateTime getLastActiveAt();
}
