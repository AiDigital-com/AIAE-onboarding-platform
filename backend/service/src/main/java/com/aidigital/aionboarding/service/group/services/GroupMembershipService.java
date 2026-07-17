package com.aidigital.aionboarding.service.group.services;

import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.user.models.UserRecord;

/**
 * Manages the structural composition of a group: its members and leads. Kept Admin-only via
 * {@code groups.manage} — a Team Lead may view and rename groups they lead, but does not manage
 * arbitrary membership.
 */
public interface GroupMembershipService {

    /**
     * Adds a member to a group and enrolls them into every standing group roadmap assignment
     * their current grade matches (or which has no grade filter).
     *
     * @param viewer       authenticated caller
     * @param groupId      group primary key
     * @param memberUserId user to add
     * @return the added member's user record
     * @throws com.aidigital.aionboarding.service.common.error.AppException when the caller lacks
     *     {@code groups.manage}, the group or user is missing, or the user is already a member
     */
    UserRecord addMember(AppUser viewer, Long groupId, Long memberUserId);

    /**
     * Removes a member from a group. Existing enrollments and progress are left untouched, and no
     * future group-assignment enrollment is created for them.
     *
     * @param viewer       authenticated caller
     * @param groupId      group primary key
     * @param memberUserId member to remove
     * @throws com.aidigital.aionboarding.service.common.error.AppException when the caller lacks {@code groups.manage}
     */
    void removeMember(AppUser viewer, Long groupId, Long memberUserId);

    /**
     * Assigns a user as an additional lead of a group. The user must already hold the Team Lead or
     * Admin role.
     *
     * @param viewer     authenticated caller
     * @param groupId    group primary key
     * @param leadUserId user to add as lead
     * @return the added lead's user record
     * @throws com.aidigital.aionboarding.service.common.error.AppException when the caller lacks
     *     {@code groups.manage}, the group or user is missing, or the user is not a team lead/admin
     */
    UserRecord addLead(AppUser viewer, Long groupId, Long leadUserId);

    /**
     * Removes a lead from a group.
     *
     * @param viewer     authenticated caller
     * @param groupId    group primary key
     * @param leadUserId lead to remove
     * @throws com.aidigital.aionboarding.service.common.error.AppException when the caller lacks {@code groups.manage}
     */
    void removeLead(AppUser viewer, Long groupId, Long leadUserId);
}
