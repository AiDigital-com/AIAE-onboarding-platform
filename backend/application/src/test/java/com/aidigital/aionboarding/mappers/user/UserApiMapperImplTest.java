package com.aidigital.aionboarding.mappers.user;

import com.aidigital.aionboarding.api.v1.model.AdminUserStatsV1;
import com.aidigital.aionboarding.api.v1.model.AssignableUserSummaryV1;
import com.aidigital.aionboarding.api.v1.model.UpdateUserGradeResponseV1;
import com.aidigital.aionboarding.api.v1.model.UserProfileV1;
import com.aidigital.aionboarding.api.v1.model.UserRoleCodeV1;
import com.aidigital.aionboarding.api.v1.model.UserSummaryV1;
import com.aidigital.aionboarding.mappers.common.UserRoleCodeApiMapper;
import com.aidigital.aionboarding.service.user.models.AdminUserStatsRecord;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserApiMapperImplTest {

	@InjectMocks
	private UserApiMapperImpl userApiMapperImpl;

	@Mock
	private UserRoleCodeApiMapper userRoleCodeApiMapper;

	@BeforeEach
	void setUp() {
		when(userRoleCodeApiMapper.mapUserRoleCode(anyString())).thenReturn(Instancio.create(UserRoleCodeV1.class));
	}

	@Test
	void shouldToUserSummaryV1UserRecordTest() {
		// Given:
		UserRecord user = Instancio.create(UserRecord.class);

		// When:
		UserSummaryV1 actualResult = userApiMapperImpl.toUserSummaryV1(user);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToUserSummaryV1UserRecordWithNullTest() {
		// Given:
		UserRecord user = null;

		// When:
		UserSummaryV1 actualResult = userApiMapperImpl.toUserSummaryV1(user);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToUserProfileV1UserRecordTest() {
		// Given:
		UserRecord user = Instancio.create(UserRecord.class);

		// When:
		UserProfileV1 actualResult = userApiMapperImpl.toUserProfileV1(user);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToUserProfileV1UserRecordWithNullTest() {
		// Given:
		UserRecord user = null;

		// When:
		UserProfileV1 actualResult = userApiMapperImpl.toUserProfileV1(user);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToAssignableUserSummaryV1UserRecordTest() {
		// Given:
		UserRecord user = Instancio.create(UserRecord.class);

		// When:
		AssignableUserSummaryV1 actualResult = userApiMapperImpl.toAssignableUserSummaryV1(user);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToAssignableUserSummaryV1UserRecordWithNullTest() {
		// Given:
		UserRecord user = null;

		// When:
		AssignableUserSummaryV1 actualResult = userApiMapperImpl.toAssignableUserSummaryV1(user);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToUpdateUserGradeResponseV1UserRecordTest() {
		// Given:
		UserRecord user = Instancio.create(UserRecord.class);

		// When:
		UpdateUserGradeResponseV1 actualResult = userApiMapperImpl.toUpdateUserGradeResponseV1(user);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToUpdateUserGradeResponseV1UserRecordWithNullTest() {
		// Given:
		UserRecord user = null;

		// When:
		UpdateUserGradeResponseV1 actualResult = userApiMapperImpl.toUpdateUserGradeResponseV1(user);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToAdminUserStatsV1AdminUserStatsRecordTest() {
		// Given:
		AdminUserStatsRecord stats = Instancio.create(AdminUserStatsRecord.class);

		// When:
		AdminUserStatsV1 actualResult = userApiMapperImpl.toAdminUserStatsV1(stats);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToAdminUserStatsV1AdminUserStatsRecordWithNullTest() {
		// Given:
		AdminUserStatsRecord stats = null;

		// When:
		AdminUserStatsV1 actualResult = userApiMapperImpl.toAdminUserStatsV1(stats);

		// Then:
		assertThat(actualResult).isNull();
	}

}