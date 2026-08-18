package com.aidigital.aionboarding.service.permission.services.impl;

import com.aidigital.aionboarding.service.common.security.AppUser;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.Select.field;

@ExtendWith(MockitoExtension.class)
class PermissionPolicyServiceImplTest {

	@InjectMocks
	private PermissionPolicyServiceImpl service;

	@Test
	void isTeamManagerShouldReturnTrueWhenUserIsAdminTest() {
		// Given:
		AppUser admin = Instancio.of(AppUser.class)
				.set(field(AppUser::roleCode), "admin")
				.create();

		// When:
		boolean result = service.isTeamManager(admin);

		// Then:
		assertThat(result).isTrue();
	}

	@Test
	void isTeamManagerShouldReturnTrueWhenUserIsTeamLeadTest() {
		// Given:
		AppUser teamLead = Instancio.of(AppUser.class)
				.set(field(AppUser::roleCode), "teamlead")
				.create();

		// When:
		boolean result = service.isTeamManager(teamLead);

		// Then:
		assertThat(result).isTrue();
	}

	@Test
	void isTeamManagerShouldReturnFalseWhenUserHasNeitherAdminNorTeamLeadRoleTest() {
		// Given:
		AppUser learner = Instancio.of(AppUser.class)
				.set(field(AppUser::roleCode), "learner")
				.create();

		// When:
		boolean result = service.isTeamManager(learner);

		// Then:
		assertThat(result).isFalse();
	}

	@Test
	void isTeamManagerShouldReturnFalseWhenUserIsNullTest() {
		// When:
		boolean result = service.isTeamManager(null);

		// Then:
		assertThat(result).isFalse();
	}
}
