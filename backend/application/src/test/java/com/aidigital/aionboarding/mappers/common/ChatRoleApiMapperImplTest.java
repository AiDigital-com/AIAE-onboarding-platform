package com.aidigital.aionboarding.mappers.common;

import com.aidigital.aionboarding.api.v1.model.ChatRoleV1;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatRoleApiMapperImplTest {

	private final ChatRoleApiMapperImpl chatRoleApiMapperImpl = new ChatRoleApiMapperImpl();

	@Test
	void shouldMapChatRoleStringTest() {
		// Given:
		String role = "value";

		// When:
		ChatRoleV1 actualResult = chatRoleApiMapperImpl.mapChatRole(role);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldFromChatRoleChatRoleV1Test() {
		// Given:
		ChatRoleV1 role = Instancio.create(ChatRoleV1.class);

		// When:
		String actualResult = chatRoleApiMapperImpl.fromChatRole(role);

		// Then:
		assertThat(actualResult).isNotNull();
	}

}