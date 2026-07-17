package com.aidigital.aionboarding.service.mappers.user;

import com.aidigital.aionboarding.domain.common.dictionary.entities.UserRole;
import com.aidigital.aionboarding.domain.grade.entities.Grade;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserRecordMapperTest {

	private final UserRecordMapper mapper = new UserRecordMapperImpl();

	@Test
	void shouldMapUserToRecordWithRoleAndGradeFieldsTest() {
		// Given:
		UserRole role = new UserRole();
		role.setCode("teamlead");
		Grade grade = new Grade();
		grade.setId(10L);
		grade.setCode("junior");
		grade.setName("Junior");
		User user = new User();
		user.setId(1L);
		user.setClerkUserId("clerk-1");
		user.setName("Name");
		user.setEmail("email@test.com");
		user.setPosition("Position");
		user.setAvatarStorageKey("key");
		user.setAvatarColor("color");
		user.setRole(role);
		user.setGrade(grade);

		// When:
		UserRecord result = mapper.toRecord(user);

		// Then:
		assertThat(result.id()).isEqualTo(1L);
		assertThat(result.clerkUserId()).isEqualTo("clerk-1");
		assertThat(result.name()).isEqualTo("Name");
		assertThat(result.email()).isEqualTo("email@test.com");
		assertThat(result.roleCode()).isEqualTo("teamlead");
		assertThat(result.position()).isEqualTo("Position");
		assertThat(result.avatarStorageKey()).isEqualTo("key");
		assertThat(result.avatarColor()).isEqualTo("color");
		assertThat(result.gradeId()).isEqualTo(10L);
		assertThat(result.gradeCode()).isEqualTo("junior");
		assertThat(result.gradeName()).isEqualTo("Junior");
	}

	@Test
	void shouldMapUserToRecordWithNullRoleAndGradeTest() {
		// Given:
		User user = new User();
		user.setId(2L);
		user.setClerkUserId("clerk-2");
		user.setName("Name");
		user.setEmail("email@test.com");
		user.setRole(null);
		user.setGrade(null);

		// When:
		UserRecord result = mapper.toRecord(user);

		// Then:
		assertThat(result.roleCode()).isNull();
		assertThat(result.gradeId()).isNull();
		assertThat(result.gradeCode()).isNull();
		assertThat(result.gradeName()).isNull();
	}

	@Test
	void shouldReturnNullRecordForNullUserTest() {
		// When:
		UserRecord result = mapper.toRecord(null);

		// Then:
		assertThat(result).isNull();
	}

	@Test
	void shouldBuildAppUserFromMappedRecordTest() {
		// Given:
		UserRole role = new UserRole();
		role.setCode("admin");
		User user = new User();
		user.setId(3L);
		user.setClerkUserId("clerk-3");
		user.setName("Admin");
		user.setEmail("admin@test.com");
		user.setPosition("CEO");
		user.setAvatarStorageKey("avatar-key");
		user.setAvatarColor("#fff");
		user.setRole(role);
		user.setGrade(null);

		// When:
		AppUser result = mapper.toAppUser(user);

		// Then:
		assertThat(result.internalId()).isEqualTo(3L);
		assertThat(result.clerkUserId()).isEqualTo("clerk-3");
		assertThat(result.email()).isEqualTo("admin@test.com");
		assertThat(result.fullName()).isEqualTo("Admin");
		assertThat(result.roleCode()).isEqualTo("admin");
		assertThat(result.name()).isEqualTo("Admin");
		assertThat(result.position()).isEqualTo("CEO");
		assertThat(result.avatarStorageKey()).isEqualTo("avatar-key");
		assertThat(result.avatarColor()).isEqualTo("#fff");
	}
}
