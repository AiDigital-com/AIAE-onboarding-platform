package com.aidigital.aionboarding.mappers.auth;

import com.aidigital.aionboarding.api.v1.model.UserV1;
import com.aidigital.aionboarding.service.common.security.AppUser;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperImplTest {

	private final UserMapperImpl userMapperImpl = new UserMapperImpl();

	@Test
	void shouldToUserV1AppUserTest() {
		// Given:
		AppUser appUser = Instancio.create(AppUser.class);

		// When:
		UserV1 actualResult = userMapperImpl.toUserV1(appUser);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToUserV1AppUserWithNullTest() {
		// Given:
		AppUser appUser = null;

		// When:
		UserV1 actualResult = userMapperImpl.toUserV1(appUser);

		// Then:
		assertThat(actualResult).isNull();
	}

}