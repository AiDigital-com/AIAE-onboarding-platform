package com.aidigital.aionboarding.mappers.common;

import com.aidigital.aionboarding.api.v1.model.ChatRoleV1;
import com.aidigital.aionboarding.config.ApplicationMapperConfig;
import org.mapstruct.Mapper;

@Mapper(config = ApplicationMapperConfig.class)
public interface ChatRoleApiMapper {

	default ChatRoleV1 mapChatRole(String role) {
		if (role == null || role.isBlank()) {
			return ChatRoleV1.USER;
		}
		try {
			return ChatRoleV1.fromValue(role);
		} catch (IllegalArgumentException ex) {
			return ChatRoleV1.USER;
		}
	}

	default String fromChatRole(ChatRoleV1 role) {
		return role == null ? null : role.getValue();
	}
}
