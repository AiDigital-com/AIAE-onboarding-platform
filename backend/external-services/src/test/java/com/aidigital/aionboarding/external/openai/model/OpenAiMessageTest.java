package com.aidigital.aionboarding.external.openai.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiMessageTest {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Test
	void shouldHoldRoleAndContentTest() {
		// Given:
		OpenAiMessage message = Instancio.create(OpenAiMessage.class);

		// When:
		String role = message.role();
		String content = message.content();

		// Then:
		assertThat(role).isNotNull();
		assertThat(content).isNotNull();
	}

	@Test
	void shouldSerializeToJsonTest() throws JsonProcessingException {
		// Given:
		OpenAiMessage message = new OpenAiMessage("user", "Hello");

		// When:
		String json = OBJECT_MAPPER.writeValueAsString(message);

		// Then:
		assertThat(json).contains("\"role\":\"user\"");
		assertThat(json).contains("\"content\":\"Hello\"");
	}

	@Test
	void shouldDeserializeFromJsonTest() throws JsonProcessingException {
		// Given:
		String json = "{\"role\":\"assistant\",\"content\":\"Hi there\"}";

		// When:
		OpenAiMessage message = OBJECT_MAPPER.readValue(json, OpenAiMessage.class);

		// Then:
		assertThat(message.role()).isEqualTo("assistant");
		assertThat(message.content()).isEqualTo("Hi there");
	}
}
