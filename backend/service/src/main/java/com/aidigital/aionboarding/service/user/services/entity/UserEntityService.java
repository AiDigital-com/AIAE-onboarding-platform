package com.aidigital.aionboarding.service.user.services.entity;

import com.aidigital.aionboarding.domain.common.dictionary.UserRoleCode;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.domain.user.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Short-transaction CRUD helpers for the {@link User} entity.
 * <p>
 * This is the only service that may inject {@link UserRepository} directly.
 * All other services that require user data must depend on this service.
 */
@Service
@RequiredArgsConstructor
public class UserEntityService {

    private final UserRepository userRepository;

    /**
     * Loads a single user by Clerk user ID.
     *
     * @param clerkUserId the external Clerk user identifier
     * @return the matching {@link User}, or empty if none exists
     */
    @Transactional(readOnly = true)
    public Optional<User> findByClerkUserId(String clerkUserId) {
        return userRepository.findByClerkUserId(clerkUserId);
    }

    /**
     * Loads a single user by email address.
     *
     * @param email the normalized email address
     * @return the matching {@link User}, or empty if none exists
     */
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Loads a single user by primary key.
     *
     * @param id the user primary key
     * @return the matching {@link User}, or empty if none exists
     */
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Loads a user and takes a pessimistic write lock on the row, held for the rest of the
     * caller's transaction. The caller must already be running inside a transaction (this method
     * joins it rather than opening its own) so the lock is actually held across the caller's
     * subsequent writes, not released immediately.
     *
     * @param id the user primary key
     * @return the matching {@link User}, or empty if none exists
     */
    @Transactional
    public Optional<User> findByIdForUpdate(Long id) {
        return userRepository.findByIdForUpdate(id);
    }

    /**
     * Loads all users.
     *
     * @return every {@link User} in the system
     */
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    /**
     * Loads users whose role code is one of the given role codes.
     *
     * @param roleCodes role dictionary codes
     * @return matching users
     */
    @Transactional(readOnly = true)
    public List<User> findByRoleCodeIn(Collection<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return List.of();
        }
        return userRepository.findByRoleCodeIn(roleCodes);
    }

    /**
     * Loads a page of users whose role code is one of the given role codes.
     *
     * @param roleCodes role dictionary codes
     * @param query optional case-insensitive name/email search
     * @param pageable page and sort request
     * @return matching users page
     */
    @Transactional(readOnly = true)
    public Page<User> findByRoleCodeIn(Collection<String> roleCodes, String query, Pageable pageable) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return Page.empty(pageable);
        }
        String normalizedQuery = normalizeSearch(query);
        if (normalizedQuery == null) {
            return userRepository.findByRoleCodeIn(roleCodes, pageable);
        }
        return userRepository.findByRoleCodeIn(roleCodes, normalizedQuery, pageable);
    }

    /**
     * Loads all users except the one identified by {@code excludeId}.
     *
     * @param excludeId the primary key to exclude from the result
     * @return every {@link User} other than the excluded one
     */
    @Transactional(readOnly = true)
    public List<User> findAllExcluding(Long excludeId) {
        return userRepository.findAllExcluding(excludeId);
    }

    /**
     * Loads a page of all users except the excluded one.
     *
     * @param excludeId the primary key to exclude from the result
     * @param query optional case-insensitive name/email search
     * @param pageable page and sort request
     * @return matching users page
     */
    @Transactional(readOnly = true)
    public Page<User> findAllExcluding(Long excludeId, String query, Pageable pageable) {
        String normalizedQuery = normalizeSearch(query);
        if (normalizedQuery == null) {
            return userRepository.findAllExcluding(excludeId, pageable);
        }
        return userRepository.findAllExcluding(excludeId, normalizedQuery, pageable);
    }

    /**
     * Loads users eligible to be added as team candidates: excludes the given user and any
     * user whose role code matches {@code excludeRoleCode}.
     *
     * @param excludeId       the primary key to exclude from the result
     * @param excludeRoleCode the role code to exclude from the result
     * @return the eligible team-candidate {@link User} list
     */
    @Transactional(readOnly = true)
    public List<User> findTeamCandidates(Long excludeId, String excludeRoleCode) {
        return userRepository.findTeamCandidates(excludeId, excludeRoleCode);
    }

    /**
     * Loads a page of users eligible to be added as team candidates.
     *
     * @param excludeId       the primary key to exclude from the result
     * @param excludeRoleCode the role code to exclude from the result
     * @param query optional case-insensitive name/email search
     * @param pageable page and sort request
     * @return matching candidate users page
     */
    @Transactional(readOnly = true)
    public Page<User> findTeamCandidates(Long excludeId, String excludeRoleCode, String query, Pageable pageable) {
        String normalizedQuery = normalizeSearch(query);
        if (normalizedQuery == null) {
            return userRepository.findTeamCandidates(excludeId, excludeRoleCode, pageable);
        }
        return userRepository.findTeamCandidates(excludeId, excludeRoleCode, normalizedQuery, pageable);
    }

    /**
     * Loads a page of users eligible to become a group member: any user not already a member of
     * the group, optionally filtered by a case-insensitive name/email search.
     *
     * @param excludeUserIds ids already a member of the target group
     * @param search         optional case-insensitive name/email search
     * @param pageable       page and sort request
     * @return matching candidate users page
     */
    @Transactional(readOnly = true)
    public Page<User> findGroupMemberCandidates(Collection<Long> excludeUserIds, String search, Pageable pageable) {
        return userRepository.findGroupMemberCandidates(safeExcludeIds(excludeUserIds), likeSearch(search), pageable);
    }

    /**
     * Loads a page of users eligible to become an additional group lead: an admin or team lead
     * not already leading the group, optionally filtered by a case-insensitive name/email search.
     *
     * @param excludeUserIds ids already leading the target group
     * @param search         optional case-insensitive name/email search
     * @param pageable       page and sort request
     * @return matching candidate users page
     */
    @Transactional(readOnly = true)
    public Page<User> findGroupLeadCandidates(Collection<Long> excludeUserIds, String search, Pageable pageable) {
        return userRepository.findGroupLeadCandidates(
            Set.of(UserRoleCode.ADMIN, UserRoleCode.TEAMLEAD),
            safeExcludeIds(excludeUserIds),
            likeSearch(search),
            pageable
        );
    }

    /**
     * Searches users, optionally restricted to one role code and/or a case-insensitive name/email
     * search, sorted with admins first, then team leads, then members, then name and email.
     *
     * @param roleCode optional role dictionary code to restrict results to
     * @param query optional case-insensitive name/email search
     * @param pageable page request
     * @return matching users page
     */
    @Transactional(readOnly = true)
    public Page<User> search(String roleCode, String query, Pageable pageable) {
        return userRepository.search(roleCode, likeSearch(query), pageable);
    }

    /**
     * Loads a page of users who are a member of any of the given groups, excluding one user
     * (typically the team lead resolving their own team's roster), optionally filtered by a
     * case-insensitive name/email search. Bounded by the requested page size rather than the
     * lead's total member count.
     *
     * @param groupIds  the groups to search members of
     * @param excludeId the user id to exclude from results
     * @param search    optional case-insensitive name/email search
     * @param pageable  page and sort request
     * @return matching member users page, or an empty page when {@code groupIds} is empty
     */
    @Transactional(readOnly = true)
    public Page<User> findGroupMemberUsersByGroupIdIn(Collection<Long> groupIds, Long excludeId, String search, Pageable pageable) {
        if (groupIds == null || groupIds.isEmpty()) {
            return Page.empty(pageable);
        }
        return userRepository.findGroupMemberUsersByGroupIdIn(groupIds, excludeId, likeSearch(search), pageable);
    }

    /**
     * Counts every user in the workspace.
     *
     * @return total user count
     */
    @Transactional(readOnly = true)
    public long count() {
        return userRepository.count();
    }

    /**
     * Counts users with the given role code.
     *
     * @param roleCode role dictionary code
     * @return matching user count
     */
    @Transactional(readOnly = true)
    public long countByRoleCode(String roleCode) {
        return userRepository.countByRole_Code(roleCode);
    }

    Collection<Long> safeExcludeIds(Collection<Long> excludeUserIds) {
        return (excludeUserIds == null || excludeUserIds.isEmpty()) ? Set.of(-1L) : excludeUserIds;
    }

    String likeSearch(String search) {
        String normalized = normalizeSearch(search);
        return normalized == null ? null : "%" + normalized.toLowerCase(Locale.ROOT) + "%";
    }

    /**
     * Loads a user by display name, case-insensitively.
     *
     * @param name display name
     * @return matching user, if present
     */
    @Transactional(readOnly = true)
    public Optional<User> findByNameIgnoreCase(String name) {
        return userRepository.findByNameIgnoreCase(name);
    }

    /**
     * Loads a proxy reference to a user without hitting the database until the proxy's fields
     * are accessed.
     *
     * @param id the user primary key
     * @return a {@link User} proxy for the given ID
     */
    @Transactional(readOnly = true)
    public User getReference(Long id) {
        return userRepository.getReferenceById(id);
    }

    /**
     * Persists a user.
     *
     * @param user the user to save
     * @return the saved {@link User}
     */
    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }

    String normalizeSearch(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        return query.trim();
    }
}
