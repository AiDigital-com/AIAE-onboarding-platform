package com.aidigital.aionboarding.service.group.support;

import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.group.services.entity.GroupLeadEntityService;
import com.aidigital.aionboarding.service.group.services.entity.GroupMemberEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;

/**
 * Encapsulates the "who may act on this group" rule shared by rename, member-grade-edit, and
 * roadmap-assignment operations: Admins manage every group; a Team Lead manages only groups they
 * lead. Structural membership/lead management (create, delete, add/remove member or lead) is
 * gated separately, by the {@code groups.manage} permission, since the product intentionally
 * keeps that Admin-only.
 */
@Component
@RequiredArgsConstructor
public class GroupAccessPolicy {

    private final GroupLeadEntityService groupLeadEntityService;
    private final GroupMemberEntityService groupMemberEntityService;

    /**
     * Checks whether the viewer may rename, edit member grades for, or assign roadmaps to a group.
     *
     * @param viewer  authenticated caller
     * @param groupId group primary key
     * @return {@code true} when the viewer is an admin or leads the group
     */
    public boolean canManageGroup(AppUser viewer, Long groupId) {
        if (viewer == null || groupId == null) {
            return false;
        }
        if (viewer.isAdmin()) {
            return true;
        }
        return viewer.isTeamLead() && groupLeadEntityService.existsByGroupIdAndLeadUserId(groupId, viewer.internalId());
    }

    /**
     * Checks whether the viewer may edit the given user's grade: an admin always may; a Team Lead
     * may only when the target user is a member of at least one group the viewer leads.
     *
     * @param viewer       authenticated caller
     * @param targetUserId user whose grade would be edited
     * @return {@code true} when the viewer may edit this user's grade
     */
    public boolean canEditMemberGrade(AppUser viewer, Long targetUserId) {
        if (viewer == null || targetUserId == null) {
            return false;
        }
        if (viewer.isAdmin()) {
            return true;
        }
        if (!viewer.isTeamLead()) {
            return false;
        }
        Set<Long> ledGroupIds = groupLeadEntityService.findGroupIdsByLeadUserId(viewer.internalId());
        if (ledGroupIds.isEmpty()) {
            return false;
        }
        Set<Long> memberGroupIds = groupMemberEntityService.findGroupIdsByMemberUserId(targetUserId);
        return !Collections.disjoint(ledGroupIds, memberGroupIds);
    }
}
