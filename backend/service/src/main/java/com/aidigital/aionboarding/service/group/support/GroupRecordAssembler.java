package com.aidigital.aionboarding.service.group.support;

import com.aidigital.aionboarding.domain.group.entities.Group;
import com.aidigital.aionboarding.domain.group.entities.GroupLead;
import com.aidigital.aionboarding.domain.group.entities.GroupMember;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.group.models.GroupDetailRecord;
import com.aidigital.aionboarding.service.group.models.GroupMemberRecord;
import com.aidigital.aionboarding.service.group.models.GroupSummaryRecord;
import com.aidigital.aionboarding.service.group.services.entity.GroupLeadEntityService;
import com.aidigital.aionboarding.service.group.services.entity.GroupMemberEntityService;
import com.aidigital.aionboarding.service.mappers.user.UserRecordMapper;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Assembles {@link GroupSummaryRecord}/{@link GroupDetailRecord} from {@link Group} entities,
 * batching lead lookups and member counts across a page of groups so a group list never issues a
 * query per row; also builds new/updated {@link Group} and {@link GroupLead} entity shapes so
 * timestamp stamping lives in one place rather than duplicated at each call site.
 */
@Component
@RequiredArgsConstructor
public class GroupRecordAssembler {

    private final GroupLeadEntityService groupLeadEntityService;
    private final GroupMemberEntityService groupMemberEntityService;
    private final UserRecordMapper userMapper;
    private final CurrentTime currentTime;

    /**
     * Builds summary records for a page of groups, batching leads and member counts in one round
     * trip per collection rather than one per group.
     *
     * @param groups the groups to summarize
     * @return summary records in the same order as {@code groups}
     */
    public List<GroupSummaryRecord> toSummaryRecords(List<Group> groups) {
        if (groups.isEmpty()) {
            return List.of();
        }
        List<Long> groupIds = groups.stream().map(Group::getId).toList();

        Map<Long, List<UserRecord>> leadsByGroupId = new LinkedHashMap<>();
        for (GroupLead groupLead : groupLeadEntityService.findByGroupIdIn(groupIds)) {
            leadsByGroupId.computeIfAbsent(groupLead.getId().getGroupId(), ignored -> new ArrayList<>())
                .add(userMapper.toRecord(groupLead.getLeadUser()));
        }
        Map<Long, Long> membersCountByGroupId = groupMemberEntityService.countByGroupIdBatch(groupIds);
        Map<Long, Long> membersWithoutGradeByGroupId = groupMemberEntityService.countMembersWithoutGradeBatch(groupIds);

        return groups.stream()
            .map(group -> new GroupSummaryRecord(
                group.getId(),
                group.getName(),
                group.getDescription(),
                leadsByGroupId.getOrDefault(group.getId(), List.of()),
                membersCountByGroupId.getOrDefault(group.getId(), 0L),
                membersWithoutGradeByGroupId.getOrDefault(group.getId(), 0L),
                group.getCreatedAt(),
                group.getUpdatedAt()
            ))
            .toList();
    }

    /**
     * Builds the full detail record for one group, including its member list.
     *
     * @param group the group to assemble
     * @return the assembled detail record
     */
    public GroupDetailRecord toDetailRecord(Group group) {
        List<UserRecord> leads = groupLeadEntityService.findByGroupIdIn(List.of(group.getId())).stream()
            .map(GroupLead::getLeadUser)
            .map(userMapper::toRecord)
            .toList();
        return new GroupDetailRecord(
            group.getId(),
            group.getName(),
            group.getDescription(),
            leads,
            group.getCreatedAt(),
            group.getUpdatedAt()
        );
    }

    /**
     * Maps one page of {@link GroupMember} entities to {@link GroupMemberRecord}, preserving
     * paging metadata.
     *
     * @param members the page of memberships to map
     * @return the mapped page
     */
    public Page<GroupMemberRecord> toMemberRecordPage(Page<GroupMember> members) {
        List<GroupMemberRecord> content = members.getContent().stream().map(this::toMemberRecord).toList();
        return new PageImpl<>(content, members.getPageable(), members.getTotalElements());
    }

    GroupMemberRecord toMemberRecord(GroupMember groupMember) {
        return new GroupMemberRecord(userMapper.toRecord(groupMember.getMemberUser()), groupMember.getCreatedAt());
    }

    /**
     * Maps a candidate user entity to its API record shape.
     *
     * @param user candidate user
     * @return the mapped record
     */
    public UserRecord toUserRecord(User user) {
        return userMapper.toRecord(user);
    }

    /**
     * Builds a new, unsaved {@link Group} with normalized name, description, creator, and
     * created/updated timestamps stamped to now.
     *
     * @param name            display name
     * @param normalizedName  lowercase/trimmed name used for uniqueness checks
     * @param description     description, already defaulted to {@code ""} when absent
     * @param createdByUser   reference to the creating user
     * @return the constructed (not yet persisted) group
     */
    public Group buildNewGroup(String name, String normalizedName, String description, User createdByUser) {
        Group group = new Group();
        group.setName(name);
        group.setNormalizedName(normalizedName);
        group.setDescription(description);
        group.setCreatedByUser(createdByUser);
        LocalDateTime now = currentTime.utcDateTime();
        group.setCreatedAt(now);
        group.setUpdatedAt(now);
        return group;
    }

    /**
     * Builds a new, unsaved {@link GroupLead} linking a group to its lead, stamped with the
     * current time.
     *
     * @param group      the group being led
     * @param leadUserId the lead's internal user id
     * @param leadUser   reference to the lead user
     * @return the constructed (not yet persisted) lead assignment
     */
    public GroupLead buildGroupLead(Group group, Long leadUserId, User leadUser) {
        GroupLead groupLead = new GroupLead();
        GroupLead.GroupLeadId id = new GroupLead.GroupLeadId();
        id.setGroupId(group.getId());
        id.setLeadUserId(leadUserId);
        groupLead.setId(id);
        groupLead.setGroup(group);
        groupLead.setLeadUser(leadUser);
        groupLead.setCreatedAt(currentTime.utcDateTime());
        return groupLead;
    }

    /**
     * Applies a rename/description update to an existing group in place and stamps its updated
     * timestamp to now.
     *
     * @param group          the group being updated
     * @param name           new display name
     * @param normalizedName new lowercase/trimmed name used for uniqueness checks
     * @param description    new description, already defaulted to {@code ""} when absent
     */
    public void applyUpdate(Group group, String name, String normalizedName, String description) {
        group.setName(name);
        group.setNormalizedName(normalizedName);
        group.setDescription(description);
        group.setUpdatedAt(currentTime.utcDateTime());
    }
}
