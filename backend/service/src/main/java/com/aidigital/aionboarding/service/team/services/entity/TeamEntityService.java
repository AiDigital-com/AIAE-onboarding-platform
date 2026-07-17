package com.aidigital.aionboarding.service.team.services.entity;

import com.aidigital.aionboarding.domain.team.entities.TeamMember;
import com.aidigital.aionboarding.domain.team.repositories.TeamMemberRepository;
import com.aidigital.aionboarding.domain.user.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Short-transaction CRUD helpers for the {@link TeamMember} entity.
 * <p>
 * This is the only service that may inject {@link TeamMemberRepository} directly.
 * All other services that require team-membership data must depend on this service.
 */
@Service
@RequiredArgsConstructor
public class TeamEntityService {

    private final TeamMemberRepository teamMemberRepository;

    /**
     * Loads all team memberships.
     *
     * @return every {@link TeamMember} in the system
     */
    @Transactional(readOnly = true)
    public List<TeamMember> findAll() {
        return teamMemberRepository.findAll();
    }

    /**
     * Loads one lead's memberships with member users eagerly fetched.
     *
     * @param leadUserId lead user primary key
     * @return matching memberships
     */
    @Transactional(readOnly = true)
    public List<TeamMember> findByLeadUserIdWithMember(Long leadUserId) {
        return teamMemberRepository.findByLeadUserIdWithMember(leadUserId);
    }

    /**
     * Loads team memberships for a bounded set of lead user IDs, with member users eagerly
     * fetched, so listing cost scales with the number of leads being displayed rather than
     * every membership in the workspace.
     *
     * @param leadUserIds lead user primary keys to restrict to
     * @return matching memberships, or an empty list when {@code leadUserIds} is empty
     */
    @Transactional(readOnly = true)
    public List<TeamMember> findByLeadUserIdIn(Collection<Long> leadUserIds) {
        if (leadUserIds == null || leadUserIds.isEmpty()) {
            return List.of();
        }
        return teamMemberRepository.findByLeadUserIdIn(leadUserIds);
    }

    /**
     * Loads a page of member users for one lead.
     *
     * @param leadUserId lead user primary key
     * @param query optional case-insensitive name/email search
     * @param pageable page and sort request
     * @return matching member users page
     */
    @Transactional(readOnly = true)
    public Page<User> findMembersByLeadUserId(Long leadUserId, String query, Pageable pageable) {
        String normalizedQuery = normalizeSearch(query);
        if (normalizedQuery == null) {
            return teamMemberRepository.findMembersByLeadUserId(leadUserId, pageable);
        }
        return teamMemberRepository.findMembersByLeadUserId(leadUserId, normalizedQuery, pageable);
    }

    /**
     * Returns team member IDs for one lead, restricted to candidate member IDs.
     *
     * @param leadUserId    lead user primary key
     * @param memberUserIds candidate member user primary keys
     * @return candidate IDs that are members of the lead's team
     */
    @Transactional(readOnly = true)
    public Set<Long> findMemberUserIdsByLeadUserIdAndMemberUserIds(Long leadUserId, Collection<Long> memberUserIds) {
        if (memberUserIds == null || memberUserIds.isEmpty()) {
            return Set.of();
        }
        return teamMemberRepository.findMemberUserIdsByLeadUserIdAndMemberUserIds(leadUserId, memberUserIds);
    }

    /**
     * Checks whether a membership exists for the given lead/member pair.
     *
     * @param leadUserId   the team lead's user ID
     * @param memberUserId the member's user ID
     * @return {@code true} if the membership exists
     */
    @Transactional(readOnly = true)
    public boolean existsByIdLeadUserIdAndIdMemberUserId(Long leadUserId, Long memberUserId) {
        return teamMemberRepository.existsByIdLeadUserIdAndIdMemberUserId(leadUserId, memberUserId);
    }

    /**
     * Deletes every membership led by the given user.
     *
     * @param leadUserId the team lead's user ID
     */
    @Transactional
    public void deleteByIdLeadUserId(Long leadUserId) {
        teamMemberRepository.deleteByIdLeadUserId(leadUserId);
    }

    /**
     * Checks whether a membership exists for the given composite ID.
     *
     * @param id the composite team-member ID
     * @return {@code true} if the membership exists
     */
    @Transactional(readOnly = true)
    public boolean existsById(TeamMember.TeamMemberId id) {
        return teamMemberRepository.existsById(id);
    }

    /**
     * Deletes the membership identified by the given composite ID.
     *
     * @param id the composite team-member ID
     */
    @Transactional
    public void deleteById(TeamMember.TeamMemberId id) {
        teamMemberRepository.deleteById(id);
    }

    /**
     * Persists a team membership.
     *
     * @param teamMember the membership to save
     * @return the saved {@link TeamMember}
     */
    @Transactional
    public TeamMember save(TeamMember teamMember) {
        return teamMemberRepository.save(teamMember);
    }

    String normalizeSearch(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        return query.trim();
    }
}
