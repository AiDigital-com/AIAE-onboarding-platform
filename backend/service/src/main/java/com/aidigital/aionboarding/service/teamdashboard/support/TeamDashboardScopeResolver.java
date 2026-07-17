package com.aidigital.aionboarding.service.teamdashboard.support;

import com.aidigital.aionboarding.domain.common.dictionary.UserRoleCode;
import com.aidigital.aionboarding.domain.group.entities.GroupMember;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.group.services.entity.GroupLeadEntityService;
import com.aidigital.aionboarding.service.group.services.entity.GroupMemberEntityService;
import com.aidigital.aionboarding.service.mappers.user.UserRecordMapper;
import com.aidigital.aionboarding.service.teamdashboard.models.TeamDashboardScope;
import com.aidigital.aionboarding.service.teamdashboard.util.TeamDashboardSupport;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import com.aidigital.aionboarding.service.user.services.entity.UserEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Resolves which users' learning progress a viewer may see on the Team Progress dashboard. Admins
 * see the whole workspace even when they also lead a group. A Team Lead sees the union of every
 * group they lead — a lead may lead more than one group. Anyone else sees an empty scope.
 */
@Component
@RequiredArgsConstructor
public class TeamDashboardScopeResolver {

    private final GroupLeadEntityService groupLeadEntityService;
    private final GroupMemberEntityService groupMemberEntityService;
    private final UserEntityService userEntityService;
    private final UserRecordMapper userMapper;
    private final TeamDashboardSupport teamDashboardSupport;

    public TeamDashboardScope resolveVisibleTeamScope(AppUser viewer) {
        if (viewer == null) {
            return new TeamDashboardScope("My team", null, List.of());
        }

        if (viewer.isAdmin()) {
            return scopeForAdmin(viewer);
        }

        Set<Long> ledGroupIds = groupLeadEntityService.findGroupIdsByLeadUserId(viewer.internalId());
        if (!ledGroupIds.isEmpty()) {
            return scopeForLedGroups(viewer, ledGroupIds);
        }

        return new TeamDashboardScope("My team", null, List.of());
    }

    TeamDashboardScope scopeForLedGroups(AppUser viewer, Set<Long> groupIds) {
        List<UserRecord> members = new ArrayList<>();
        userEntityService.findById(viewer.internalId()).map(userMapper::toRecord).ifPresent(members::add);
        for (GroupMember groupMember : groupMemberEntityService.findByGroupIdIn(groupIds)) {
            members.add(userMapper.toRecord(groupMember.getMemberUser()));
        }
        return new TeamDashboardScope(
            "My team",
            viewer.internalId(),
            teamDashboardSupport.uniqById(members, UserRecord::id)
        );
    }

    TeamDashboardScope scopeForAdmin(AppUser viewer) {
        List<UserRecord> groupMembers = groupMemberEntityService.findAllWithMembers().stream()
            .map(groupMember -> userMapper.toRecord(groupMember.getMemberUser()))
            .toList();
        if (!groupMembers.isEmpty()) {
            return new TeamDashboardScope("Organization", null, teamDashboardSupport.uniqById(groupMembers, UserRecord::id));
        }
        List<UserRecord> users = userEntityService.findAll().stream()
            .filter(user -> !user.getId().equals(viewer.internalId()))
            .filter(user -> !UserRoleCode.ADMIN.equals(user.getRole().getCode()))
            .map(userMapper::toRecord)
            .toList();
        return new TeamDashboardScope("Organization", null, users);
    }
}
