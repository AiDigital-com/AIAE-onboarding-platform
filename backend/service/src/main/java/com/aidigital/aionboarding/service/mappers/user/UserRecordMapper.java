package com.aidigital.aionboarding.service.mappers.user;

import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.service.common.mapping.ServiceMapperConfig;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = ServiceMapperConfig.class, implementationName = "UserRecordMapperImpl")
public interface UserRecordMapper {

    @Mapping(target = "roleCode", source = "role.code")
    @Mapping(target = "gradeId", source = "grade.id")
    @Mapping(target = "gradeCode", source = "grade.code")
    @Mapping(target = "gradeName", source = "grade.name")
    UserRecord toRecord(User entity);

    default AppUser toAppUser(User entity) {
        UserRecord record = toRecord(entity);
        return new AppUser(
            record.id(),
            record.clerkUserId(),
            record.email(),
            record.name(),
            record.roleCode(),
            record.name(),
            record.position(),
            record.avatarStorageKey(),
            record.avatarColor()
        );
    }
}
