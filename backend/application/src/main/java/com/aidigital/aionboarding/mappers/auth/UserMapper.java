package com.aidigital.aionboarding.mappers.auth;

import com.aidigital.aionboarding.api.v1.model.UserV1;
import com.aidigital.aionboarding.config.ApplicationMapperConfig;
import com.aidigital.aionboarding.service.common.security.AppUser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = ApplicationMapperConfig.class)
public interface UserMapper {

	@Mapping(target = "userId", source = "clerkUserId")
	UserV1 toUserV1(AppUser appUser);
}
