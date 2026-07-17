package com.aidigital.aionboarding.service.teamdashboard.models;

public record TeamDashboardWeeklyActivityRecord(
    String userId,
    String label,
    int lessons,
    int quizzes
) { }
