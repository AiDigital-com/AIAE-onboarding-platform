package com.aidigital.aionboarding.service.teamdashboard.models;

import java.time.LocalDateTime;
import java.util.List;

public record TeamDashboardMemberRecord(
    String id,
    String name,
    String email,
    String role,
    boolean isTeamLead,
    String roadmap,
    String roadmapId,
    int roadmapCount,
    List<TeamDashboardMemberRoadmapRecord> roadmaps,
    int roadmapLessonCount,
    int roadmapCompletedCount,
    int progress,
    int completedInPeriod,
    TeamDashboardPeriodCountsRecord completedByPeriod,
    int openAssignments,
    Integer quiz,
    TeamDashboardQuizByPeriodRecord quizByPeriod,
    String status,
    LocalDateTime lastActiveAt,
    String lastActive,
    String avatarBg,
    String avatarStorageKey,
    String avatarColor
) { }
