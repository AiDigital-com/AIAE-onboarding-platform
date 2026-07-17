package com.aidigital.aionboarding.mappers.team;

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

    default TeamsResponseV1 toTeamsResponseV1(
        Page<TeamRecord> teams,
        Page<UserRecord> users,
        Map<String, Boolean> permissions
    ) {
        TeamsResponseV1 response = new TeamsResponseV1();
        response.setTeams(teams.stream().map(this::toTeamV1).toList());
        response.setTeamsPage(toPageInfoV1(teams));
        response.setUsers(users.stream().map(this::toUserSummaryForPage).toList());
        response.setUsersPage(toPageInfoV1(users));
        response.setPermissions(permissions);
        return response;
    }

    @Mapping(target = "member", source = "member")
    AddTeamMemberResponseV1 toAddTeamMemberResponseV1(UserRecord member);

    private UserSummaryV1 toUserSummaryForPage(UserRecord user) {
        UserSummaryV1 summary = new UserSummaryV1();
        summary.setId(user.id());
        summary.setName(user.name());
        summary.setEmail(user.email());
        summary.setRole(toUserRoleCodeV1(user.roleCode()));
        summary.setPosition(user.position());
        summary.setAvatarStorageKey(user.avatarStorageKey());
        summary.setAvatarColor(user.avatarColor());
        summary.setGradeId(user.gradeId());
        summary.setGradeCode(user.gradeCode());
        summary.setGradeName(user.gradeName());
        return summary;
    }

    private UserRoleCodeV1 toUserRoleCodeV1(String roleCode) {
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
