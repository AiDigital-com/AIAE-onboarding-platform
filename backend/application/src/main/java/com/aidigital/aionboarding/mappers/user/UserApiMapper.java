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

    @Mapping(target = "stats", source = "stats")
    AdminUserStatsResponseV1 toAdminUserStatsResponseV1(AdminUserStatsRecord stats);

    @Mapping(target = "users", expression = "java(users.stream().map(this::toUserSummaryV1).toList())")
    @Mapping(target = "page", expression = "java(toPageInfoV1(users))")
    AdminUsersListResponseV1 toAdminUsersListResponseV1(Page<UserRecord> users);

    @Mapping(target = "users", expression = "java(users.stream().map(this::toAssignableUserSummaryV1).toList())")
    @Mapping(target = "page", expression = "java(toPageInfoV1(users))")
    UsersListResponseV1 toUsersListResponseV1(Page<UserRecord> users);

    /**
     * Builds the users list response from a plain list, wrapping it in a page so it can reuse
     * the {@link #toUsersListResponseV1(Page)} mapping.
     *
     * @param users the users, or {@code null}
     * @return the users list response
     */
    default UsersListResponseV1 toUsersListResponseV1(List<UserRecord> users) {
        return toUsersListResponseV1(
                new org.springframework.data.domain.PageImpl<>(users == null ? List.of() : users));
    }

}
