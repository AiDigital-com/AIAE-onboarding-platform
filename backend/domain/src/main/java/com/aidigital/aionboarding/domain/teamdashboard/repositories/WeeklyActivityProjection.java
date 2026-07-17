package com.aidigital.aionboarding.domain.teamdashboard.repositories;

import java.time.LocalDateTime;

public interface WeeklyActivityProjection {

    Long getUserId();

    LocalDateTime getWeekStart();

    Integer getLessons();

    Integer getQuizzes();
}
