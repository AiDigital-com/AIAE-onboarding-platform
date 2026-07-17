package com.aidigital.aionboarding.mappers.common;

import com.aidigital.aionboarding.api.v1.model.UserRoleCodeV1;
import com.aidigital.aionboarding.config.ApplicationMapperConfig;
import org.mapstruct.Mapper;

@Mapper(config = ApplicationMapperConfig.class)
public interface UserRoleCodeApiMapper {

	default UserRoleCodeV1 mapUserRoleCode(String roleCode) {
		if (roleCode == null || roleCode.isBlank()) {
			return UserRoleCodeV1.MEMBER;
		}
		try {
			return UserRoleCodeV1.fromValue(roleCode);
		} catch (IllegalArgumentException ex) {
			return UserRoleCodeV1.MEMBER;
		}
	}

	default String fromUserRoleCode(UserRoleCodeV1 roleCode) {
		return roleCode == null ? null : roleCode.getValue();
	}
}
