package com.aidigital.aionboarding.service.permission.services;

import com.aidigital.aionboarding.service.common.security.AppUser;

/**
 * High-level authorization policies derived from user roles.
 */
public interface PermissionPolicyService {

    /**
     * Determines whether a user can manage teams and related administrative team views.
     *
     * @param user authenticated user
     * @return {@code true} when the user is an admin or team lead
     */
    boolean isTeamManager(AppUser user);
}
