package com.aidigital.aionboarding.service.group.services;

import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.group.models.CreateGroupInput;
import com.aidigital.aionboarding.service.group.models.GroupDetailRecord;
import com.aidigital.aionboarding.service.group.models.GroupMemberRecord;
import com.aidigital.aionboarding.service.group.models.GroupOrgStatsRecord;
import com.aidigital.aionboarding.service.group.models.GroupSummaryRecord;
import com.aidigital.aionboarding.service.group.models.UpdateGroupInput;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import org.springframework.data.domain.Page;

/**
 * Manages the lifecycle of explicit, independently named groups: creation, renaming, deletion,
 * and scoped search. Structural membership/lead changes live in {@code GroupMembershipService}.
 */
public interface GroupService {

    /**
     * Searches groups visible to the viewer. Admins see every group; a Team Lead sees only groups
     * they lead.
     *
     * @param viewer     authenticated caller
     * @param searchText matched against group name and lead name/email; {@code null} to skip
     * @param page       zero-based page index
     * @param size       page size
     * @return matching page of group summaries
     */
    Page<GroupSummaryRecord> listGroups(AppUser viewer, String searchText, int page, int size);

    /**
     * Computes aggregate team/member/lead counts, scoped the same way as {@link #listGroups}.
     * Admins get workspace-wide totals; a Team Lead gets totals for only the groups they lead.
     *
     * @param viewer authenticated caller
     * @return aggregate counts, all zero when the viewer leads no groups
     */
    GroupOrgStatsRecord getOrgStats(AppUser viewer);

    /**
     * Loads full group detail, including its members.
     *
     * @param viewer  authenticated caller
     * @param groupId group primary key
     * @return the group detail
     * @throws com.aidigital.aionboarding.service.common.error.AppException when the group is
     *     missing or the viewer cannot manage it
     */
    GroupDetailRecord getGroup(AppUser viewer, Long groupId);

    /**
     * Loads one page of a group's members, optionally filtered by a case-insensitive name/email
     * search. Members are always paged so a large group never loads its entire roster at once.
     *
     * @param viewer  authenticated caller
     * @param groupId group primary key
     * @param search  optional case-insensitive name/email search
     * @param page    zero-based page index
     * @param size    page size
     * @return matching page of group members
     * @throws com.aidigital.aionboarding.service.common.error.AppException when the group is
     *     missing or the viewer cannot manage it
     */
    Page<GroupMemberRecord> listGroupMembers(AppUser viewer, Long groupId, String search, int page, int size);

    /**
     * Loads one page of users eligible to be added to a group, excluding whoever already holds
     * that role in the group. Admin only, since only an Admin can add members or leads.
     *
     * @param viewer   authenticated caller; must have {@code groups.manage}
     * @param groupId  group primary key
     * @param forLeads {@code true} to search lead candidates (admin/team lead role, excluding
     *                 current leads); {@code false} to search member candidates (any role,
     *                 excluding current members)
     * @param search   optional case-insensitive name/email search
     * @param page     zero-based page index
     * @param size     page size
     * @return matching page of candidate users
     * @throws com.aidigital.aionboarding.service.common.error.AppException when the caller lacks
     *     {@code groups.manage} or the group is missing
     */
    Page<UserRecord> listCandidateUsers(AppUser viewer, Long groupId, boolean forLeads, String search, int page, int size);

    /**
     * Creates a new group. Only an Admin may create groups.
     *
     * @param viewer authenticated caller
     * @param input  required name and optional description
     * @return the created group's detail
     * @throws com.aidigital.aionboarding.service.common.error.AppException when the caller lacks
     *     {@code groups.manage}, the name is invalid, or the name is already in use
     */
    GroupDetailRecord createGroup(AppUser viewer, CreateGroupInput input);

    /**
     * Renames/updates a group. Admins may rename any group; a Team Lead may rename only groups
     * they lead. Renaming changes display only — assignments, membership, and progress are
     * untouched.
     *
     * @param viewer  authenticated caller
     * @param groupId group primary key
     * @param input   new name and optional description
     * @return the updated group's detail
     * @throws com.aidigital.aionboarding.service.common.error.AppException when the group is
     *     missing, the caller cannot manage it, the name is invalid, or the name is already in use
     */
    GroupDetailRecord updateGroup(AppUser viewer, Long groupId, UpdateGroupInput input);

    /**
     * Deletes a group. Only an Admin may delete groups.
     *
     * @param viewer  authenticated caller
     * @param groupId group primary key
     * @throws com.aidigital.aionboarding.service.common.error.AppException when the caller lacks
     *     {@code groups.manage} or the group is missing
     */
    void deleteGroup(AppUser viewer, Long groupId);
}
