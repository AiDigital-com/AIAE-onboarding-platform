package com.aidigital.aionboarding.service.teamdashboard.models;

public record TeamDashboardRecentActivityRecord(
    String id,
    String userId,
    String who,
    String action,
    String what,
    String when,
    String kind,
    Integer score,
    Boolean passed,
    String avatarBg,
    String avatarStorageKey,
    String avatarColor
) { }
