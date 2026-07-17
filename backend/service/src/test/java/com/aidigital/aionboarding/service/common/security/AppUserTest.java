package com.aidigital.aionboarding.service.common.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AppUserTest {

	@Test
	void shouldExposeIdentityFieldsTest() {
		AppUser user = new AppUser(
				1L,
				"user_abc123",
				"alice@aidigital.com",
				"Alice Example",
				"member",
				"Alice Example",
				"",
				"",
				""
		);

		assertThat(user.clerkUserId()).isEqualTo("user_abc123");
		assertThat(user.email()).isEqualTo("alice@aidigital.com");
		assertThat(user.fullName()).isEqualTo("Alice Example");
	}

	@Test
	void shouldSupportRecordEqualityTest() {
		AppUser a = new AppUser(1L, "user_1", "alice@aidigital.com", "Alice", "member", "Alice", "", "", "");
		AppUser b = new AppUser(1L, "user_1", "alice@aidigital.com", "Alice", "member", "Alice", "", "", "");

		assertThat(a).isEqualTo(b);
	}
}
