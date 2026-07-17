package com.aidigital.aionboarding.service.teamdashboard.models;

public record TeamDashboardRoadmapStatRecord(
    String id,
    String name,
    int learners,
    int lessonCount,
    int progress,
    String color
) { }
