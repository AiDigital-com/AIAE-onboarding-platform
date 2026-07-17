package com.aidigital.aionboarding.domain.teamdashboard.repositories;

public interface LowConfidenceLessonProjection {

    Long getId();

    String getTitle();

    Integer getAttempts();

    Integer getLearners();

    Integer getAvgScore();

    Integer getAttemptsExcludingLead();

    Integer getLearnersExcludingLead();

    Integer getAvgScoreExcludingLead();

    String getAttemptItems();
}
