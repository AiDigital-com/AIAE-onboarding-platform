package com.aidigital.aionboarding.service.group.services.impl;

import com.aidigital.aionboarding.domain.common.dictionary.UserRoleCode;
import com.aidigital.aionboarding.domain.group.entities.Group;
import com.aidigital.aionboarding.domain.group.entities.GroupLead;
import com.aidigital.aionboarding.domain.group.entities.GroupMember;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.group.services.GroupMembershipService;
import com.aidigital.aionboarding.service.group.services.entity.GroupEntityService;
import com.aidigital.aionboarding.service.group.services.entity.GroupLeadEntityService;
import com.aidigital.aionboarding.service.group.services.entity.GroupMemberEntityService;
import com.aidigital.aionboarding.service.mappers.user.UserRecordMapper;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import com.aidigital.aionboarding.service.roadmap.services.RoadmapGroupAssignmentSyncService;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import com.aidigital.aionboarding.service.user.services.entity.UserEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GroupMembershipServiceImpl implements GroupMembershipService {

    private final GroupEntityService groupEntityService;
    private final GroupMemberEntityService groupMemberEntityService;
    private final GroupLeadEntityService groupLeadEntityService;
    private final UserEntityService userEntityService;
    private final RoadmapGroupAssignmentSyncService roadmapGroupAssignmentSyncService;
    private final PermissionService permissionService;
    private final UserRecordMapper userMapper;
    private final CurrentTime currentTime;

    @Override
    @Transactional
    public UserRecord addMember(AppUser viewer, Long groupId, Long memberUserId) {
        permissionService.requirePermission(viewer, PermissionKeys.GROUPS_MANAGE);
        Group group = requireGroup(groupId);
        User member = requireUser(memberUserId);
        if (groupMemberEntityService.existsByGroupIdAndMemberUserId(groupId, memberUserId)) {
            throw new AppException(ErrorReason.C006, "This user is already a member of the group.");
        }

        GroupMember groupMember = new GroupMember();
        GroupMember.GroupMemberId id = new GroupMember.GroupMemberId();
        id.setGroupId(groupId);
        id.setMemberUserId(memberUserId);
        groupMember.setId(id);
        groupMember.setGroup(group);
        groupMember.setMemberUser(member);
        groupMember.setCreatedAt(currentTime.utcDateTime());
        groupMemberEntityService.save(groupMember);

        roadmapGroupAssignmentSyncService.syncNewGroupMember(groupId, memberUserId);
        return userMapper.toRecord(member);
    }

    @Override
    @Transactional
    public void removeMember(AppUser viewer, Long groupId, Long memberUserId) {
        permissionService.requirePermission(viewer, PermissionKeys.GROUPS_MANAGE);
        GroupMember.GroupMemberId id = new GroupMember.GroupMemberId();
        id.setGroupId(groupId);
        id.setMemberUserId(memberUserId);
        groupMemberEntityService.deleteById(id);
    }

    @Override
    @Transactional
    public UserRecord addLead(AppUser viewer, Long groupId, Long leadUserId) {
        permissionService.requirePermission(viewer, PermissionKeys.GROUPS_MANAGE);
        Group group = requireGroup(groupId);
        User lead = requireUser(leadUserId);
        String roleCode = lead.getRole().getCode();
        if (!UserRoleCode.ADMIN.equals(roleCode) && !UserRoleCode.TEAMLEAD.equals(roleCode)) {
            throw new AppException(ErrorReason.C002, "A group lead must be an admin or team lead.");
        }
        if (groupLeadEntityService.existsByGroupIdAndLeadUserId(groupId, leadUserId)) {
            throw new AppException(ErrorReason.C006, "This user is already a lead of the group.");
        }

        GroupLead groupLead = new GroupLead();
        GroupLead.GroupLeadId id = new GroupLead.GroupLeadId();
        id.setGroupId(groupId);
        id.setLeadUserId(leadUserId);
        groupLead.setId(id);
        groupLead.setGroup(group);
        groupLead.setLeadUser(lead);
        groupLead.setCreatedAt(currentTime.utcDateTime());
        groupLeadEntityService.save(groupLead);
        return userMapper.toRecord(lead);
    }

    @Override
    @Transactional
    public void removeLead(AppUser viewer, Long groupId, Long leadUserId) {
        permissionService.requirePermission(viewer, PermissionKeys.GROUPS_MANAGE);
        GroupLead.GroupLeadId id = new GroupLead.GroupLeadId();
        id.setGroupId(groupId);
        id.setLeadUserId(leadUserId);
        groupLeadEntityService.deleteById(id);
    }

    Group requireGroup(Long groupId) {
        return groupEntityService.findById(groupId).orElseThrow(() -> new AppException(ErrorReason.C001, "Group not found."));
    }

    User requireUser(Long userId) {
        return userEntityService.findById(userId).orElseThrow(() -> new AppException(ErrorReason.C001, userId));
    }
}
