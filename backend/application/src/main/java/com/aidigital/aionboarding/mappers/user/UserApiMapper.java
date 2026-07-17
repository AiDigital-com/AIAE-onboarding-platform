package com.aidigital.aionboarding.mappers.user;

import com.aidigital.aionboarding.api.v1.model.AdminUserStatsResponseV1;
import com.aidigital.aionboarding.api.v1.model.AdminUserStatsV1;
import com.aidigital.aionboarding.api.v1.model.AdminUsersListResponseV1;
import com.aidigital.aionboarding.api.v1.model.AssignableUserSummaryV1;
import com.aidigital.aionboarding.api.v1.model.UpdateUserGradeResponseV1;
import com.aidigital.aionboarding.api.v1.model.UserProfileV1;
import com.aidigital.aionboarding.api.v1.model.UserSummaryV1;
import com.aidigital.aionboarding.api.v1.model.UsersListResponseV1;
import com.aidigital.aionboarding.config.ApplicationMapperConfig;
import com.aidigital.aionboarding.mappers.common.PageInfoApiMapper;
import com.aidigital.aionboarding.mappers.common.UserRoleCodeApiMapper;
import com.aidigital.aionboarding.service.user.models.AdminUserStatsRecord;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;

@Mapper(config = ApplicationMapperConfig.class, uses = { UserRoleCodeApiMapper.class, PageInfoApiMapper.class })
public interface UserApiMapper extends PageInfoApiMapper {

    @Mapping(target = "role", source = "roleCode")
    UserSummaryV1 toUserSummaryV1(UserRecord user);

    @Mapping(target = "user", source = ".")
    UserProfileV1 toUserProfileV1(UserRecord user);

    @Mapping(target = "role", source = "roleCode")
    AssignableUserSummaryV1 toAssignableUserSummaryV1(UserRecord user);

    @Mapping(target = "user", source = ".")
    UpdateUserGradeResponseV1 toUpdateUserGradeResponseV1(UserRecord user);

    AdminUserStatsV1 toAdminUserStatsV1(AdminUserStatsRecord stats);

    default AdminUserStatsResponseV1 toAdminUserStatsResponseV1(AdminUserStatsRecord stats) {
        AdminUserStatsResponseV1 response = new AdminUserStatsResponseV1();
        response.setStats(toAdminUserStatsV1(stats));
        return response;
    }

    default AdminUsersListResponseV1 toAdminUsersListResponseV1(Page<UserRecord> users) {
        AdminUsersListResponseV1 response = new AdminUsersListResponseV1();
        response.setUsers(users.stream().map(this::toUserSummaryV1).toList());
        response.setPage(toPageInfoV1(users));
        return response;
    }

    default UsersListResponseV1 toUsersListResponseV1(List<UserRecord> users) {
        UsersListResponseV1 response = new UsersListResponseV1();
        response.setUsers(users == null ? List.of() : users.stream().map(this::toAssignableUserSummaryV1).toList());
        response.setPage(toPageInfoV1(new org.springframework.data.domain.PageImpl<>(
            users == null ? List.of() : users
        )));
        return response;
    }

    default UsersListResponseV1 toUsersListResponseV1(Page<UserRecord> users) {
        UsersListResponseV1 response = new UsersListResponseV1();
        response.setUsers(users.stream().map(this::toAssignableUserSummaryV1).toList());
        response.setPage(toPageInfoV1(users));
        return response;
    }
}
