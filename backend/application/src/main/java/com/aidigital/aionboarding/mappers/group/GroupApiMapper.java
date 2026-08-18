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

    @Mapping(target = "stats", source = "stats")
    GroupOrgStatsResponseV1 toGroupOrgStatsResponseV1(GroupOrgStatsRecord stats);

    GroupV1 toGroupV1(GroupDetailRecord group);

    GroupMemberV1 toGroupMemberV1(GroupMemberRecord member);

    @Mapping(target = "group", source = "group")
    GroupResponseV1 toGroupResponseV1(GroupDetailRecord group);

    @Mapping(target = "groups", expression = "java(groups.stream().map(this::toGroupSummaryV1).toList())")
    @Mapping(target = "page", expression = "java(toPageInfoV1(groups))")
    GroupsListResponseV1 toGroupsListResponseV1(Page<GroupSummaryRecord> groups);

    @Mapping(target = "members", expression = "java(members.stream().map(this::toGroupMemberV1).toList())")
    @Mapping(target = "page", expression = "java(toPageInfoV1(members))")
    GroupMembersListResponseV1 toGroupMembersListResponseV1(Page<GroupMemberRecord> members);

    @Mapping(target = "users", source = "users")
    @Mapping(target = "page", expression = "java(toPageInfoV1(page))")
    GroupCandidateUsersListResponseV1 toGroupCandidateUsersListResponseV1(
        List<UserSummaryV1> users,
        Page<?> page
    );

    /**
     * Builds the candidate-users list response directly from the paged query result, mapping
     * each candidate through the given {@link UserApiMapper} since a mapper default method
     * cannot reach another mapper's {@code uses}-injected instance.
     *
     * @param candidates   paged candidate users
     * @param userApiMapper mapper used to convert each candidate to its summary shape
     * @return the candidate-users list response
     */
    default GroupCandidateUsersListResponseV1 toGroupCandidateUsersListResponseV1(
        Page<UserRecord> candidates,
        UserApiMapper userApiMapper
    ) {
        return toGroupCandidateUsersListResponseV1(
            candidates.stream().map(userApiMapper::toUserSummaryV1).toList(),
            candidates
        );
    }

    @Mapping(target = "member", source = "member")
    AddGroupMemberResponseV1 toAddGroupMemberResponseV1(UserRecord member);

    @Mapping(target = "lead", source = "lead")
    AddGroupLeadResponseV1 toAddGroupLeadResponseV1(UserRecord lead);
}
