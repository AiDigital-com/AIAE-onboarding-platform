package com.aidigital.aionboarding.service.group.services.impl;

import com.aidigital.aionboarding.domain.group.entities.Group;
import com.aidigital.aionboarding.domain.group.entities.GroupLead;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.domain.group.entities.GroupMember;
import com.aidigital.aionboarding.service.group.models.CreateGroupInput;
import com.aidigital.aionboarding.service.group.models.GroupDetailRecord;
import com.aidigital.aionboarding.service.group.models.GroupListQuery;
import com.aidigital.aionboarding.service.group.models.GroupMemberRecord;
import com.aidigital.aionboarding.service.group.models.GroupOrgStatsRecord;
import com.aidigital.aionboarding.service.group.models.GroupSummaryRecord;
import com.aidigital.aionboarding.service.group.models.UpdateGroupInput;
import com.aidigital.aionboarding.service.group.services.GroupService;
import com.aidigital.aionboarding.service.group.services.entity.GroupEntityService;
import com.aidigital.aionboarding.service.group.services.entity.GroupLeadEntityService;
import com.aidigital.aionboarding.service.group.services.entity.GroupMemberEntityService;
import com.aidigital.aionboarding.service.group.support.GroupAccessPolicy;
import com.aidigital.aionboarding.service.group.support.GroupRecordAssembler;
import com.aidigital.aionboarding.service.group.support.GroupSpecificationBuilder;
import com.aidigital.aionboarding.service.mappers.user.UserRecordMapper;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import com.aidigital.aionboarding.service.user.services.entity.UserEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService {

    private final GroupEntityService groupEntityService;
    private final GroupLeadEntityService groupLeadEntityService;
    private final GroupMemberEntityService groupMemberEntityService;
    private final UserEntityService userEntityService;
    private final GroupAccessPolicy groupAccessPolicy;
    private final GroupSpecificationBuilder groupSpecificationBuilder;
    private final GroupRecordAssembler groupRecordAssembler;
    private final UserRecordMapper userMapper;
    private final PermissionService permissionService;
    private final CurrentTime currentTime;

    @Override
    @Transactional(readOnly = true)
    public Page<GroupSummaryRecord> listGroups(AppUser viewer, String searchText, int page, int size) {
        Set<Long> restrictToGroupIds = resolveVisibleGroupIds(viewer);
        if (restrictToGroupIds != null && restrictToGroupIds.isEmpty()) {
            return Page.empty();
        }

        GroupListQuery query = new GroupListQuery(searchText, restrictToGroupIds);
        Specification<Group> specification = groupSpecificationBuilder.build(query);
        Page<Group> groupsPage = groupEntityService.search(specification, page, size);
        List<GroupSummaryRecord> summaries = groupRecordAssembler.toSummaryRecords(groupsPage.getContent());
        return new PageImpl<>(summaries, groupsPage.getPageable(), groupsPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public GroupOrgStatsRecord getOrgStats(AppUser viewer) {
        Set<Long> restrictToGroupIds = resolveVisibleGroupIds(viewer);
        if (restrictToGroupIds != null && restrictToGroupIds.isEmpty()) {
            return new GroupOrgStatsRecord(0, 0, 0);
        }
        if (restrictToGroupIds == null) {
            return new GroupOrgStatsRecord(
                (int) groupEntityService.count(),
                (int) groupMemberEntityService.countDistinctMemberUsers(),
                (int) groupLeadEntityService.countDistinctLeadUsers());
        }
        return new GroupOrgStatsRecord(
            restrictToGroupIds.size(),
            (int) groupMemberEntityService.countDistinctMemberUsers(restrictToGroupIds),
            (int) groupLeadEntityService.countDistinctLeadUsers(restrictToGroupIds));
    }

    /**
     * @return {@code null} when unrestricted (Admin), or the exact set of visible group ids
     *     otherwise (possibly empty, for a viewer who leads no groups)
     */
    Set<Long> resolveVisibleGroupIds(AppUser viewer) {
        if (viewer == null) {
            return Set.of();
        }
        if (viewer.isAdmin()) {
            return null;
        }
        if (!viewer.isTeamLead()) {
            return Set.of();
        }
        return groupLeadEntityService.findGroupIdsByLeadUserId(viewer.internalId());
    }

    @Override
    @Transactional(readOnly = true)
    public GroupDetailRecord getGroup(AppUser viewer, Long groupId) {
        Group group = requireGroup(groupId);
        requireCanManage(viewer, groupId);
        return groupRecordAssembler.toDetailRecord(group);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GroupMemberRecord> listGroupMembers(AppUser viewer, Long groupId, String search, int page, int size) {
        requireGroup(groupId);
        requireCanManage(viewer, groupId);
        Page<GroupMember> members = groupMemberEntityService.findByGroupId(groupId, search, PageRequest.of(page, size));
        return groupRecordAssembler.toMemberRecordPage(members);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserRecord> listCandidateUsers(AppUser viewer, Long groupId, boolean forLeads, String search, int page, int size) {
        permissionService.requirePermission(viewer, PermissionKeys.GROUPS_MANAGE);
        requireGroup(groupId);
        Pageable pageable = PageRequest.of(page, size);
        if (forLeads) {
            Set<Long> excludeIds = groupLeadEntityService.findByGroupIdIn(List.of(groupId)).stream()
                .map(lead -> lead.getId().getLeadUserId())
                .collect(Collectors.toSet());
            return userEntityService.findGroupLeadCandidates(excludeIds, search, pageable).map(userMapper::toRecord);
        }
        Set<Long> excludeIds = groupMemberEntityService.findByGroupId(groupId).stream()
            .map(member -> member.getId().getMemberUserId())
            .collect(Collectors.toSet());
        return userEntityService.findGroupMemberCandidates(excludeIds, search, pageable).map(userMapper::toRecord);
    }

    @Override
    @Transactional
    public GroupDetailRecord createGroup(AppUser viewer, CreateGroupInput input) {
        permissionService.requirePermission(viewer, PermissionKeys.GROUPS_MANAGE);
        String name = requireValidName(input.name());
        String normalizedName = normalize(name);
        if (groupEntityService.existsByNormalizedName(normalizedName)) {
            throw new AppException(ErrorReason.C006, "A team with this name already exists.");
        }

        Group group = new Group();
        group.setName(name);
        group.setNormalizedName(normalizedName);
        group.setDescription(input.description() == null ? "" : input.description().trim());
        group.setCreatedByUser(userEntityService.getReference(viewer.internalId()));
        LocalDateTime now = currentTime.utcDateTime();
        group.setCreatedAt(now);
        group.setUpdatedAt(now);
        Group saved = groupEntityService.save(group);

        // A non-admin creator (team lead) sees and manages only groups they lead
        // (see resolveVisibleGroupIds). Auto-assign them as a lead so the team
        // they just created is visible and manageable to them. Admins see every
        // group unrestricted, so they need no lead row.
        if (!viewer.isAdmin()) {
            assignCreatorAsLead(saved, viewer, now);
        }
        return groupRecordAssembler.toDetailRecord(saved);
    }

    /**
     * Records the group's creator as one of its leads.
     *
     * @param group the freshly persisted group
     * @param viewer the authenticated creator
     * @param now the creation timestamp to stamp on the lead assignment
     */
    void assignCreatorAsLead(Group group, AppUser viewer, LocalDateTime now) {
        GroupLead groupLead = new GroupLead();
        GroupLead.GroupLeadId id = new GroupLead.GroupLeadId();
        id.setGroupId(group.getId());
        id.setLeadUserId(viewer.internalId());
        groupLead.setId(id);
        groupLead.setGroup(group);
        groupLead.setLeadUser(userEntityService.getReference(viewer.internalId()));
        groupLead.setCreatedAt(now);
        groupLeadEntityService.save(groupLead);
    }

    @Override
    @Transactional
    public GroupDetailRecord updateGroup(AppUser viewer, Long groupId, UpdateGroupInput input) {
        Group group = requireGroup(groupId);
        requireCanManage(viewer, groupId);

        String name = requireValidName(input.name());
        String normalizedName = normalize(name);
        if (groupEntityService.existsByNormalizedNameExcluding(normalizedName, groupId)) {
            throw new AppException(ErrorReason.C006, "A team with this name already exists.");
        }

        group.setName(name);
        group.setNormalizedName(normalizedName);
        group.setDescription(input.description() == null ? "" : input.description().trim());
        group.setUpdatedAt(currentTime.utcDateTime());
        Group saved = groupEntityService.save(group);
        return groupRecordAssembler.toDetailRecord(saved);
    }

    @Override
    @Transactional
    public void deleteGroup(AppUser viewer, Long groupId) {
        permissionService.requirePermission(viewer, PermissionKeys.GROUPS_MANAGE);
        Group group = requireGroup(groupId);
        groupEntityService.delete(group);
    }

    Group requireGroup(Long groupId) {
        return groupEntityService.findById(groupId).orElseThrow(() -> new AppException(ErrorReason.C001, "Group not found."));
    }

    void requireCanManage(AppUser viewer, Long groupId) {
        if (!groupAccessPolicy.canManageGroup(viewer, groupId)) {
            throw new AppException(ErrorReason.C004);
        }
    }

    String requireValidName(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.length() < 3 || trimmed.length() > 100) {
            throw new AppException(ErrorReason.C002, "Team name must be 3-100 characters.");
        }
        return trimmed;
    }

    String normalize(String name) {
        return name.toLowerCase(Locale.ROOT).trim();
    }
}
