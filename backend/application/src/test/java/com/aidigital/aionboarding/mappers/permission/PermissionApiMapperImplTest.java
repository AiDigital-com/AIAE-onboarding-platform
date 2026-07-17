package com.aidigital.aionboarding.mappers.permission;

import com.aidigital.aionboarding.api.v1.model.CurrentUserPermissionsResponseV1;
import com.aidigital.aionboarding.api.v1.model.PermissionSnapshotResponseV1;
import com.aidigital.aionboarding.api.v1.model.SetPermissionOverridesResponseV1;
import com.aidigital.aionboarding.api.v1.model.UserPermissionSnapshotV1;
import com.aidigital.aionboarding.api.v1.model.UserRoleCodeV1;
import com.aidigital.aionboarding.api.v1.model.UserSummaryV1;
import com.aidigital.aionboarding.mappers.common.PermissionDefinitionApiMapper;
import com.aidigital.aionboarding.mappers.common.PermissionDefinitionRegistry;
import com.aidigital.aionboarding.mappers.common.UserRoleCodeApiMapper;
import com.aidigital.aionboarding.mappers.user.UserApiMapper;
import com.aidigital.aionboarding.service.permission.models.PermissionSnapshotRecord;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PermissionApiMapperImplTest {

	@InjectMocks
	private PermissionApiMapperImpl permissionApiMapperImpl;

	@Mock
	private UserRoleCodeApiMapper userRoleCodeApiMapper;

	@Mock
	private UserApiMapper userApiMapper;

	@BeforeEach
	void setUp() {
		when(userRoleCodeApiMapper.mapUserRoleCode(anyString())).thenReturn(Instancio.create(UserRoleCodeV1.class));
		when(userApiMapper.toUserSummaryV1(any(UserRecord.class))).thenReturn(Instancio.create(UserSummaryV1.class));
	}

	@Test
	void shouldToUserPermissionSnapshotV1PermissionSnapshotRecordTest() {
		// Given:
		PermissionSnapshotRecord snapshot = Instancio.create(PermissionSnapshotRecord.class);

		// When:
		UserPermissionSnapshotV1 actualResult = permissionApiMapperImpl.toUserPermissionSnapshotV1(snapshot);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToUserPermissionSnapshotV1PermissionSnapshotRecordWithNullTest() {
		// Given:
		PermissionSnapshotRecord snapshot = null;

		// When:
		UserPermissionSnapshotV1 actualResult = permissionApiMapperImpl.toUserPermissionSnapshotV1(snapshot);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToPermissionSnapshotResponseV1ListMapPermissionDefinitionApiMapperPermissionDefinitionRegistryTest() {
		// Given:
		List<UserRecord> users = Instancio.ofList(UserRecord.class).create();
		Map<Long, PermissionSnapshotRecord> permissionsByUserId = Map.of(1L,
				Instancio.create(PermissionSnapshotRecord.class));
		PermissionDefinitionApiMapper permissionDefinitionApiMapper = mock(PermissionDefinitionApiMapper.class);
		when(permissionDefinitionApiMapper.allDefinitions(any(PermissionDefinitionRegistry.class))).thenReturn(List.of());
		PermissionDefinitionRegistry permissionDefinitionRegistry = mock(PermissionDefinitionRegistry.class);
		when(permissionDefinitionRegistry.all()).thenReturn(List.of());

		// When:
		PermissionSnapshotResponseV1 actualResult = permissionApiMapperImpl.toPermissionSnapshotResponseV1(users,
				permissionsByUserId, permissionDefinitionApiMapper, permissionDefinitionRegistry);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToSetPermissionOverridesResponseV1PermissionSnapshotRecordTest() {
		// Given:
		PermissionSnapshotRecord snapshot = Instancio.create(PermissionSnapshotRecord.class);

		// When:
		SetPermissionOverridesResponseV1 actualResult =
				permissionApiMapperImpl.toSetPermissionOverridesResponseV1(snapshot);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToSetPermissionOverridesResponseV1PermissionSnapshotRecordWithNullTest() {
		// Given:
		PermissionSnapshotRecord snapshot = null;

		// When:
		SetPermissionOverridesResponseV1 actualResult =
				permissionApiMapperImpl.toSetPermissionOverridesResponseV1(snapshot);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToCurrentUserPermissionsResponseV1PermissionSnapshotRecordTest() {
		// Given:
		PermissionSnapshotRecord snapshot = Instancio.create(PermissionSnapshotRecord.class);

		// When:
		CurrentUserPermissionsResponseV1 actualResult =
				permissionApiMapperImpl.toCurrentUserPermissionsResponseV1(snapshot);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToCurrentUserPermissionsResponseV1PermissionSnapshotRecordWithNullTest() {
		// Given:
		PermissionSnapshotRecord snapshot = null;

		// When:
		CurrentUserPermissionsResponseV1 actualResult =
				permissionApiMapperImpl.toCurrentUserPermissionsResponseV1(snapshot);

		// Then:
		assertThat(actualResult).isNull();
	}

}