package com.aidigital.aionboarding.mappers.permission;

import com.aidigital.aionboarding.api.v1.model.CurrentUserPermissionsResponseV1;
import com.aidigital.aionboarding.api.v1.model.PermissionSnapshotResponseV1;
import com.aidigital.aionboarding.api.v1.model.SetPermissionOverridesResponseV1;
import com.aidigital.aionboarding.api.v1.model.UserPermissionSnapshotV1;
import com.aidigital.aionboarding.config.ApplicationMapperConfig;
import com.aidigital.aionboarding.mappers.common.PermissionDefinitionApiMapper;
import com.aidigital.aionboarding.mappers.common.PermissionDefinitionRegistry;
import com.aidigital.aionboarding.mappers.common.UserRoleCodeApiMapper;
import com.aidigital.aionboarding.mappers.user.UserApiMapper;
import com.aidigital.aionboarding.service.permission.models.PermissionSnapshotRecord;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Mapper(
		config = ApplicationMapperConfig.class,
		uses = {
				UserRoleCodeApiMapper.class,
				PermissionDefinitionApiMapper.class,
				UserApiMapper.class
		}
)
public interface PermissionApiMapper {

	@Mapping(target = "roleCode", source = "roleCode")
	UserPermissionSnapshotV1 toUserPermissionSnapshotV1(PermissionSnapshotRecord snapshot);

	@Mapping(target = "permissionDefinitions", expression = "java(permissionDefinitionApiMapper.allDefinitions" +
			"(permissionDefinitionRegistry))")
	@Mapping(target = "permissionsByUserId", expression = "java(mapPermissionsByUserId(permissionsByUserId))")
	@Mapping(target = "users", source = "users")
	PermissionSnapshotResponseV1 toPermissionSnapshotResponseV1(
			List<UserRecord> users,
			Map<Long, PermissionSnapshotRecord> permissionsByUserId,
			@Context PermissionDefinitionApiMapper permissionDefinitionApiMapper,
			@Context PermissionDefinitionRegistry permissionDefinitionRegistry
	);

	@Mapping(target = "permissions", source = ".")
	SetPermissionOverridesResponseV1 toSetPermissionOverridesResponseV1(PermissionSnapshotRecord snapshot);

	@Mapping(target = "permissions", source = ".")
	CurrentUserPermissionsResponseV1 toCurrentUserPermissionsResponseV1(PermissionSnapshotRecord snapshot);

	default Map<String, UserPermissionSnapshotV1> mapPermissionsByUserId(
			Map<Long, PermissionSnapshotRecord> permissionsByUserId
	) {
		if (permissionsByUserId == null) {
			return Map.of();
		}
		Map<String, UserPermissionSnapshotV1> byUserId = new LinkedHashMap<>();
		permissionsByUserId.forEach(
				(userId, snapshot) -> byUserId.put(String.valueOf(userId), toUserPermissionSnapshotV1(snapshot))
		);
		return byUserId;
	}
}
