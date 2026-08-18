package com.aidigital.aionboarding.service.team.services;

import com.aidigital.aionboarding.service.user.models.UserRecord;

import java.util.Optional;

/**
 * Promotes and demotes users to/from the team-lead role by email, for the Admin team-management
 * screen.
 */
public interface TeamLeadPromotionService {

    /**
     * Promotes a user to team lead by email, leaving admins unchanged.
     *
     * @param email user email address (trimmed and normalized to lowercase)
     * @return updated user record when the user exists
     * @throws com.aidigital.aionboarding.service.common.error.AppException when the team-lead role
     *     dictionary entry is missing
     */
    Optional<UserRecord> promoteTeamLeadByEmail(String email);

    /**
     * Demotes a team lead to member by email and removes their team memberships as lead.
     *
     * @param email team-lead email address (trimmed and normalized to lowercase)
     * @return updated user record when a team-lead user exists, otherwise empty
     * @throws com.aidigital.aionboarding.service.common.error.AppException when the member role
     *     dictionary entry is missing
     */
    Optional<UserRecord> demoteTeamLeadByEmail(String email);
}
