package com.aidigital.aionboarding.service.permission.services;

/**
 * Resolves team-leadership relationships (direct team membership and group-lead-to-group-member
 * overlap) used by authorization decisions elsewhere in the permission domain.
 */
public interface TeamLeadershipService {

    /**
     * Checks whether one user is the team lead of another, either through a direct team
     * membership row or by leading a group the member also belongs to.
     *
     * @param leadUserId   team lead internal user id
     * @param memberUserId team member internal user id
     * @return {@code true} when the lead-member relationship exists
     */
    boolean isTeamLeadForMember(Long leadUserId, Long memberUserId);
}
