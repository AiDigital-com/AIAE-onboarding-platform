package com.aidigital.aionboarding.mappers.common;

import com.aidigital.aionboarding.api.v1.model.UserRoleCodeV1;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserRoleCodeApiMapperImplTest {

	private final UserRoleCodeApiMapperImpl userRoleCodeApiMapperImpl = new UserRoleCodeApiMapperImpl();

	@Test
	void shouldMapUserRoleCodeStringTest() {
		// Given:
		String roleCode = "value";

		// When:
		UserRoleCodeV1 actualResult = userRoleCodeApiMapperImpl.mapUserRoleCode(roleCode);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldFromUserRoleCodeUserRoleCodeV1Test() {
		// Given:
		UserRoleCodeV1 roleCode = Instancio.create(UserRoleCodeV1.class);

		// When:
		String actualResult = userRoleCodeApiMapperImpl.fromUserRoleCode(roleCode);

		// Then:
		assertThat(actualResult).isNotNull();
	}

}