package com.aidigital.aionboarding.service.roadmap.models;

/**
 * One of the first few learners enrolled in a roadmap, for the avatar stack on roadmap cards.
 *
 * @param userId           enrolled user id
 * @param name             display name
 * @param email            email address
 * @param avatarColor      avatar background color, when set
 * @param avatarStorageKey avatar image storage key, when set
 */
public record RoadmapAssigneePreviewRecord(
    Long userId,
    String name,
    String email,
    String avatarColor,
    String avatarStorageKey
) { }
