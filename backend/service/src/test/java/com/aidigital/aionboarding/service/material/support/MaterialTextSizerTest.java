package com.aidigital.aionboarding.service.material.support;

import com.aidigital.aionboarding.service.common.mapping.JsonMapReader;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MaterialTextSizerTest {

	private final MaterialTextSizer sizer = new MaterialTextSizer(new JsonMapReader());

	@Test
	void countShouldIncludeMaterialAndNestedTextFieldsTest() {
		Map<String, Object> material = Map.of(
				"title", "Title",
				"description", "Description",
				"text", "Body",
				"youtubeTranscripts", List.of(
						Map.of("preparedText", "Transcript A"),
						Map.of("preparedText", "Transcript B")
				),
				"linkAssets", List.of(
						Map.of("extractedText", "Link A"),
						Map.of("extractedText", "Link B")
				)
		);

		int expected = String.join("\n",
				"Title",
				"Description",
				"Body",
				"Transcript A\nTranscript B",
				"Link A\nLink B").length();

		assertThat(sizer.count(material)).isEqualTo(expected);
	}

	@Test
	void countShouldIgnoreNonMapNestedEntriesTest() {
		Map<String, Object> material = Map.of(
				"title", "T",
				"description", "",
				"text", "",
				"youtubeTranscripts", List.of("bad", Map.of("preparedText", "Y")),
				"linkAssets", List.of(3, Map.of("extractedText", "L"))
		);

		assertThat(sizer.count(material)).isEqualTo(String.join("\n", "T", "", "", "Y", "L").length());
	}
}
