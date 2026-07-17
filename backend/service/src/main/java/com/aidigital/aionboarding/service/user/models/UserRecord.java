package com.aidigital.aionboarding.service.user.models;

public record UserRecord(
    Long id,
    String clerkUserId,
    String name,
    String email,
    String roleCode,
    String position,
    String avatarStorageKey,
    String avatarColor,
    Long gradeId,
    String gradeCode,
    String gradeName
) { }
