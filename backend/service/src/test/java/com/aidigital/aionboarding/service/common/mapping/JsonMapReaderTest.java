package com.aidigital.aionboarding.service.common.mapping;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JsonMapReaderTest {

	private final JsonMapReader reader = new JsonMapReader();

	@Test
	void mapShouldReturnEmptyMapForNonMapInputTest() {
		assertThat(reader.map("not-map")).isEmpty();
	}

	@Test
	void mapShouldKeepOnlyStringKeysTest() {
		Map<Object, Object> raw = Map.of("a", 1, 7, "ignored");

		assertThat(reader.map(raw)).containsExactly(Map.entry("a", 1));
	}

	@Test
	void mapListShouldFilterMixedCollectionsInOrderTest() {
		List<Object> raw = List.of(Map.of("a", 1), "x", Map.of("b", 2));

		assertThat(reader.mapList(raw)).containsExactly(
				Map.of("a", 1),
				Map.of("b", 2)
		);
	}

	@Test
	void mapListShouldReturnEmptyListForNonCollectionInputTest() {
		assertThat(reader.mapList("not-list")).isEmpty();
	}

	@Test
	void stringListShouldFilterBlankAndNonStringItemsTest() {
		assertThat(reader.stringList(List.of("a", " ", "b", 3))).containsExactly("a", "b");
	}

	@Test
	void stringListShouldPreserveStringWhitespaceTest() {
		assertThat(reader.stringList(List.of("  a  "))).containsExactly("  a  ");
	}
}
