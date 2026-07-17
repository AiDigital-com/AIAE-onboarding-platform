package com.aidigital.aionboarding.service.common.security;

/**
 * Immutable caller context passed through the service layer.
 * Built by {@code AppUserFactory} after Clerk JWT validation and internal user provisioning.
 */
public record AppUser(
    Long internalId,
    String clerkUserId,
    String email,
    String fullName,
    String roleCode,
    String name,
    String position,
    String avatarStorageKey,
    String avatarColor
) {
    public boolean isAdmin() {
        return "admin".equals(roleCode);
    }

    public boolean isTeamLead() {
        return "teamlead".equals(roleCode);
    }
}
