package com.aidigital.aionboarding.service.permission.support;

import com.aidigital.aionboarding.domain.common.dictionary.UserRoleCode;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PermissionDefaultsProviderTest {

	private final PermissionDefaultsProvider provider = new PermissionDefaultsProvider();

	@Test
	void shouldReturnAllTrueForAdminRoleTest() {
		// Given:
		String roleCode = UserRoleCode.ADMIN;

		// When:
		Map<String, Boolean> result = provider.baseDefaults(roleCode);

		// Then:
		assertThat(result).hasSize(PermissionKeys.ALL.size());
		assertThat(result.values()).allMatch(Boolean.TRUE::equals);
	}

	@Test
	void shouldReturnTeamLeadDefaultsTest() {
		// Given:
		String roleCode = UserRoleCode.TEAMLEAD;

		// When:
		Map<String, Boolean> result = provider.baseDefaults(roleCode);

		// Then:
		assertThat(result.get(PermissionKeys.ADMIN_MANAGE_ROLES)).isFalse();
		assertThat(result.get(PermissionKeys.PERMISSIONS_MANAGE_TEAMLEADS)).isFalse();
		assertThat(result.get(PermissionKeys.GROUPS_MANAGE)).isFalse();
		assertThat(result.get(PermissionKeys.MATERIALS_CREATE)).isTrue();
		assertThat(result.get(PermissionKeys.LESSONS_MANAGE_ASSETS)).isTrue();
		assertThat(result.get(PermissionKeys.ROADMAPS_MANAGE)).isTrue();
	}

	@Test
	void shouldReturnMemberDefaultsForUnknownRoleTest() {
		// Given:
		String roleCode = "unknown";

		// When:
		Map<String, Boolean> result = provider.baseDefaults(roleCode);

		// Then:
		assertThat(result.get(PermissionKeys.ADMIN_MANAGE_ROLES)).isFalse();
		assertThat(result.get(PermissionKeys.MATERIALS_CREATE)).isFalse();
		assertThat(result.get(PermissionKeys.LEARNING_ENROLL)).isTrue();
		assertThat(result.get(PermissionKeys.LEARNING_ASK)).isTrue();
	}

	@Test
	void shouldExposeTeamLeadDefaultsDirectlyTest() {
		// When:
		Map<String, Boolean> result = provider.teamLeadDefaults();

		// Then:
		assertThat(result.get(PermissionKeys.TEAMS_MANAGE_MEMBERS)).isTrue();
		assertThat(result.get(PermissionKeys.GRADES_MANAGE)).isTrue();
	}

	@Test
	void shouldExposeMemberDefaultsDirectlyTest() {
		// When:
		Map<String, Boolean> result = provider.memberDefaults();

		// Then:
		assertThat(result.get(PermissionKeys.TEAMS_MANAGE_MEMBERS)).isFalse();
		assertThat(result.get(PermissionKeys.LEARNING_COMPLETE)).isTrue();
	}
}
