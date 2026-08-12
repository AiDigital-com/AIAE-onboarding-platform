package com.aidigital.aionboarding.mappers.team;

import com.aidigital.aionboarding.api.v1.model.AddTeamMemberRequestV1;
import com.aidigital.aionboarding.api.v1.model.AddTeamMemberResponseV1;
import com.aidigital.aionboarding.api.v1.model.TeamLeadAdminViewV1;
import com.aidigital.aionboarding.api.v1.model.TeamV1;
import com.aidigital.aionboarding.api.v1.model.TeamsResponseV1;
import com.aidigital.aionboarding.api.v1.model.UserRoleCodeV1;
import com.aidigital.aionboarding.api.v1.model.UserSummaryV1;
import com.aidigital.aionboarding.config.ApplicationMapperConfig;
import com.aidigital.aionboarding.mappers.common.PageInfoApiMapper;
import com.aidigital.aionboarding.mappers.user.UserApiMapper;
import com.aidigital.aionboarding.service.team.models.TeamRecord;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import java.util.List;
import java.util.Map;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.data.domain.Page;

@Mapper(config = ApplicationMapperConfig.class, uses = { UserApiMapper.class, PageInfoApiMapper.class })
public interface TeamApiMapper extends PageInfoApiMapper {

    TeamV1 toTeamV1(TeamRecord team);

    @Mapping(target = "teams", source = "teams")
    @Mapping(target = "users", source = "users")
    TeamLeadAdminViewV1 toTeamLeadAdminViewV1(List<TeamRecord> teams, List<UserRecord> users);

    default TeamsResponseV1 toTeamsResponseV1(
        List<TeamRecord> teams,
        List<UserRecord> users,
        Map<String, Boolean> permissions
    ) {
        return toTeamsResponseV1(
            new org.springframework.data.domain.PageImpl<>(teams == null ? List.of() : teams),
            new org.springframework.data.domain.PageImpl<>(users == null ? List.of() : users),
            permissions
        );
    }

    @Mapping(target = "teams", expression = "java(teams.stream().map(this::toTeamV1).toList())")
    @Mapping(target = "teamsPage", expression = "java(toPageInfoV1(teams))")
    @Mapping(target = "users", expression = "java(users.stream().map(this::toUserSummaryForPage).toList())")
    @Mapping(target = "usersPage", expression = "java(toPageInfoV1(users))")
    @Mapping(target = "permissions", source = "permissions")
    TeamsResponseV1 toTeamsResponseV1(
        Page<TeamRecord> teams,
        Page<UserRecord> users,
        Map<String, Boolean> permissions
    );

    @Mapping(target = "member", source = "member")
    AddTeamMemberResponseV1 toAddTeamMemberResponseV1(UserRecord member);

    /**
     * Resolves an add-team-member request's member reference, preferring the explicit
     * {@code member} field and falling back to {@code email}.
     *
     * @param request the add-team-member request
     * @return the member reference to resolve against
     */
    default String resolveMemberRef(AddTeamMemberRequestV1 request) {
        return request.getMember() != null ? request.getMember() : request.getEmail();
    }

    /**
     * Builds a team-candidate user summary, resolving the role with a safe fallback rather than
     * failing on an unrecognized stored role code.
     *
     * @param user the candidate user
     * @return the summary, with {@link UserRoleCodeV1#MEMBER} substituted for an unknown role
     */
    @Named("teamCandidateUserSummary")
    @Mapping(target = "role", expression = "java(toUserRoleCodeV1(user.roleCode()))")
    UserSummaryV1 toUserSummaryForPage(UserRecord user);

    /**
     * Resolves a stored role code to its wire enum, defaulting to {@link UserRoleCodeV1#MEMBER}
     * when the code is blank or unrecognized.
     *
     * @param roleCode the stored role code, or {@code null}
     * @return the resolved role, never {@code null}
     */
    default UserRoleCodeV1 toUserRoleCodeV1(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return UserRoleCodeV1.MEMBER;
        }
        try {
            return UserRoleCodeV1.fromValue(roleCode);
        } catch (IllegalArgumentException ex) {
            return UserRoleCodeV1.MEMBER;
        }
    }
}
