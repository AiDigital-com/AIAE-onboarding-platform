package com.aidigital.aionboarding.service.user.services;

import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.user.models.AdminUserStatsRecord;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Resolves Clerk-authenticated users and manages profile reads and updates.
 */
public interface UserService {

    /**
     * Finds an existing user by Clerk id or email, or creates a new member account.
     *
     * @param clerkUserId Clerk subject identifier
     * @param email user email (trimmed and normalized to lowercase)
     * @param fullName optional display name used when creating a new user
     * @return authenticated application user snapshot
     * @throws com.aidigital.aionboarding.service.common.error.AppException when the member role
     *     dictionary entry is missing during user creation
     */
    AppUser resolveOrCreateFromClerk(String clerkUserId, String email, String fullName);

    /**
     * Updates the authenticated user's profile fields.
     *
     * @param viewer authenticated user updating their own profile
     * @param name optional display name
     * @param position optional job title
     * @param avatarStorageKey optional avatar storage key
     * @param avatarColor optional avatar color token
     * @return updated user record
     * @throws com.aidigital.aionboarding.service.common.error.AppException when the user record is missing
     */
    UserRecord updateProfile(AppUser viewer, String name, String position, String avatarStorageKey, String avatarColor);

    /**
     * Looks up a user by internal id.
     *
     * @param id internal user id
     * @return matching user record when found
     */
    Optional<UserRecord> findById(Long id);

    /**
     * Looks up a user by email address.
     *
     * @param email email address (trimmed and normalized to lowercase)
     * @return matching user record when found
     */
    Optional<UserRecord> findByEmail(String email);

    /**
     * Returns all users sorted by role priority, then name and email.
     *
     * @return all user records in the system
     */
    List<UserRecord> getAllUsers();

    /**
     * Returns a page of users for the Admin role-assignment view, optionally restricted to one
     * role and/or filtered by a case-insensitive name/email search.
     *
     * @param roleCode optional role code to restrict results to, one of {@code admin},
     *     {@code teamlead}, {@code member}
     * @param search optional case-insensitive name/email search
     * @param page zero-based page index
     * @param size maximum number of users returned in one response
     * @return matching users page, sorted with admins first, then team leads, then members
     */
    Page<UserRecord> listUsers(String roleCode, String search, int page, int size);

    /**
     * Returns workspace-wide user/admin/team-lead counts for the Admin page stats row,
     * independent of the current search/filter/page.
     *
     * @return the aggregate counts
     */
    AdminUserStatsRecord getAdminUserStats();

    /**
     * Returns users the caller may assign learning content to.
     *
     * @param viewer authenticated user; admins receive all other users, team leads receive their members
     * @return assignable users, or an empty list when the caller is not an admin or team lead
     */
    List<UserRecord> listAssignableUsers(AppUser viewer);

    /**
     * Returns a page of users the caller may assign learning content to.
     *
     * @param viewer authenticated user; admins receive all other users, team leads receive their members
     * @param query optional name/email search
     * @param pageable page and sort request
     * @return assignable users page
     */
    Page<UserRecord> listAssignableUsers(AppUser viewer, String query, Pageable pageable);

    /**
     * Sets or clears a user's grade. An admin may edit any user; a Team Lead may edit only users
     * who are members of a group they lead. Changing the grade re-evaluates standing group roadmap
     * assignments so the user is enrolled into any assignment their new grade now matches; it never
     * removes an existing enrollment.
     *
     * @param viewer authenticated caller
     * @param userId user whose grade is being set
     * @param gradeId new grade id, or {@code null} to clear the grade
     * @return updated user record
     * @throws com.aidigital.aionboarding.service.common.error.AppException when the user or grade is
     *     missing, or the caller cannot edit this user's grade
     */
    UserRecord updateGrade(AppUser viewer, Long userId, Long gradeId);

    /**
     * Sets a user's role to Admin, Team Lead, or User. Admin only. Existing group leadership and
     * membership records are left untouched; access checks re-evaluate the user's role on every
     * request, so a demoted Team Lead immediately loses group-management access even if a stale
     * group-lead row remains.
     *
     * @param viewer authenticated caller; must be an admin
     * @param userId user whose role is being changed
     * @param roleCode target role code, one of {@code admin}, {@code teamlead}, {@code member}
     * @return updated user record
     * @throws com.aidigital.aionboarding.service.common.error.AppException when the caller is not
     *     an admin or the user is missing
     */
    UserRecord assignRole(AppUser viewer, Long userId, String roleCode);
}
