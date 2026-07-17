package com.aidigital.aionboarding.service.team.services;

import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.team.models.TeamRecord;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Manages team composition, membership, role promotion, and assignable learner lookups.
 */
public interface TeamService {

    /**
     * Returns all teams grouped by admin or team-lead owners with their members.
     *
     * @return teams sorted by lead name, each with members sorted by name
     */
    List<TeamRecord> getTeams();

    /**
     * Returns a page of teams visible to the caller.
     *
     * @param user authenticated user; admins receive all teams, team leads receive their own team
     * @param query optional lead-name/email search
     * @param pageable page and sort request
     * @return visible teams page
     */
    Page<TeamRecord> getTeams(AppUser user, String query, Pageable pageable);

    /**
     * Returns users the caller may assign learning content to.
     *
     * @param user authenticated user; admins receive all other users, team leads receive their members
     * @return assignable users, or an empty list when the caller is not an admin or team lead
     */
    List<UserRecord> getAssignableLearningUsers(AppUser user);

    /**
     * Returns a page of users the caller may assign learning content to.
     *
     * @param user authenticated user; admins receive all other users, team leads receive their members
     * @param query optional name/email search
     * @param pageable page and sort request
     * @return assignable users page
     */
    Page<UserRecord> getAssignableLearningUsers(AppUser user, String query, Pageable pageable);

    /**
     * Returns candidate users available for team management discovery.
     * Admin callers receive all users except self; team leads receive all non-admin users except self;
     * other roles receive an empty list.
     *
     * @param user authenticated user
     * @return candidate users for team discovery, or an empty list when the caller is not an admin or team lead
     */
    List<UserRecord> getTeamCandidateUsers(AppUser user);

    /**
     * Returns a page of candidate users available for team management discovery.
     *
     * @param user authenticated user
     * @param query optional name/email search
     * @param pageable page and sort request
     * @return candidate users page
     */
    Page<UserRecord> getTeamCandidateUsers(AppUser user, String query, Pageable pageable);

    /**
     * Looks up a user by internal id.
     *
     * @param id internal user id
     * @return matching user record when found
     */
    Optional<UserRecord> getUserById(Long id);

    /**
     * Looks up a user by email address.
     *
     * @param email email address (trimmed and normalized to lowercase)
     * @return matching user record when found
     */
    Optional<UserRecord> getUserByEmail(String email);

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

    /**
     * Adds a member to a team, resolving the member by id or email/name when needed.
     *
     * @param leadUserId team lead internal user id
     * @param memberUserId optional member internal user id; used when present
     * @param memberEmailOrName fallback email or display name used when {@code memberUserId} is {@code null}
     * @return added member user record
     * @throws com.aidigital.aionboarding.service.common.error.AppException when the member cannot be resolved
     *     or a lead attempts to add themselves
     */
    UserRecord addTeamMember(Long leadUserId, Long memberUserId, String memberEmailOrName);

    /**
     * Removes a member from a team.
     *
     * @param leadUserId team lead internal user id
     * @param memberUserId member internal user id
     * @return {@code true} when the membership existed and was removed, otherwise {@code false}
     */
    boolean removeTeamMember(Long leadUserId, Long memberUserId);
}
