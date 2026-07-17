package com.aidigital.aionboarding.mappers.group;

import com.aidigital.aionboarding.api.v1.model.AddGroupLeadResponseV1;
import com.aidigital.aionboarding.api.v1.model.AddGroupMemberResponseV1;
import com.aidigital.aionboarding.api.v1.model.GroupCandidateUsersListResponseV1;
import com.aidigital.aionboarding.api.v1.model.GroupMemberV1;
import com.aidigital.aionboarding.api.v1.model.GroupMembersListResponseV1;
import com.aidigital.aionboarding.api.v1.model.GroupOrgStatsResponseV1;
import com.aidigital.aionboarding.api.v1.model.GroupOrgStatsV1;
import com.aidigital.aionboarding.api.v1.model.GroupResponseV1;
import com.aidigital.aionboarding.api.v1.model.GroupSummaryV1;
import com.aidigital.aionboarding.api.v1.model.GroupV1;
import com.aidigital.aionboarding.api.v1.model.GroupsListResponseV1;
import com.aidigital.aionboarding.api.v1.model.UserSummaryV1;
import com.aidigital.aionboarding.config.ApplicationMapperConfig;
import com.aidigital.aionboarding.mappers.common.PageInfoApiMapper;
import com.aidigital.aionboarding.mappers.user.UserApiMapper;
import com.aidigital.aionboarding.service.group.models.GroupDetailRecord;
import com.aidigital.aionboarding.service.group.models.GroupMemberRecord;
import com.aidigital.aionboarding.service.group.models.GroupOrgStatsRecord;
import com.aidigital.aionboarding.service.group.models.GroupSummaryRecord;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(config = ApplicationMapperConfig.class, uses = { UserApiMapper.class, PageInfoApiMapper.class })
public interface GroupApiMapper extends PageInfoApiMapper {

    GroupSummaryV1 toGroupSummaryV1(GroupSummaryRecord group);

    GroupOrgStatsV1 toGroupOrgStatsV1(GroupOrgStatsRecord stats);

    default GroupOrgStatsResponseV1 toGroupOrgStatsResponseV1(GroupOrgStatsRecord stats) {
        GroupOrgStatsResponseV1 response = new GroupOrgStatsResponseV1();
        response.setStats(toGroupOrgStatsV1(stats));
        return response;
    }

    GroupV1 toGroupV1(GroupDetailRecord group);

    GroupMemberV1 toGroupMemberV1(GroupMemberRecord member);

    @Mapping(target = "group", source = "group")
    GroupResponseV1 toGroupResponseV1(GroupDetailRecord group);

    default GroupsListResponseV1 toGroupsListResponseV1(Page<GroupSummaryRecord> groups) {
        GroupsListResponseV1 response = new GroupsListResponseV1();
        response.setGroups(groups.stream().map(this::toGroupSummaryV1).toList());
        response.setPage(toPageInfoV1(groups));
        return response;
    }

    default GroupMembersListResponseV1 toGroupMembersListResponseV1(Page<GroupMemberRecord> members) {
        GroupMembersListResponseV1 response = new GroupMembersListResponseV1();
        response.setMembers(members.stream().map(this::toGroupMemberV1).toList());
        response.setPage(toPageInfoV1(members));
        return response;
    }

    default GroupCandidateUsersListResponseV1 toGroupCandidateUsersListResponseV1(
        List<UserSummaryV1> users,
        Page<?> page
    ) {
        GroupCandidateUsersListResponseV1 response = new GroupCandidateUsersListResponseV1();
        response.setUsers(users);
        response.setPage(toPageInfoV1(page));
        return response;
    }

    @Mapping(target = "member", source = "member")
    AddGroupMemberResponseV1 toAddGroupMemberResponseV1(UserRecord member);

    @Mapping(target = "lead", source = "lead")
    AddGroupLeadResponseV1 toAddGroupLeadResponseV1(UserRecord lead);
}
